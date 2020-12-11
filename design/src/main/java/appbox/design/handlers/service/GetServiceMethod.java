package appbox.design.handlers.service;

import appbox.design.DesignHub;
import appbox.design.handlers.IDesignHandler;
import appbox.model.ModelType;
import appbox.runtime.InvokeArgs;
import com.alibaba.fastjson.JSON;

import java.util.concurrent.CompletableFuture;

/** 用于前端调试服务方法或测试调用服务方法时定位服务方法及获取参数 */
public final class GetServiceMethod implements IDesignHandler {

    @Override
    public CompletableFuture<Object> handle(DesignHub hub, InvokeArgs args) {
        var modelId = args.getString();
        var line    = args.getInt() - 1;
        var column  = args.getInt() - 1;

        var modelNode =
                hub.designTree.findModelNode(ModelType.Service, Long.parseUnsignedLong(modelId));
        if (modelNode == null)
            return CompletableFuture.failedFuture(new RuntimeException("Can't find service node"));

        var servieMethodInfo = hub.typeSystem.languageServer.findServiceMethod(
                modelNode.appNode.model.name(), modelNode.model().name(), line, column);
        if (servieMethodInfo == null) {
            return CompletableFuture.failedFuture(new RuntimeException("Can't find service method"));
        }

        //TODO:暂兼容旧前端代码转换为字符串
        return CompletableFuture.completedFuture(JSON.toJSONString(servieMethodInfo));
    }

}
