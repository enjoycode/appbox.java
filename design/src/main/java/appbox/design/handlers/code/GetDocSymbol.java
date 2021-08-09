package appbox.design.handlers.code;

import appbox.data.JsonResult;
import appbox.design.DesignHub;
import appbox.design.handlers.IDesignHandler;
import appbox.model.ModelType;
import appbox.runtime.InvokeArgs;
import org.eclipse.lsp4j.DocumentSymbol;

import java.util.concurrent.CompletableFuture;

public final class GetDocSymbol implements IDesignHandler {

    static {
        JsonResult.registerType(DocumentSymbol.class, (serializer, object, fieldName, fieldType, features) -> {
            final var item = (DocumentSymbol) object;
            final var wr   = serializer.getWriter();

            wr.writeFieldValue('{', "name", item.getName());
            wr.writeFieldValue(',', "detail", item.getDetail());
            wr.writeFieldValue(',', "kind", item.getKind().getValue() + 1); //+1同monaco一致
            wr.write(',');
            wr.writeFieldName("range");
            serializer.write(item.getRange());
            wr.write(',');
            wr.writeFieldName("selectionRange");
            serializer.write(item.getSelectionRange());
            if (item.getChildren() != null && item.getChildren().size() > 0) {
                wr.write(',');
                wr.writeFieldName("children");
                serializer.write(item.getChildren());
            }
            wr.write('}');
        });
    }

    @Override
    public CompletableFuture<Object> handle(DesignHub hub, InvokeArgs args) {
        final var fileName = args.getString(); //TODO:考虑修改前端传模型标识

        //TODO:待修改以下查找，暂根据名称找到模型
        var modelNode = hub.designTree.findModelNodeByFileName(fileName);
        if (modelNode == null)
            return CompletableFuture.failedFuture(new RuntimeException("Can't find model: " + fileName));

        if (modelNode.model().modelType() == ModelType.Service) {
            final var modelId = modelNode.model().id();
            final var doc     = hub.typeSystem.javaLanguageServer.findOpenedDocument(modelId);
            if (doc == null) {
                var error = String.format("Can't find opened ServiceModel: %s", fileName);
                return CompletableFuture.failedFuture(new RuntimeException(error));
            }

            final var list = hub.typeSystem.javaLanguageServer.documentSymbol(doc);
            return CompletableFuture.completedFuture(new JsonResult(list));
        } else if (modelNode.model().modelType() == ModelType.View) {
            //TODO:
            return CompletableFuture.failedFuture(new RuntimeException("未实现"));
        }

        return CompletableFuture.failedFuture(new RuntimeException("Not supported"));
    }

}
