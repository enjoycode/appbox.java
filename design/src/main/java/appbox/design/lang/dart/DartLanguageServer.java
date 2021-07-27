package appbox.design.lang.dart;

import appbox.design.DesignHub;
import appbox.design.handlers.view.OpenViewModel;
import appbox.design.tree.ModelNode;
import appbox.design.utils.PathUtil;
import appbox.logging.Log;
import appbox.model.ModelType;
import appbox.model.ViewModel;
import appbox.serialization.BytesOutputStream;
import appbox.store.KVTransaction;
import appbox.store.ModelStore;
import com.google.dart.server.AnalysisServerListener;
import com.google.dart.server.FormatConsumer;
import com.google.dart.server.GetHoverConsumer;
import com.google.dart.server.GetSuggestionsConsumer;
import com.google.dart.server.internal.remote.RemoteAnalysisServerImpl;
import com.google.dart.server.internal.remote.StdioServerSocket;
import com.google.dart.server.utilities.logging.Logger;
import com.google.dart.server.utilities.logging.Logging;
import org.dartlang.analysis.server.protocol.*;
import org.eclipse.lsp4j.CompletionItem;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.zip.GZIPOutputStream;

/**
 * 用于前端Flutter工程的语言服务
 */
public class DartLanguageServer {

    private static final String sdkPath;
    private static final String dartVMPath;
    private static final String flutterVMPath;
    private static final String devcVMPath;
    private static final String analyzerSnapshotPath;
    //private static final String pubSnapshotPath;

    protected final DesignHub             hub;
    private final   Path                  rootPath;
    private final   AtomicInteger         _initDone   = new AtomicInteger(0);
    protected final HashMap<String, Long> openedFiles = new HashMap<>();

    private StdioServerSocket        serverSocket;
    private RemoteAnalysisServerImpl analysisServer;
    private AnalysisServerListener   analysisServerListener;

    //TODO:考虑发送Completion事件给IDE,而不是用CompletionTask等待
    protected final HashMap<String, CompletionTask>                            completionTasks   = new HashMap<>();
    protected final HashMap<Integer, AvailableSuggestionSet>                   cachedCompletions = new HashMap<>();
    protected final HashMap<String, HashMap<String, HashMap<String, Boolean>>> _existingImports  = new HashMap<>();

    static {
        //TODO:fix sdk path with run command>: flutter sdk-path,或者读取配置
        sdkPath       = "/home/rick/snap/flutter/common/flutter/";
        dartVMPath    = sdkPath + "bin/dart";
        flutterVMPath = sdkPath + "bin/flutter";
        devcVMPath    = Path.of(PathUtil.currentPath, "preview", "dartdevc").toString();

        analyzerSnapshotPath = sdkPath + "bin/cache/dart-sdk/bin/snapshots/analysis_server.dart.snapshot";
        //pubSnapshotPath      = sdkPath + "bin/cache/dart-sdk/bin/snapshots/pub.dart.snapshot";

        Logging.setLogger(new Logger() {
            @Override
            public void logError(String message) {
                Log.error("DartAnalysisServer error: " + message);
            }

            @Override
            public void logError(String message, Throwable exception) {
                Log.error("DartAnalysisServer error: " + message);
            }

            @Override
            public void logInformation(String message) {
                Log.info("DartAnalysisServer info: " + message);
            }

            @Override
            public void logInformation(String message, Throwable exception) {
                Log.info("DartAnalysisServer info: " + message);
            }
        });
    }

