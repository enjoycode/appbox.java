package appbox.design.handlers.code;

import appbox.data.JsonResult;
import appbox.design.DesignHub;
import appbox.design.handlers.IDesignHandler;
import appbox.logging.Log;
import appbox.model.ModelType;
import appbox.runtime.InvokeArgs;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionItemKind;

import java.util.concurrent.CompletableFuture;

public final class GetCompletion implements IDesignHandler {

    static {
        JsonResult.registerType(CompletionItem.class, (serializer, object, fieldName, fieldType, features) -> {
            var item = (CompletionItem) object;
            var wr   = serializer.getWriter();

            wr.writeFieldValue('{', "label", item.getLabel()); //必须有值，否则前端报错
            if (item.getInsertText() == null) {
                wr.writeFieldValue(',', "insertText", item.getTextEdit().getNewText());
                var range = item.getTextEdit().getRange();
                wr.write(',');
                wr.writeFieldName("range");
                wr.writeFieldValue('{', "startLineNumber", range.getStart().getLine() + 1);
                wr.writeFieldValue(',', "startColumn", range.getStart().getCharacter() + 1);
                wr.writeFieldValue(',', "endLineNumber", range.getEnd().getLine() + 1);
                wr.writeFieldValue(',', "endColumn", range.getEnd().getCharacter() + 1);
                wr.write('}');
            } else {
                wr.writeFieldValue(',', "insertText", item.getInsertText());
            }
            wr.writeFieldValue(',', "kind", mapKind(item.getKind())); //类型与monaco不一致
            //wr.writeFieldValue(',', "insertTextRules", 0);
            if (item.getDetail() != null) {
                wr.writeFieldValue(',', "detail", item.getDetail());
            }
            //wr.writeFieldValue(',', "sortText", item.getSortText());

            if (item.getAdditionalTextEdits() != null && item.getAdditionalTextEdits().size() > 0) {
                wr.write(',');
                wr.writeFieldName("additionalTextEdits");
                wr.write('[');
                for (var additional : item.getAdditionalTextEdits()) {
                    var range = additional.getRange();
                    wr.write('{');
                    wr.writeFieldName("range");
                    wr.writeFieldValue('{', "startLineNumber", range.getStart().getLine() + 1);
                    wr.writeFieldValue(',', "startColumn", range.getStart().getCharacter() + 1);
                    wr.writeFieldValue(',', "endLineNumber", range.getEnd().getLine() + 1);
                    wr.writeFieldValue(',', "endColumn", range.getEnd().getCharacter() + 1);
                    wr.write('}');
                    if(additional.getNewText() != null) {
                        wr.writeFieldValue(',', "text", additional.getNewText());
                    } else {
                        wr.writeFieldValue(',', "text", "null");
                    }

                    wr.write('}');
                }
                wr.write(']');
            }

            wr.write('}');
        });
    }

    private static int mapKind(CompletionItemKind kind) {
        switch (kind) {
            case Method:
                return 0;
            case Function:
                return 1;
            case Constructor:
                return 2;
            case Field:
                return 3;
            case Variable:
                return 4;
            case Class:
                return 5;
            case Interface:
                return 7;
            case Module:
                return 8;
            case Property:
                return 9;
            case Unit:
                return 12;
            case Value:
                return 13;
            case Enum:
                return 15;
            case Keyword:
                return 17;
            case Snippet:
                return 27;
            case Color:
                return 19;
            case File:
                return 20;
            case Reference:
                return 21;
            case Folder:
                return 23;
            case EnumMember:
                return 16;
            case Constant:
                return 14;
            case Struct:
                return 6;
            case Event:
                return 10;
            case Operator:
                return 11;
            case TypeParameter:
                return 24;
            default:
                return 18; //Text
        }
    }

    @Override
    public CompletableFuture<Object> handle(DesignHub hub, InvokeArgs args) {
        var type           = args.getInt();
        var fileName       = args.getString(); //TODO:考虑修改前端传模型标识
        var line           = args.getInt() - 1; //注意：前端值需要-1
        var column         = args.getInt() - 1; //注意：前端值需要-1
        var wordToComplete = args.getString();

        //Log.debug(String.format("%d %s %d-%d %s", type, fileName, line, column, wordToComplete));

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
