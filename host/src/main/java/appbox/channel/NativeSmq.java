package appbox.channel;

import com.sun.jna.*;

public class NativeSmq {
    static {
        Native.register("smq");
    }

    //====打开/关闭====
    public static native Pointer SMQ_Open(String name);

    public static native void SMQ_Close(Pointer smq);

    public static native Pointer SMQ_GetBufferPtr(Pointer smq);

    //====读====
    public static native Pointer SMQ_GetNodeForReading(Pointer smq, int timeout);

    public static native void SMQ_ReturnNode(Pointer smq, Pointer node);

    //====写====
    public static native Pointer SMQ_GetNodeForWriting(Pointer smq, int timeout);

    public static native void SMQ_PostNode(Pointer smq, Pointer node);

    //====测试用====
    public static native void SMQ_Test(String name);

    //====辅助方法====
    public static int getNodeOffset(Pointer node) {
        return node.getInt(16);
    }

    public static Pointer getDataPtr(Pointer chunk) {
        return chunk.share(16);
    }

    public static byte getMsgType(Pointer chunk) {
        return chunk.getByte(0);
    }

    public static void setMsgType(Pointer chunk, byte type) {
        chunk.setByte(0, type);
    }

    public static void setMsgFlag(Pointer chunk, byte flag) {
        chunk.setByte(1, flag);
    }

    public static void setMsgDataLen(Pointer chunk, short len) {
        chunk.setShort(2, len);
    }

    public static int getMsgId(Pointer chunk) {
        return chunk.getInt(4);
    }

    public static void setMsgId(Pointer chunk, int id) {
        chunk.setInt(4, id);
    }

    public static void setMsgFirst(Pointer chunk, Pointer first) {
        chunk.setPointer(232, first);
    }

    public static void setMsgNext(Pointer chunk, Pointer next) {
        chunk.setPointer(240, next);
    }
}

//public interface NativeSmq extends Library {
//    NativeSmq INSTANCE = (NativeSmq) Native.load("smq", NativeSmq.class);
//    public void SMQ_Test(String name);
//}
