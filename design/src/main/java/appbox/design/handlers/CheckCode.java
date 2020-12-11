package appbox.design.handlers;

import appbox.data.JsonResult;
import appbox.design.DesignHub;
import appbox.model.ModelType;
import appbox.runtime.InvokeArgs;

import java.util.concurrent.CompletableFuture;

public final class CheckCode implements IDesignHandler {
    @Override
    public CompletableFuture<Object> handle(DesignHub hub, InvokeArgs args) {
        int type     = args.getInt();
        var targetId = args.getString();
        //Log.debug(String.format("CheckCode: %d %s", type, targetId));

        var modelId   = Long.parseUnsignedLong(targetId);
        var modelNode = hub.designTree.findModelNode(ModelType.Service, modelId);
        if (modelNode == null) {
            var error = String.format("Can't find ServiceModel: %d", modelId);
            return CompletableFuture.failedFuture(new Exception(error));
        }
        var doc       = hub.typeSystem.languageServer.findOpenedDocument(modelId);
        if (doc == null) {
            var error = String.format("Can't find ServiceModel: %d", modelId);
            return CompletableFuture.failedFuture(new Exception(error));
        }
        return CompletableFuture.supplyAsync(() -> {
            //List<QuickFix> quickFixes = new ArrayList<>();
            //hub.typeSystem.languageServer.getAst
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
