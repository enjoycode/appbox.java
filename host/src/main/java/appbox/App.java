package appbox;

import appbox.runtime.RuntimeContext;
import appbox.channel.SharedMemoryChannel;
import appbox.server.runtime.HostRuntimeContext;
import appbox.store.SysStoreApi;

public class App {

    public static void main(String[] args) {
        //System.setProperty("jna.debug_load", "true");
        //System.setProperty("jna.library.path",dllResourcePath);
        //System.setProperty("jna.platform.library.path",dllResourcePath);

        //先判断是否调试服务
        String debugSessionId = null;
        String channelName    = "AppChannel";
        if (args.length > 0) {
            debugSessionId = args[0];
            channelName    = args[0];
        }

        System.out.println("Java AppHost running...");

        //初始化运行时
        var ctx = new HostRuntimeContext();
        RuntimeContext.init(ctx, (short) 0x1041/*TODO: fix peerId*/);

        //如果调试服务中,预先注入服务实例
        if (debugSessionId != null) {
            ctx.injectDebugService(debugSessionId);
        }

        //连接Channel并开始阻塞接收
        var channel = new SharedMemoryChannel(channelName);
        //初始化系统存储Api
        SysStoreApi.init(channel);
        channel.startReceive();

        System.out.println("Java AppHost stopped.");
    }

}
