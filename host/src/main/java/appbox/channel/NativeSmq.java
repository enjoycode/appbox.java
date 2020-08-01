package appbox.channel;

import com.sun.jna.*;

public class NativeSmq {
    static {
        Native.register("smq");
    }

    //====打开/关闭====
    public static native Pointer SMQ_Open(String name);

    public static native void SMQ_Close(Pointer smq);

    //====读====
    public static native Pointer SMQ_GetChunkForReading(Pointer smq, int timeout);

    public static native void SMQ_ReturnChunk(Pointer smq, Pointer chunk);

    public static native void SMQ_ReturnAllChunks(Pointer smq, Pointer first);

    //====写====
    public static native Pointer SMQ_GetChunkForWriting(Pointer smq, int timeout);

    public static native void SMQ_PostChunk(Pointer smq, Pointer chunk);

    //====辅助方法====
    public static Pointer getDataPtr(Pointer chunk) {
        return chunk.share(16);
    }

    public static byte getMsgType(Pointer chunk) {
        return chunk.getByte(0);
    }

    public static void setMsgType(Pointer chunk, byte type) {
        chunk.setByte(0, type);
    }

    public static byte getMsgFlag(Pointer chunk) {
        return chunk.getByte(1);
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

    public static Pointer getMsgFirst(Pointer chunk) { return chunk.getPointer(232); }

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
