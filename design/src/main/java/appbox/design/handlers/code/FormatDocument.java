package appbox.design.handlers.code;

import appbox.data.JsonResult;
import appbox.design.DesignHub;
import appbox.design.handlers.IDesignHandler;
import appbox.model.ModelType;
import appbox.runtime.InvokeArgs;
import org.dartlang.analysis.server.protocol.SourceEdit;
import org.eclipse.lsp4j.TextEdit;

import java.util.concurrent.CompletableFuture;

/** 格式化代码 */
public final class FormatDocument implements IDesignHandler {

    static {
        JsonResult.registerType(SourceEdit.class, (serializer, object, fieldName, fieldType, features) -> {
            final var item = (SourceEdit) object;
            final var wr   = serializer.getWriter();

            wr.writeFieldValue('{', "offset", item.getOffset());
            wr.writeFieldValue(',', "length", item.getLength());
            wr.writeFieldValue(',', "replacement", item.getReplacement());
            wr.write('}');
        });

        JsonResult.registerType(TextEdit.class, ((serializer, object, fieldName, fieldType, features) -> {
            final var item = (TextEdit) object;
            final var wr   = serializer.getWriter();
            wr.write('{');
            wr.writeFieldName("range");
            serializer.write(item.getRange());
            wr.writeFieldValue(',', "text", item.getNewText());
            wr.write('}');
        }));
    }

    @Override
    public CompletableFuture<Object> handle(DesignHub hub, InvokeArgs args) {
        final var fileName = args.getString();  //TODO:考虑修改前端传模型标识
        //TODO:待修改以下查找，暂根据名称找到模型
        var modelNode = hub.designTree.findModelNodeByFileName(fileName);
        if (modelNode == null)
            return CompletableFuture.failedFuture(new RuntimeException("Can't find model: " + fileName));

        if (modelNode.model().modelType() == ModelType.Service) {
            final var modelId = modelNode.model().id();
            final var doc     = hub.typeSystem.languageServer.findOpenedDocument(modelId);
            if (doc == null) {
                var error = String.format("Can't find opened ServiceModel: %s", fileName);
                return CompletableFuture.failedFuture(new RuntimeException(error));
            }

            final var list = hub.typeSystem.languageServer.formatting(doc);
            return CompletableFuture.completedFuture(new JsonResult(list)); //TODO:转为新IDE格式
        } else if (modelNode.model().modelType() == ModelType.View) {
            return hub.dartLanguageServer.formatDocument(modelNode)
                    .thenApply(JsonResult::new);
        }

        return CompletableFuture.failedFuture(new RuntimeException("Not supported"));
    }

}
