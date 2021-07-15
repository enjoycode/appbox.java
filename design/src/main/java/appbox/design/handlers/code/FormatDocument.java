package appbox.design.handlers.code;

import appbox.data.JsonResult;
import appbox.design.DesignHub;
import appbox.design.handlers.IDesignHandler;
import appbox.model.ModelType;
import appbox.runtime.InvokeArgs;
import org.dartlang.analysis.server.protocol.SourceEdit;
import org.eclipse.lsp4j.CompletionItem;

import java.util.concurrent.CompletableFuture;

/** 格式化代码 */
public final class FormatDocument implements IDesignHandler {

    static {
        JsonResult.registerType(SourceEdit.class, (serializer, object, fieldName, fieldType, features) -> {
            var item = (SourceEdit) object;
            var wr   = serializer.getWriter();

            wr.writeFieldValue('{', "offset", item.getOffset());
            wr.writeFieldValue(',', "length", item.getLength());
            wr.writeFieldValue(',', "replacement", item.getReplacement());
            wr.write('}');
        });
    }

    @Override
    public CompletableFuture<Object> handle(DesignHub hub, InvokeArgs args) {
        final var fileName = args.getString();  //TODO:考虑修改前端传模型标识
        //TODO:待修改以下查找，暂根据名称找到模型
        var modelNode = hub.designTree.findModelNodeByFileName(fileName);
        if (modelNode == null)
            return CompletableFuture.failedFuture(new RuntimeException("Can't find model: " + fileName));

        if (modelNode.model().modelType() == ModelType.Service) {
           throw new RuntimeException("Not implemented.");
        } else if (modelNode.model().modelType() == ModelType.View) {
           return hub.dartLanguageServer.formatDocument(modelNode)
                   .thenApply(JsonResult::new);
        }

        return CompletableFuture.failedFuture(new RuntimeException("Not supported"));
    }

}
