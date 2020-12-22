package appbox.design.handlers.view;

import appbox.data.JsonResult;
import appbox.design.DesignHub;
import appbox.design.handlers.IDesignHandler;
import appbox.design.services.StagedService;
import appbox.model.ModelType;
import appbox.model.ViewModel;
import appbox.runtime.InvokeArgs;
import appbox.serialization.IJsonSerializable;
import appbox.serialization.IJsonWriter;
import appbox.store.ModelStore;
import appbox.store.ViewCode;

import java.util.concurrent.CompletableFuture;

public final class OpenViewModel implements IDesignHandler {

    @Override
    public CompletableFuture<Object> handle(DesignHub hub, InvokeArgs args) {
        var modelId   = Long.parseUnsignedLong(args.getString());
        var modelNode = hub.designTree.findModelNode(ModelType.View, modelId);
        if (modelNode == null)
            throw new RuntimeException("Can't find view model");

        final var res = new OpenViewModelResult();
        res.model = (ViewModel) modelNode.model();
        if (modelNode.isCheckoutByMe()) {
            return StagedService.loadViewCodeAsync(modelId).thenCompose(code -> {
                if (code == null)
                    return ModelStore.loadViewCodeAsync(modelId);
                return CompletableFuture.completedFuture(code);
            }).thenApply(code -> {
                res.code = code;
                return new JsonResult(res);
            });
        } else {
            return ModelStore.loadViewCodeAsync(modelId).thenApply(code -> {
                res.code = code;
                return new JsonResult(res);
            });
        }
    }

    static final class OpenViewModelResult implements IJsonSerializable {
        public ViewModel model;
        public ViewCode  code;

        @Override
        public void writeToJson(IJsonWriter writer) {
            writer.startObject();

            model.writeToJson(writer);

            writer.writeKeyValue("Template", code == null ? "" : code.Template);
            writer.writeKeyValue("Script", code == null ? "" : code.Script);
            writer.writeKeyValue("Style", code == null ? "" : code.Style);

            writer.endObject();
        }
    }

}
