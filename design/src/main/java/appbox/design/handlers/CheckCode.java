package appbox.design.handlers;

import appbox.data.JsonResult;
import appbox.design.DesignHub;
import appbox.logging.Log;
import appbox.model.ModelType;
import appbox.runtime.InvokeArg;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class CheckCode implements IRequestHandler {
    @Override
    public CompletableFuture<Object> handle(DesignHub hub, List<InvokeArg> args) {
        int type = args.get(0).getInt();
        var targetId    = args.get(1).getString();
        Log.debug(String.format("CheckCode: %d %s", type, targetId));
        var modelId   = Long.parseUnsignedLong(targetId);
        var modelNode = hub.designTree.findModelNode(ModelType.Service, modelId);
        if (modelNode == null) {
            var error = String.format("Can't find ServiceModel: %d", modelId);
            return CompletableFuture.failedFuture(new Exception(error));
        }

        var fileName = String.format("%s.Services.%s.java",
                modelNode.appNode.model.name(), modelNode.model().name());
        var doc = hub.typeSystem.opendDocs.get(fileName);
        if (doc == null) {
            var error = String.format("Can't find opened ServiceModel: %s", fileName);
            return CompletableFuture.failedFuture(new Exception(error));
        }
        return CompletableFuture.supplyAsync(() -> {
            //List<QuickFix> quickFixes = new ArrayList<>();
            //var sourceFile = new SourceFileObject(Path.of(fileName), doc.sourceText.toString(), Instant.now());
            //try(CompileTask task = hub.typeSystem.workspace.compiler().compile(List.of(sourceFile))) {
            //    LineMap lines = task.root().getLineMap();
            //    for (javax.tools.Diagnostic<? extends JavaFileObject> diagnostic : task.diagnostics) {
            //        QuickFix quickFix = new QuickFix();
            //        quickFix.setLine((int) diagnostic.getLineNumber());
            //        quickFix.setColumn((int) diagnostic.getColumnNumber());
            //        quickFix.setText(diagnostic.getMessage(Locale.CHINESE));
            //        quickFix.setLevel(quickFix.getLevelFromKind(diagnostic.getKind()));//TODO check
            //        quickFix.setEndLine((int) lines.getLineNumber(diagnostic.getEndPosition()));
            //        quickFix.setEndColumn((int) lines.getColumnNumber(diagnostic.getEndPosition()));
            //        quickFixes.add(quickFix);
            //    }
            //    return new JsonResult(quickFixes);
                  return new JsonResult(null);
            //}
        }, hub.codeEditorTaskPool);
    }

}
