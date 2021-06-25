package appbox.design.dart;

import appbox.design.DesignHub;
import appbox.design.handlers.view.OpenViewModel;
import appbox.design.tree.ModelNode;
import appbox.design.utils.PathUtil;
import appbox.logging.Log;
import appbox.model.ModelType;
import appbox.model.ViewModel;
import com.google.dart.server.AnalysisServerListener;
import com.google.dart.server.AnalysisServerListenerAdapter;
import com.google.dart.server.GetSuggestionsConsumer;
import com.google.dart.server.internal.remote.RemoteAnalysisServerImpl;
import com.google.dart.server.internal.remote.StdioServerSocket;
import org.dartlang.analysis.server.protocol.*;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionItemKind;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * 用于前端Flutter工程的语言服务
 */
public class DartLanguageServer {

    private static final String sdkPath;
    private static final String dartVMPath;
    private static final String flutterVMPath;
    private static final String analyzerSnapshotPath;
    //private static final String pubSnapshotPath;

    private final DesignHub     hub;
    private final Path          rootPath;
    private final AtomicInteger _initDone = new AtomicInteger(0);

    private StdioServerSocket        serverSocket;
    private RemoteAnalysisServerImpl analysisServer;
    private AnalysisServerListener   analysisServerListener;

    private final HashMap<Long, String>                                    openedFiles     = new HashMap<>();
    private final HashMap<String, CompletableFuture<List<CompletionItem>>> completionTasks = new HashMap<>();

    static {
        //TODO:fix sdk path with run command>: flutter sdk-path,或者读取配置
        sdkPath              = "/home/rick/snap/flutter/common/flutter/";
        dartVMPath           = sdkPath + "bin/dart";
        flutterVMPath        = sdkPath + "bin/flutter";
        analyzerSnapshotPath = sdkPath + "bin/cache/dart-sdk/bin/snapshots/analysis_server.dart.snapshot";
        //pubSnapshotPath      = sdkPath + "bin/cache/dart-sdk/bin/snapshots/pub.dart.snapshot";
    }

