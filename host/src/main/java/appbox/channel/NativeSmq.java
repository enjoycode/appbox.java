package appbox.channel;

import com.sun.jna.*;

public class NativeSmq {
    static {
        Native.register("smq");
    }

//    public static native Pointer SMQ_Open(String name);

    public static native void SMQ_Test(String name);
}

//public interface NativeSmq extends Library {
//    NativeSmq INSTANCE = (NativeSmq) Native.load("smq", NativeSmq.class);
//
//    public void SMQ_Test(String name);
//}
