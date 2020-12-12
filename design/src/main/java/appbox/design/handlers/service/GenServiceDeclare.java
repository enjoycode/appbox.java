package appbox.design.handlers.service;

import appbox.data.JsonResult;
import appbox.design.DesignHub;
import appbox.design.handlers.IDesignHandler;
import appbox.design.tree.ModelNode;
import appbox.model.ModelType;
import appbox.runtime.InvokeArgs;
import appbox.serialization.IJsonSerializable;
import appbox.serialization.IJsonWriter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/** 用于生成服务模型的前端TypeScript调用声明 */
public final class GenServiceDeclare implements IDesignHandler {

    @Override
    public CompletableFuture<Object> handle(DesignHub hub, InvokeArgs args) {
        var     modelId = args.getString();
        boolean loadAll = modelId == null || modelId.isEmpty(); //空表示所有服务模型用于初次加载

        List<ModelNode> serviceNodes;
        if (loadAll) {
            serviceNodes = hub.designTree.findNodesByType(ModelType.Service);
        } else { //指定标识用于更新
            var node = hub.designTree.findModelNode(ModelType.Service, Long.parseUnsignedLong(modelId));
            serviceNodes = new ArrayList<>() {{ add(node); }};
        }

        var list = new ArrayList<TypeScriptDeclare>();
        //TODO:生成TypeScript声明代码
        return CompletableFuture.completedFuture(new JsonResult(list));
    }

    public static final class TypeScriptDeclare implements IJsonSerializable {

        public String name;
        public String declare;

        public TypeScriptDeclare() {
        }

        public TypeScriptDeclare(String name, String declare) {
            this.name = name;
            this.declare = declare;
        }

        @Override
        public void writeToJson(IJsonWriter writer) {
            throw new RuntimeException("未实现");
        }

    }

}
