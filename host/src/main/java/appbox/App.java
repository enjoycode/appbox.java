package appbox;

import appbox.core.runtime.RuntimeContext;
import appbox.server.channel.SharedMemoryChannel;
import appbox.server.runtime.HostRuntimeContext;
import appbox.store.SysStoreApi;

public class App {

    public static void main(String[] args) {
        //System.setProperty("jna.debug_load", "true");
        //System.setProperty("jna.library.path",dllResourcePath);
        //System.setProperty("jna.platform.library.path",dllResourcePath);
        System.out.println("Java AppHost running...");

        // 初始化运行时
        RuntimeContext.init(new HostRuntimeContext(), (short) 1/*TODO: fix peerId*/);

        // 新建Channel并开始阻塞接收
        var channel = new SharedMemoryChannel("AppChannel");
        // 初始化系统存储Api
        SysStoreApi.init(channel);
        channel.startReceive();

        System.out.println("Java AppHost stopped.");
    }

}