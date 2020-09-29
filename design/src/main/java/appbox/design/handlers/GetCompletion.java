package appbox.design.handlers;

import appbox.data.JsonResult;
import appbox.design.DesignHub;
import appbox.logging.Log;
import appbox.runtime.InvokeArg;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class GetCompletion implements IRequestHandler {
    @Override
    public CompletableFuture<Object> handle(DesignHub hub, List<InvokeArg> args) {
        var type           = args.get(0).getInt();
        var fileName       = args.get(1).getString();
        var line           = args.get(2).getInt() - 1; //注意：前端值需要-1
        var column         = args.get(3).getInt() - 1; //注意：前端值需要-1
        var wordToComplete = args.get(4).getString();

        Log.debug(String.format("GetCompletion: %d %s %d-%d %s", type, fileName, line, column, wordToComplete));
        var doc = hub.typeSystem.opendDocs.get(fileName);
        if (doc == null) {
            var error = String.format("Can't find opened ServiceModel: %s", fileName);
            return CompletableFuture.failedFuture(new Exception(error));
        }

        //继续测试
        return CompletableFuture.supplyAsync(() -> {
            Log.debug(String.format("GetCompletion: run at thread: %s", Thread.currentThread().getName()));
            //var provider   = new CompletionProvider(hub.typeSystem.workspace.compiler());
            //var sourceFile = new SourceFileObject(Path.of(fileName), doc.sourceText.toString(), Instant.now());
            //var list       = provider.complete(sourceFile, line + 1, column + 1);
            //if (list != CompletionProvider.NOT_SUPPORTED) {
            //    return new JsonResult(list.items);
            //    //return CompletableFuture.completedFuture(new JsonResult(list.items));
            //}

            //return CompletableFuture.completedFuture(new JsonResult(null));
            return new JsonResult(null); //TODO: empty array
        }, hub.codeEditorTaskPool);

    }
}
