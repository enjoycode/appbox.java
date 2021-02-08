package appbox.design.handlers.service;

import appbox.design.DesignHub;
import appbox.design.handlers.IDesignHandler;
import appbox.design.services.PublishService;
import appbox.design.utils.PathUtil;
import appbox.logging.Log;
import appbox.model.ModelType;
import appbox.model.ServiceModel;
import appbox.runtime.InvokeArgs;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

/** 开始调试服务模型 */
public final class StartDebugging implements IDesignHandler {

    @Override
    public CompletableFuture<Object> handle(DesignHub hub, InvokeArgs args) {
        var modelId     = args.getString();
        var methodName  = args.getString();
        var methodArgs  = args.getString(); //TODO:
        var breakPoints = args.getString();
        Log.debug(breakPoints);

        //先编译服务模型,将编译结果保存至当前会话的调试目录内，同时保存当前会话至文件
        var serviceNode = hub.designTree.findModelNode(ModelType.Service, Long.parseUnsignedLong(modelId));
        try {
            var pkg             = PublishService.compileService(hub, (ServiceModel) serviceNode.model(), true);
            var sessionIdString = Long.toUnsignedString(hub.session.sessionId());
            var dbgPath         = PathUtil.getDebugPath(sessionIdString);
            if (Files.exists(dbgPath)) {
                cleanDebugPath(dbgPath);
            } else {
                Files.createDirectories(dbgPath);
            }
            //写入编译好的服务组件
            var dbgFile = dbgPath.resolve(
                    serviceNode.appNode.model.name() + "." + serviceNode.model().name() + ".bin");
            Files.write(dbgFile, pkg);
            //写入当前会话信息
            var sessionFile = dbgPath.resolve("session.bin");
            Files.write(sessionFile, hub.session.getSerializedData());
        } catch (Exception ex) {
            Log.error("Save debug session and service file error: " + ex);
            throw new RuntimeException(ex);
        }

        //启动调试器与目标子进程
        return hub.debugService().startDebugger(serviceNode.appNode.model.name()
                , serviceNode.model().name(), methodName, null, breakPoints) //TODO: args
                .thenApply(r -> null);
    }

    private static void cleanDebugPath(Path dbgPath) throws IOException {
        Files.list(dbgPath).forEach(path -> {
            try {
                Files.delete(path);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

}
