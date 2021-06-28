package appbox.design.lang.dart;

import org.eclipse.lsp4j.CompletionItem;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/** 挂起的任务 */
public final class CompletionTask {
    public final CompletableFuture<List<CompletionItem>> future = new CompletableFuture<>();
    public final String wordToComplete;

    CompletionTask(String wordToComplete) {
        this.wordToComplete = wordToComplete;
    }
}
