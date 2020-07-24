package appbox;

import appbox.core.DemoLibrary;

public class App {

    public static void main(String[] args) {
//        System.setProperty("jna.debug_load", "true");
//        System.setProperty("jna.library.path",dllResourcePath);
//        System.setProperty("jna.platform.library.path",dllResourcePath);

        var lib = new DemoLibrary();
        lib.sayHello();

         appbox.channel.NativeSmq.SMQ_Test("你好 from Java");
//        NativeSmq.INSTANCE.SMQ_Test("String from vm");

        System.out.println("Hello Future!");
    }

}