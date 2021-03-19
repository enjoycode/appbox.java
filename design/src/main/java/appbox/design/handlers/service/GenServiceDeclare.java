package appbox.design.handlers.service;

import appbox.data.JsonResult;
import appbox.design.DesignHub;
import appbox.design.handlers.IDesignHandler;
import appbox.design.handlers.code.TypeScriptDeclare;
import appbox.design.services.CodeGenService;
import appbox.design.tree.ModelNode;
import appbox.model.ModelType;
import appbox.runtime.InvokeArgs;

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
        //生成TypeScript声明代码
        for (var node : serviceNodes) {
            var name    = String.format("%s.Services.%s", node.appNode.model.name(), node.model().name());
            var declare = CodeGenService.genServiceDeclareCode(hub, node);
            list.add(new TypeScriptDeclare(name, declare));
        }

        //初次加载时添加系统服务声明
        if (modelId == null) {
            var adminServiceDeclare = "declare namespace sys.Services.AdminService {function LoadPermissionNodes():Promise<object[]>;function SavePermission(id:string, orgunits:string[]):Promise<void>;}";
            list.add(new TypeScriptDeclare("sys.Services.AdminService", adminServiceDeclare));
        }

        return CompletableFuture.completedFuture(new JsonResult(list));
    }

}