    public DartLanguageServer(DesignHub hub) {
        this.hub      = hub;
        this.rootPath = Path.of(PathUtil.tmpPath, "appbox", "flutter",
                Long.toUnsignedString(hub.session.sessionId()));
        if (!Files.exists(this.rootPath)) {
            try {
                Files.createDirectories(this.rootPath);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    //region ====Dart Analysis Server====
    public void init() {
        int oldState = _initDone.compareAndExchange(0, 1);
        if (oldState != 0)
            throw new RuntimeException("Not implemented");

        extractFlutterFiles();

        extractModels().thenAccept(r -> {
            Log.debug("Extract flutter files done and start analyzer...");
            try {
                runPubGet();
                startAnalysisServer();
                _initDone.set(2);
            } catch (Exception e) {
                _initDone.set(3);
                Log.error("Can't start dart analysis server: " + e.getMessage());
            }
        });
    }

    private void startAnalysisServer() throws Exception {
        serverSocket = new StdioServerSocket(dartVMPath, null, analyzerSnapshotPath, null, null);
        serverSocket.setClientId("VSCODE-REMOTE");
        serverSocket.setClientVersion("1.0.0");

        analysisServer = new RemoteAnalysisServerImpl(serverSocket);
        analysisServer.start();
        //订阅事件
        analysisServer.addStatusListener((this::onAnalysisServerStatusChanged));
        analysisServer.completion_setSubscriptions(List.of("AVAILABLE_SUGGESTION_SETS"));
        analysisServerListener = createAnalysisServerListener();
        analysisServer.addAnalysisServerListener(analysisServerListener);

        //设置工作目录
        analysisServer.analysis_setAnalysisRoots(List.of(rootPath.toString()), null, null);
    }

    private void stopAnalysisServer() {
        analysisServer.removeAnalysisServerListener(analysisServerListener);
        analysisServer.server_shutdown();
        serverSocket.stop();

        serverSocket           = null;
        analysisServerListener = null;
        analysisServer         = null;
    }

    private AnalysisServerListener createAnalysisServerListener() {
        return new AnalysisServerListenerAdapter() {
            @Override
            public void serverConnected(String version) {
                Log.info("Dart analysis server connected: " + version);
            }

            @Override
            public void serverError(boolean isFatal, String message, String stackTrace) {
                Log.warn("Dart analysis server error: " + message);
            }

            @Override
            public void requestError(RequestError requestError) {
                Log.warn("Dart analysis server request error: " + requestError.getMessage());
            }

            @Override
            public void computedErrors(String file, List<AnalysisError> errors) {
                if (!errors.isEmpty()) {
                    //TODO:事件转发至前端
                    var sb = new StringBuilder();
                    for (var error : errors) {
                        sb.append(error.toString());
                    }
                    Log.warn("File error: " + file + "\n" + sb.toString());
                }
            }

            @Override
            public void computedCompletion(String completionId, int replacementOffset,
                                           int replacementLength, List<CompletionSuggestion> completions,
                                           List<IncludedSuggestionSet> includedSuggestionSets,
                                           List<String> includedElementKinds,
                                           List<IncludedSuggestionRelevanceTag> includedSuggestionRelevanceTags,
                                           boolean isLast, String libraryFile) {
                //TODO:根据isLast合并结果
                if (!isLast) Log.warn("Completion is not last.");
                var task = completionTasks.remove(completionId);
                if (task == null) {
                    Log.warn("Can't find completion task: " + completionId);
                    return;
                }
                var items = new ArrayList<CompletionItem>(completions.size());
                for (var suggestion : completions) {
                    items.add(convertCompletionItem(suggestion));
                }
                task.complete(items);
            }
        };
    }

    private void onAnalysisServerStatusChanged(boolean isAlive) {
        if (isAlive) return;

        stopAnalysisServer();
        Log.warn("Dart analysis server stopped.");
    }

    /**
     * 创建Flutter应用的相关文件,相当于flutter create
     */
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

    private CompletableFuture<Void> extractViewModel(ModelNode node) {
        return OpenViewModel.loadSourceCode(node).thenApply(code -> {
            var filePath = getModelFilePath(node);
            try {
                if (!filePath.toFile().getParentFile().exists())
                    filePath.toFile().getParentFile().mkdirs();

                Files.writeString(filePath, code.Script, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        });
    }

    private void runPubGet() throws IOException, InterruptedException {
        var cmd = List.of(flutterVMPath, "pub", "get");
        var process = new ProcessBuilder().command(cmd)
                .directory(rootPath.toFile()).inheritIO().start();
        boolean ok = process.waitFor(30, TimeUnit.SECONDS);
        if (ok)
            Log.debug("Run pub get done.");
        else
            Log.warn("Run pub get timeout.");
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
            default:
                types = "unknown"; break;
        }
        return Path.of(rootPath.toString(), "lib",
                node.appNode.model.name(), types, node.model().name() + ".dart");
    }
    //endregion

    //region ====Completion Converters====
    private static CompletionItem convertCompletionItem(CompletionSuggestion suggestion) {
        //TODO:暂简单处理
        var item = new CompletionItem(suggestion.getDisplayText() != null && !suggestion.getDisplayText().isEmpty()
                ? suggestion.getDisplayText() : suggestion.getCompletion());
        item.setKind(convertCompletionKind(suggestion.getKind()));
        item.setInsertText(suggestion.getCompletion());
        return item;
    }

    /**
     * CompletionSuggestion.kind to CompletionItemKind
     */
    private static CompletionItemKind convertCompletionKind(String kind) {
        switch (kind) {
            case "ARGUMENT_LIST":
                //return label.startsWith("dart:")
                //        ? CompletionItemKind.Module
                //        : path.extname(label.toLowerCase()) === ".dart"
                //        ? CompletionItemKind.File
                //        : CompletionItemKind.Folder;
            case "IDENTIFIER":
            case "OPTIONAL_ARGUMENT":
            case "NAMED_ARGUMENT":
                return CompletionItemKind.Variable;
            case "IMPORT":
                return CompletionItemKind.Module;
            case "INVOCATION":
                return CompletionItemKind.Method;
            case "KEYWORD":
                return CompletionItemKind.Keyword;
            case "PARAMETER":
                return CompletionItemKind.Value;
            default:
                return CompletionItemKind.Text;
        }
    }
    //endregion

    //region ====Dart Language Server====
    public void openDocument(ModelNode node, String content) {
        //检查是否已打开
        if (openedFiles.containsKey(node.model().id()))
            return;

        var filePath = getModelFilePath(node);
        openedFiles.put(node.model().id(), filePath.toString());

        analysisServer.analysis_setPriorityFiles(new ArrayList<>(openedFiles.values()));

        var add = new AddContentOverlay(content);
        var files = new HashMap<String, Object>() {{
            put(filePath.toString(), add);
        }};
        analysisServer.analysis_updateContent(files, () -> {});
    }

    public void changeDocument(ModelNode node, int offset, int length, String newText) {
        var filePath = getModelFilePath(node);
        var change   = new ChangeContentOverlay(List.of(new SourceEdit(offset, length, newText, null)));
        var files = new HashMap<String, Object>() {{
            put(filePath.toString(), change);
        }};
        analysisServer.analysis_updateContent(files, () -> {});
    }

    public CompletableFuture<List<CompletionItem>> completion(ModelNode node, int offset) {
        final var filePath = getModelFilePath(node);
        var       task     = new CompletableFuture<List<CompletionItem>>();
        analysisServer.completion_getSuggestions(filePath.toString(), offset, new GetSuggestionsConsumer() {
            @Override
            public void computedCompletionId(String completionId) {
                completionTasks.put(completionId, task);
            }

            @Override
            public void onError(RequestError requestError) {
                task.completeExceptionally(new RuntimeException(requestError.getMessage()));
            }
        });
        return task;
    }
    //endregion
}