    public DartLanguageServer(DesignHub hub, boolean forTest) {
        this.hub      = hub;
        this.rootPath = Path.of(PathUtil.tmpPath, "appbox", "flutter", hub.session.name());

        if (forTest) return; //仅用于测试,不清空

        try {
            //TODO:暂清空重建
            if (this.rootPath.toFile().exists()) {
                Files.walk(this.rootPath)
                        .map(Path::toFile)
                        .sorted((o1, o2) -> -o1.compareTo(o2))
                        .forEach(File::delete);
            }

            Files.createDirectories(Path.of(this.rootPath.toString(), "build"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //region ====Dart Analysis Server====

    /** 开始创建工程并启动分析服务 */
    public void start() {
        int oldState = _initDone.compareAndExchange(0, 1);
        if (oldState != 0)
            throw new RuntimeException("Not implemented");

        extractFlutterFiles();

        extractModels().thenCompose(r -> {
            Log.debug("Extract flutter files done.");
            return runPubGet();
        }).thenAccept((ok) -> {
            try {
                Log.debug("Start analysis server...");
                startAnalysisServer();
                _initDone.set(2);
            } catch (Exception e) {
                _initDone.set(3);
                Log.error("Can't start dart analysis server: " + e.getMessage());
            }
        });
    }

    public void stop() {
        //TODO:判断是否已启动,另中止未完成的CompletionTask

        analysisServer.server_shutdown();
    }

    private void startAnalysisServer() throws Exception {
        serverSocket = new StdioServerSocket(dartVMPath, null, analyzerSnapshotPath, null, null);
        serverSocket.setClientId("AppBoxStudio");
        serverSocket.setClientVersion("1.0.0");

        analysisServer = new RemoteAnalysisServerImpl(serverSocket);
        analysisServer.start();
        //订阅事件
        analysisServer.addStatusListener((this::onAnalysisServerStatusChanged));
        analysisServer.completion_setSubscriptions(List.of(CompletionService.AVAILABLE_SUGGESTION_SETS));
        analysisServerListener = new DartAnalysisListener(this);
        analysisServer.addAnalysisServerListener(analysisServerListener);

        //设置工作目录
        analysisServer.analysis_setAnalysisRoots(List.of(rootPath.toString()), null, null);
    }

    private void stopAnalysisServer() {
        Log.debug("Stop analysis server.");
        analysisServer.removeAnalysisServerListener(analysisServerListener);
        analysisServer.server_shutdown(); //TODO: check is need
        serverSocket.stop();

        serverSocket           = null;
        analysisServerListener = null;
        analysisServer         = null;
    }

    private void onAnalysisServerStatusChanged(boolean isAlive) {
        if (isAlive) return;

        stopAnalysisServer();
        Log.warn("Analysis server stopped.");
    }

    private CompletableFuture<Boolean> runPubGet() {
        var cmd = List.of(flutterVMPath, "pub", "get");
        try {
            var process = new ProcessBuilder().command(cmd)
                    .directory(rootPath.toFile()).inheritIO().start();
            return process.onExit().thenApply((p) -> {
                if (p.exitValue() != 0) {
                    Log.error(String.format("Run flutter pub get with error: %d", p.exitValue()));
                    return false;
                }
                Log.debug("Run flutter pub get done.");
                return true;
            });
        } catch (IOException e) {
            Log.error("Can't run flutter pub get");
            return CompletableFuture.completedFuture(false);
        }
    }

    //endregion

    //region ====Extract Flutter Files====

    /** 创建Flutter应用的相关文件,相当于flutter create */
    private void extractFlutterFiles() {
        //TODO: pubspec.yaml需要根据包生成,暂简单复制
        var fs       = DartLanguageServer.class.getResourceAsStream("/flutter/pubspec.yaml");
        var filePath = Path.of(rootPath.toString(), "pubspec.yaml");
        try {
            Files.copy(fs, filePath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private CompletableFuture<Void> extractModels() {
        //处理视图模型
        var                     views = hub.designTree.findNodesByType(ModelType.View);
        CompletableFuture<Void> fut   = CompletableFuture.completedFuture(null);
        for (var view : views) {
            var model = (ViewModel) view.model();
            if (model.getType() == ViewModel.TYPE_FLUTTER) {
                fut = fut.thenCompose(r -> extractViewModel(view));
            }
        }
        return fut;
    }

    private void extractWebFiles(String appName) {
        //TODO: other files

        //index.html
        var fs       = DartLanguageServer.class.getResourceAsStream("/flutter/index.html");
        var filePath = Path.of(rootPath.toString(), "web", "index.html");
        try {
            Files.createDirectory(Path.of(rootPath.toString(), "web"));

            //注意修改index.html的<base href="/">
            var indexHtml = new String(fs.readAllBytes());
            indexHtml = indexHtml.replace("<base href=\"/\">", "<base href=\"/" + appName + "/\">");

            Files.writeString(filePath, indexHtml, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private CompletableFuture<Void> extractViewModel(ModelNode node) {
        return OpenViewModel.loadSourceCode(node)
                .thenAccept(code -> writeViewModelFile(node, code.Script));
    }

    private void writeViewModelFile(ModelNode node, String code) {
        final var filePath = getModelFilePath(node);
        try {
            if (!filePath.toFile().getParentFile().exists())
                filePath.toFile().getParentFile().mkdirs();

            Files.writeString(filePath, code, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            Log.error(String.format("Write view's dart file[%s.%s] error: %s",
                    node.appNode.text(), node.text(), e));
        }
    }

    private Path getModelFilePath(ModelNode node) {
        String types;
        switch (node.model().modelType()) {
            case Service:
                types = "services"; break;
            case Entity:
                types = "entities"; break;
            case View:
                types = "views"; break;
            case Enum:
                types = "enums"; break;
            default:
                types = "unknown"; break;
        }
        return getFilePath(node.appNode.model.name(), types, node.model().name());
    }

    private Path getFilePath(String appName, String typeName, String modelName) {
        return Path.of(rootPath.toString(), "lib",
                appName, typeName, modelName + ".dart");
    }

    //endregion

    //region ====Dart Language Server====

    /** 打开的文件改变后更新分析服务的相关订阅 */
    private void onOpennedChanges() {
        analysisServer.analysis_setPriorityFiles(new ArrayList<>(openedFiles.keySet()));
        analysisServer.analysis_setSubscriptions(Map.of(AnalysisService.FOLDING, new ArrayList<>(openedFiles.keySet())));
    }

    public void openDocument(ModelNode node, String content) {
        final var filePath = getModelFilePath(node);
        //检查是否已打开
        if (openedFiles.containsKey(filePath.toString()))
            return;

        openedFiles.put(filePath.toString(), node.model().id());

        //TODO: check analysis server is running
        onOpennedChanges();

        var add = new AddContentOverlay(content);
        var files = new HashMap<String, Object>() {{
            put(filePath.toString(), add);
        }};
        analysisServer.analysis_updateContent(files, () -> {});
    }

    public void changeDocument(ModelNode node, int offset, int length, String newText) {
        final var filePath = getModelFilePath(node);
        var       change   = new ChangeContentOverlay(List.of(new SourceEdit(offset, length, newText, null)));
        var files = new HashMap<String, Object>() {{
            put(filePath.toString(), change);
        }};
        analysisServer.analysis_updateContent(files, () -> {});
    }

    public void closeDocument(ModelNode node) {
        final var filePath = getModelFilePath(node);
        if (!openedFiles.containsKey(filePath.toString()))
            return;

        openedFiles.remove(filePath.toString());

        onOpennedChanges();

        var remove = new RemoveContentOverlay();
        var files = new HashMap<String, Object>() {{
            put(filePath.toString(), remove);
        }};
        analysisServer.analysis_updateContent(files, () -> {});
    }

    public CompletableFuture<List<CompletionItem>> completion(ModelNode node, int offset, String wordToComplete) {
        final var filePath = getModelFilePath(node);
        var       task     = new CompletionTask(wordToComplete);
        analysisServer.completion_getSuggestions(filePath.toString(), offset, new GetSuggestionsConsumer() {
            @Override
            public void computedCompletionId(String completionId) {
                completionTasks.put(completionId, task);
            }

            @Override
            public void onError(RequestError requestError) {
                task.future.completeExceptionally(new RuntimeException(requestError.getMessage()));
            }
        });
        return task.future;
    }

    /** 格式化整个文档 */
    public CompletableFuture<List<SourceEdit>> formatDocument(ModelNode node) {
        final var filePath = getModelFilePath(node);
        var       task     = new CompletableFuture<List<SourceEdit>>();
        analysisServer.edit_format(filePath.toString(), 0, 0, 0, new FormatConsumer() {

            @Override
            public void computedFormat(List<SourceEdit> edits, int selectionOffset, int selectionLength) {
                task.complete(edits);
            }

            @Override
            public void onError(RequestError requestError) {
                Log.warn("Format document error: " + requestError.getMessage());
                task.completeExceptionally(new RuntimeException(requestError.getMessage()));
            }
        });

        return task;
    }

    public CompletableFuture<HoverInformation[]> getHover(ModelNode node, int offset) {
        final var filePath = getModelFilePath(node);
        var       task     = new CompletableFuture<HoverInformation[]>();
        analysisServer.analysis_getHover(filePath.toString(), offset, new GetHoverConsumer() {
            @Override
            public void computedHovers(HoverInformation[] hovers) {
                task.complete(hovers);
            }

            @Override
            public void onError(RequestError requestError) {
                Log.warn("Get hover error: " + requestError.getMessage());
                task.completeExceptionally(new RuntimeException(requestError.getMessage()));
            }
        });

        return task;
    }
    //endregion

    //region ====Preview & Publish Complier====

    /**
     * 编译预览js
     * @param path         eg: packages/appbox/sys/views/HomePage.dart.js
     * @param forceRebuild only for test
     * @return true成功
     */
    public CompletableFuture<Object> compilePreview(String path, boolean forceRebuild) {
        if (!path.startsWith("packages/") && !path.startsWith("lib/")) {
            Log.warn("Invalid preview path: " + path);
            return CompletableFuture.completedFuture(null);
        }

        // 先判断是否源代码 xxx.dart
        if (path.endsWith(".dart")) {
            final var srcFile = Path.of(rootPath.toString(), path);
            return CompletableFuture.completedFuture(srcFile.toString());
        }

        // 再判断是否已经编译好,是则直接返回
        final String buildPath = Path.of(this.rootPath.toString(), "build").toString();
        final var    fullPath  = Path.of(buildPath, path).toFile();
        if (!forceRebuild && fullPath.exists()) {
            return CompletableFuture.completedFuture(fullPath.toString());
        }

        // 开始编译 TODO: *** 1.增量编译;2.判断是否存在错误,是则直接返回
        var cmd = new ArrayList<String>();
        cmd.add(devcVMPath);
        cmd.add("--modules=amd");
        cmd.add("--no-summarize"); //不需要生成dill
        //cmd.add("--inline-source-map"); //不是嵌入source map
        cmd.add("--sound-null-safety");
        cmd.add("--enable-experiment=non-nullable");

        cmd.add("--dart-sdk-summary=" + Path.of(sdkPath, "bin", "cache", "flutter_web_sdk", "flutter_web_sdk",
                "kernel", "flutter_ddc_sdk_sound.dill").toString());

        cmd.add("-s");
        cmd.add(Path.of(PathUtil.currentPath, "preview", "flutter_web.dill").toString());
        cmd.add("-s");
        cmd.add(Path.of(PathUtil.currentPath, "preview", "get.dill").toString());
        //TODO:其他通用包

        cmd.add("-o");
        cmd.add(buildPath);

        try {
            final var    paths    = path.split("/");
            final String viewName = paths[4].substring(0, paths[4].length() - 3); //remove '.js'
            cmd.add(String.format("package:appbox/%s/%s/%s", paths[2], paths[3], viewName));

            var process = new ProcessBuilder().command(cmd)
                    .directory(rootPath.toFile()).inheritIO().start();
            return process.onExit().thenApply((ps) -> ps.exitValue() == 0 ? fullPath.toString() : null);
        } catch (Exception e) {
            Log.error(String.format("Can't start devc: %s", e));
            return CompletableFuture.completedFuture(null);
        }
    }

    /** 用于保存时更新代码,同时删除已经编译的js文件 */
    public void updateViewModelCode(ModelNode node, String code) {
        writeViewModelFile(node, code);

        final var jsFilePath = Path.of(this.rootPath.toString(),
                "build", "packages", "appbox",
                node.appNode.model.name(), "views", node.model().name() + ".dart.js");
        //Log.debug(String.format("Delete js file: %s", jsFilePath));
        try {
            Files.deleteIfExists(jsFilePath);
            //TODO: need delete js.map file
        } catch (IOException e) {
            Log.warn(String.format("Delete compiled js file[%s] error: %s", node.model().name(), e));
        }
    }

    /** 生成并发布Web应用 */
    public CompletableFuture<Object> buildWebApp(String appName, boolean isHtmlRenderer, boolean forTest) {
        if (!forTest) {
            final var appNode = hub.designTree.findApplicationNodeByName(appName);
            if (appNode == null)
                return CompletableFuture.failedFuture(new RuntimeException("Can't find application"));
        }

        //TODO: globle lock for only one can buid

        //1.创建main.dart文件,并准备编译web所需要的文件
        final var mainFilePath = Path.of(this.rootPath.toString(), "lib", appName, "main.dart");
        try {
            createAppMainFile(appName, mainFilePath);
            extractWebFiles(appName);
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }

        //2.开始编译
        var cmd = List.of(flutterVMPath, "build", "web",
                "--web-renderer", isHtmlRenderer ? "html" : "auto",
                //"--no-pub",
                "--no-source-maps",
                "-t",
                mainFilePath.toString());
        try {
            var process = new ProcessBuilder().command(cmd)
                    .directory(rootPath.toFile()).inheritIO().start();
            return process.onExit().thenCompose((p) -> {
                if (p.exitValue() != 0) {
                    Log.error(String.format("Run flutter buid web with error: %d", p.exitValue()));
                    return CompletableFuture.failedFuture(new RuntimeException("Build web app error."));
                }
                Log.debug("Run flutter buid web done.");
                //开始保存编译结果
                if (!forTest) {
                    return saveAppWebFiles(appName).thenApply(r -> {
                        Log.info("Save compiled web files done.");
                        return null;
                    });
                }

                return CompletableFuture.completedFuture(null);
            });
        } catch (IOException e) {
            Log.error("Can't run flutter build web");
            return CompletableFuture.completedFuture(false);
        }
    }

    private void createAppMainFile(String appName, Path mainFilePath) throws IOException {
        final var sb = new StringBuilder(500);
        sb.append("import 'package:flutter/material.dart';\n");
        sb.append("import 'package:appbox/" + appName + "/views/HomePage.dart';\n");
        sb.append("void main() {\n");
        sb.append("  runApp(MaterialApp(\n");
        sb.append("    title: '" + appName + "',\n");
        sb.append("    home: HomePage()\n");
        sb.append("  ));\n");
        sb.append("}");


        Files.writeString(mainFilePath, sb, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    private CompletableFuture<Void> saveAppWebFiles(String appName) {
        //TODO:清空旧的,另考虑通用文件不用保存,或者直接传输路径给主进程，由主进程处理

        return KVTransaction.beginAsync().thenCompose((txn) -> {
            try {
                final var outPath = Path.of(rootPath.toString(), "build", "web");
                var files = Files.walk(outPath)
                        .map(Path::toFile)
                        .filter(file -> file.isFile() && !file.getName().equals("NOTICES"))
                        .collect(Collectors.toList());

                CompletableFuture<Void> task = CompletableFuture.completedFuture(null);
                for (var file : files) {
                    final var zipData  = gzipFileToBytes(file);
                    final var filePath = file.toPath();
                    //asmName eg: "/erp/index.html"
                    final var asmName =
                            String.format("/%s/%s", appName,
                                    filePath.subpath(outPath.getNameCount(), filePath.getNameCount()));
                    task = task.thenCompose(r -> ModelStore.upsertAssemblyAsync(false, asmName, zipData, txn));
                }

                return task.thenCompose(r -> txn.commitAsync());
            } catch (Exception ex) {
                Log.error("Save compiled web files error: " + ex.getMessage());
                return CompletableFuture.failedFuture(ex);
            }
        });
    }

    private byte[] gzipFileToBytes(File file) throws IOException {
        var fis = new FileInputStream(file);
        var fos = new BytesOutputStream(512);
        fos.writeByte((byte) 2); //gzip compress flag
        var gzipOS = new GZIPOutputStream(fos);

        byte[] buffer = new byte[1024];
        int    len;
        while ((len = fis.read(buffer)) != -1) {
            gzipOS.write(buffer, 0, len);
        }
        //close resources
        gzipOS.close();
        fos.close();
        fis.close();

        return fos.toByteArray(); //TODO:直接返BytesOutputStream,避免copy
    }
    //endregion
}
