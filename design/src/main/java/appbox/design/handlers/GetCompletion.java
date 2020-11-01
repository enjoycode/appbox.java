package appbox.design.handlers;

import appbox.data.JsonResult;
import appbox.design.DesignHub;
import appbox.logging.Log;
import appbox.model.ModelType;
import appbox.runtime.InvokeArg;
import org.eclipse.lsp4j.CompletionItem;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class GetCompletion implements IRequestHandler {

    static {
        JsonResult.registerType(CompletionItem.class, (serializer, object, fieldName, fieldType, features) -> {
            var item = (CompletionItem) object;
            var wr   = serializer.getWriter();

            wr.writeFieldValue('{', "label", item.getLabel()); //必须有值，否则前端报错
            wr.writeFieldValue(',', "insertText", item.getTextEdit().getNewText());
            wr.writeFieldValue(',', "kind", item.getKind().getValue());
            if (item.getDetail() != null) {
                wr.writeFieldValue(',', "detail", item.getDetail());
            }

            //TODO:判断不等于offset才写入range以节省流量
            var range = item.getTextEdit().getRange();
            wr.write(',');
            wr.writeFieldName("range");
            wr.writeFieldValue('{', "startLineNumber", range.getStart().getLine() + 1);
            wr.writeFieldValue(',', "startColumn", range.getStart().getCharacter() + 1);
            wr.writeFieldValue(',', "endLineNumber", range.getEnd().getLine() + 1);
            wr.writeFieldValue(',', "endColumn", range.getEnd().getCharacter() + 1);
            wr.write('}');

            //wr.writeFieldValue(',', "sortText", item.getSortText());
            wr.write('}');
        });
    }

    @Override
    public CompletableFuture<Object> handle(DesignHub hub, List<InvokeArg> args) {
        var type           = args.get(0).getInt();
        var fileName       = args.get(1).getString(); //TODO:考虑修改前端传模型标识
        var line           = args.get(2).getInt() - 1; //注意：前端值需要-1
        var column         = args.get(3).getInt() - 1; //注意：前端值需要-1
        var wordToComplete = args.get(4).getString();

        Log.debug(String.format("%d %s %d-%d %s", type, fileName, line, column, wordToComplete));

        //TODO:待修改以下查找，暂根据名称找到模型
        var firstDot  = fileName.indexOf('.');
        var lastDot   = fileName.lastIndexOf('.');
        var appName   = fileName.substring(0, firstDot);
        var app       = hub.designTree.findApplicationNodeByName(appName);
        var secondDot = fileName.indexOf('.', firstDot + 1);
        var modelName = fileName.substring(secondDot + 1, lastDot);
        var modelNode = hub.designTree.findModelNodeByName(app.model.id(), ModelType.Service, modelName);
        var modelId   = modelNode.model().id();
        var doc       = hub.typeSystem.languageServer.findOpenedDocument(modelId);
        if (doc == null) {
            var error = String.format("Can't find opened ServiceModel: %s", fileName);
            return CompletableFuture.failedFuture(new Exception(error));
        }

        //暂在同一线程内处理
        return CompletableFuture.supplyAsync(() -> {
            //Log.debug(String.format("GetCompletion: run at thread: %s", Thread.currentThread().getName()));
            var list = hub.typeSystem.languageServer.completion(doc, line, column, wordToComplete);
            return new JsonResult(list);
        }, hub.codeEditorTaskPool);
    }

}
