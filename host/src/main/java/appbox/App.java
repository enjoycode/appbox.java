package appbox;

import appbox.server.channel.SharedMemoryChannel;

public class App {

    public static void main(String[] args) {
        //System.setProperty("jna.debug_load", "true");
        //System.setProperty("jna.library.path",dllResourcePath);
        //System.setProperty("jna.platform.library.path",dllResourcePath);
        System.out.println("Java AppHost running...");

        // 新建Channel并开始阻塞接收
        var channel = new SharedMemoryChannel("AppChannel");
        channel.startReceive();

        System.out.println("Java AppHost stopped.");
    }

}