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
import com.google.dart.server.utilities.logging.Logger;
import com.google.dart.server.utilities.logging.Logging;
import org.dartlang.analysis.server.protocol.*;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionItemKind;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.*;
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

    private final DesignHub             hub;
    private final Path                  rootPath;
    private final AtomicInteger         _initDone   = new AtomicInteger(0);
    private final HashMap<Long, String> openedFiles = new HashMap<>();

    private StdioServerSocket        serverSocket;
    private RemoteAnalysisServerImpl analysisServer;
    private AnalysisServerListener   analysisServerListener;

    private final HashMap<String, CompletionTask>                            completionTasks   = new HashMap<>();
    private final HashMap<Integer, AvailableSuggestionSet>                   cachedCompletions = new HashMap<>();
    private final HashMap<String, HashMap<String, HashMap<String, Boolean>>> _existingImports  = new HashMap<>();

    static {
        //TODO:fix sdk path with run command>: flutter sdk-path,或者读取配置
        sdkPath              = "/home/rick/snap/flutter/common/flutter/";
        dartVMPath           = sdkPath + "bin/dart";
        flutterVMPath        = sdkPath + "bin/flutter";
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
                    items.add(toCompletionItem(suggestion));
                }

                // getCachedResults
                if (!includedSuggestionSets.isEmpty() && !includedElementKinds.isEmpty()) {
                    var existingImports =
                            libraryFile == null || libraryFile.isEmpty() ? null : _existingImports.get(libraryFile);

                    // Create a fast lookup for which kinds to include.
                    var elementKinds = new HashMap<String, Boolean>();
                    includedElementKinds.forEach(k -> elementKinds.put(k, true));

                    // Create a fast lookup for relevance boosts based on tag string.
                    var tagBoosts = new HashMap<String, Integer>();
                    includedSuggestionRelevanceTags.forEach(r -> tagBoosts.put(r.getTag(), r.getRelevanceBoost()));

                    // Keep track of suggestion sets we've seen to avoid included them twice.
                    // See https://github.com/dart-lang/sdk/issues/37211.
                    var usedSuggestionSets = new HashMap<Integer, Boolean>();
                    // Keep track of items items we've included so we don't show dupes if
                    // there are multiple libraries importing the same thing.
                    var includedItems = new HashMap<String, Boolean>();
                    for (var includedSuggestionSet : includedSuggestionSets) {
                        if (usedSuggestionSets.containsKey(includedSuggestionSet.getId())) continue;

                        // Mark that we've done this one so we don't do it again.
                        usedSuggestionSets.put(includedSuggestionSet.getId(), true);

                        var suggestionSet = cachedCompletions.get(includedSuggestionSet.getId());
                        if (suggestionSet == null) {
                            Log.warn("Suggestion set [" + includedSuggestionSet.getId() + "] was not available now.");
                            continue;
                        }

                        var unresolvedItems = suggestionSet.getItems().stream()
                                .filter(suggestion -> {
                                    if (!elementKinds.containsKey(suggestion.getElement().getKind()))
                                        return false;

                                    // 根据wordToComplete附加过滤
                                    if (!task.wordToComplete.isEmpty() && !suggestion.getLabel().startsWith(task.wordToComplete))
                                        return false;

                                    // Check existing imports to ensure we don't already import
                                    // this element (note: this exact element from its declaring
                                    // library, not just something with the same name). If we do
                                    // we'll want to skip it.
                                    // Trim back to the . to handle enum values
                                    // https://github.com/Dart-Code/Dart-Code/issues/1835
                                    var key = String.format("%s/%s",
                                            suggestion.getLabel().split("\\.")[0], suggestion.getDeclaringLibraryUri());
                                    var importingUris = existingImports == null ? null : existingImports.get(key);

                                    // If there are no URIs already importing this, then include it as an auto-import.
                                    if (importingUris == null) return true;
                                    // Otherwise, it is imported but if it's not by this file, then skip it.
                                    if (importingUris.get(suggestionSet.getUri()) == null) return false;

                                    // Finally, we're importing a file that has this item, so include
                                    // it only if it has not already been included by another imported file.

                                    // Unlike the above, we include the Kind here so that things with similar labels
                                    // like Constructors+Class are still included.
                                    var fullItemKey = String.format("%s/%s/%s",
                                            suggestion.getLabel(), suggestion.getElement().getKind(), suggestion.getDeclaringLibraryUri());
                                    var itemHasAlreadyBeenIncluded = includedItems.containsKey(fullItemKey);
                                    includedItems.put(fullItemKey, true);

                                    return !itemHasAlreadyBeenIncluded;
                                })
                                .map(suggestion -> {
                                    //// Calculate the relevance for this item.
                                    //int relevanceBoost = 0;
                                    //if(suggestion.getRelevanceTags() != null)
                                    //    suggestion.getRelevanceTags().forEach(t ->
                                    //            relevanceBoost = Math.max(relevanceBoost, tagBoosts.getOrDefault(t, 0)));

                                    return toCompletionItemFromSuggestion(suggestion);
                                })
                                .collect(Collectors.toList());

                        items.addAll(unresolvedItems);
                    }
                }

                //TODO: 排序及限制数量
                //Log.debug("Get completion items: " + items.size());
                task.future.complete(items);
            }

            @Override
            public void computedAvailableSuggestions(List<AvailableSuggestionSet> changed, int[] removed) {
                // storeCompletionSuggestions,暂缓存在后端(数据量较大),
                for (var set : changed) {
                    cachedCompletions.put(set.getId(), set);
                }

                for (var r : removed) {
                    cachedCompletions.remove(r);
                }
            }

            @Override
            public void computedExistingImports(String file, ExistingImports existingImports) {
                // storeExistingImports
                // Map with key "elementName/elementDeclaringLibraryUri"
                // Value is a set of imported URIs that import that element.
                var alreadyImportedSymbols = new HashMap<String, HashMap<String, Boolean>>();
                for (var existingImport : existingImports.getImports()) {
                    for (var importedElement : existingImport.getElements()) {
                        // This is the symbol name and declaring library. That is, the
                        // library that declares the symbol, not the one that was imported.
                        // This wil be the same for an element that is re-exported by other
                        // libraries, so we can avoid showing the exact duplicate.
                        var elementName = existingImports.getElements().getStrings()
                                .get(existingImports.getElements().getNames()[importedElement]);
                        var elementDeclaringLibraryUri = existingImports.getElements().getStrings()
                                .get(existingImports.getElements().getUris()[importedElement]);
                        var importedUri = existingImports.getElements().getStrings()
                                .get(existingImport.getUri());
                        var key = String.format("%s/%s", elementName, elementDeclaringLibraryUri);

                        if (!alreadyImportedSymbols.containsKey(key))
                            alreadyImportedSymbols.put(key, new HashMap<>());
                        alreadyImportedSymbols.get(key).put(importedUri, true);
                    }
                }

                _existingImports.put(file, alreadyImportedSymbols);
            }
        };
    }

    private void onAnalysisServerStatusChanged(boolean isAlive) {
        if (isAlive) return;

        stopAnalysisServer();
        Log.warn("Dart analysis server stopped.");
    }

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

    //region ====Completion Converters & Helpers====
    private static CompletionItem toCompletionItem(CompletionSuggestion suggestion) {
        //TODO:暂简单处理
        var item = new CompletionItem(suggestion.getDisplayText() != null && !suggestion.getDisplayText().isEmpty()
                ? suggestion.getDisplayText() : suggestion.getCompletion());
        item.setKind(toCompletionKind(suggestion.getKind()));
        item.setInsertText(suggestion.getCompletion());
        return item;
    }

    private static CompletionItem toCompletionItemFromSuggestion(AvailableSuggestion suggestion) {
        //TODO:暂简单处理
        var item = new CompletionItem(suggestion.getLabel());
        item.setKind(suggestion.getElement() != null ? toCompletionKind(suggestion.getElement().getKind()) : CompletionItemKind.Text);
        item.setInsertText(suggestion.getLabel());
        return item;
    }

    /** CompletionSuggestion.kind to CompletionItemKind */
    private static CompletionItemKind toCompletionKind(String kind) {
        switch (kind) {
            case "ARGUMENT_LIST":
            case "IDENTIFIER":
            case "OPTIONAL_ARGUMENT":
            case "NAMED_ARGUMENT":
                return CompletionItemKind.Variable;
            case "IMPORT":
                //return label.startsWith("dart:")
                //        ? CompletionItemKind.Module
                //        : path.extname(label.toLowerCase()) === ".dart"
                //        ? CompletionItemKind.File
                //        : CompletionItemKind.Folder;
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
    //endregion
}
