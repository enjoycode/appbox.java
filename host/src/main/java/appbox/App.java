package appbox;

import appbox.channel.SharedMemoryChannel;

public class App {

    public static void main(String[] args) {
        //System.setProperty("jna.debug_load", "true");
        //System.setProperty("jna.library.path",dllResourcePath);
        //System.setProperty("jna.platform.library.path",dllResourcePath);

        // 新建Channel并开始阻塞接收
        var channel = new SharedMemoryChannel("AppChannel");
        channel.startReceive();

        System.out.println("Stopped.");
    }

}