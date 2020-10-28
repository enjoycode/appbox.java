package appbox.design.handlers;

import appbox.data.JsonResult;
import appbox.design.DesignHub;
import appbox.logging.Log;
import appbox.model.ModelType;
import appbox.runtime.InvokeArg;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class GetCompletion implements IRequestHandler {
    //region ====AutoCompleteItem====
    //public static final class AutoCompleteItem {
    //    public String detail;           //returnType + displayText
    //    public String documentation;
    //    public int    kind;
    //    public String insertText;
    //    public String label;            //displayText
    //}
    //endregion

    @Override
    public CompletableFuture<Object> handle(DesignHub hub, List<InvokeArg> args) {
        var type = args.get(0).getInt();
        var fileName       = args.get(1).getString(); //TODO:考虑修改前端传模型标识
        var line           = args.get(2).getInt() - 1; //注意：前端值需要-1
        var column         = args.get(3).getInt() - 1; //注意：前端值需要-1
        var wordToComplete = args.get(4).getString();

        Log.debug(String.format("%d %s %d-%d %s", type, fileName, line, column, wordToComplete));
        return CompletableFuture.completedFuture(Collections.EMPTY_LIST);

        //TODO:待修改以下查找，暂根据名称找到模型
        //var firstDot = fileName.indexOf('.');
        //var lastDot = fileName.lastIndexOf('.');
        //var appName = fileName.substring(0, firstDot);
        //var app = hub.designTree.findApplicationNodeByName(appName);
        //var secondDot = fileName.indexOf('.', firstDot +1);
        //var modelName = fileName.substring(secondDot + 1, lastDot);
        //var modelNode = hub.designTree.findModelNodeByName(app.model.id(), ModelType.Service, modelName);
        //var modelId   = modelNode.model().id();
        //var doc = hub.typeSystem.workspace.findOpenedDocument(modelId);
        //if (doc == null) {
        //    var error = String.format("Can't find opened ServiceModel: %s", fileName);
        //    return CompletableFuture.failedFuture(new Exception(error));
        //}
        //
        ////暂在同一线程内处理
        //return CompletableFuture.supplyAsync(() -> {
        //    //Log.debug(String.format("GetCompletion: run at thread: %s", Thread.currentThread().getName()));
        //    var list = hub.typeSystem.workspace.fillCompletion(doc, line, column, wordToComplete);
        //    var result = new ArrayList<AutoCompleteItem>(list.size());
        //    //转换为前端所需结构
        //    for(var cr : list) {
        //        //System.out.println(cr.toString());
        //        var item = new AutoCompleteItem();
        //        item.label = item.insertText = item.detail = cr.getLookupElement().getLookupString();
        //        item.kind = getKind(cr);
        //        result.add(item);
        //    }
        //    return new JsonResult(result);
        //}, hub.codeEditorTaskPool);
    }

}
