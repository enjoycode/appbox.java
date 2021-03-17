package appbox.design.handlers.service;

import appbox.design.DesignHub;
import appbox.design.handlers.IDesignHandler;
import appbox.model.ModelType;
import appbox.runtime.InvokeArgs;
import com.alibaba.fastjson.JSON;

import java.util.concurrent.CompletableFuture;

/** 根据服务名称获取相关信息 */
public final class GetMethodInfo implements IDesignHandler {

    @Override
    public CompletableFuture<Object> handle(DesignHub hub, InvokeArgs args) {
        var service = args.getString(); //eg: sys.HelloService.sayHello
        var path    = service.split("\\.");
        if (path.length != 3)
            throw new RuntimeException("Service format error");

        var appNode = hub.designTree.findApplicationNodeByName(path[0]);
        if (appNode == null)
            throw new RuntimeException("Can't find Application");
        var serviceNode = hub.designTree.findModelNodeByName(appNode.model.id(), ModelType.Service, path[1]);
        if (serviceNode == null)
            throw new RuntimeException("Can't find Service");

        var methodInfo = hub.typeSystem.languageServer.findServiceMethod(serviceNode, path[2]);
        if (methodInfo == null)
            throw new RuntimeException("Can't find Service method");

        //TODO:暂兼容旧前端代码转换为字符串
        return CompletableFuture.completedFuture(JSON.toJSONString(methodInfo));
    }

}
