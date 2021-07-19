package appbox.design.lang.dart;

import appbox.design.event.CodeFoldingEvent;
import appbox.logging.Log;
import com.google.dart.server.AnalysisServerListenerAdapter;
import org.dartlang.analysis.server.protocol.*;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionItemKind;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

/** 监听dart analysis server的事件(from stdio) */
public final class DartAnalysisListener extends AnalysisServerListenerAdapter {
    private final DartLanguageServer server;

    public DartAnalysisListener(DartLanguageServer server) {
        this.server = server;
    }

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
        var task = server.completionTasks.remove(completionId);
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
                    libraryFile == null || libraryFile.isEmpty() ? null : server._existingImports.get(libraryFile);

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

                var suggestionSet = server.cachedCompletions.get(includedSuggestionSet.getId());
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
            server.cachedCompletions.put(set.getId(), set);
        }

        for (var r : removed) {
            server.cachedCompletions.remove(r);
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

        server._existingImports.put(file, alreadyImportedSymbols);
    }

    @Override
    public void computedFolding(String file, List<FoldingRegion> regions) {
        final var modelId = server.openedFiles.get(file);
        if (modelId == null) {
            Log.warn("Can't find opened file: " + file);
            return;
        }

        //封装为事件发送给前端
        final var event = new CodeFoldingEvent(modelId, regions);
        server.hub.session.sendEvent(event);
    }

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

}
