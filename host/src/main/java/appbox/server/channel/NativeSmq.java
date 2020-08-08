package appbox.server.channel;

import com.sun.jna.*;

public class NativeSmq {
    static {
        Native.register("smq");
    }

    //====MessageChunk常量====
    public static final int CHUNK_DATA_SIZE = 216;

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

    //====辅助读写Chunk方法(TODO:考虑移至MessageChunk类内)====
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

    public static short getMsgDataLen(Pointer chunk) {
        return chunk.getShort(2);
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

    public static long getMsgSource(Pointer chunk) {
        return chunk.getLong(8);
    }

    public static void setMsgSource(Pointer chunk, long source) {
        chunk.setLong(8, source);
    }

    public static Pointer getMsgFirst(Pointer chunk) {
        return chunk.getPointer(232);
    }

    public static void setMsgFirst(Pointer chunk, Pointer first) {
        chunk.setPointer(232, first);
    }

    public static Pointer getMsgNext(Pointer chunk) {
        return chunk.getPointer(240);
    }

    public static void setMsgNext(Pointer chunk, Pointer next) {
        chunk.setPointer(240, next);
    }


    private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();

    /**
     * 获取单包的调试信息
     */
    public static String getDebugInfo(Pointer chunk, boolean withData) {
        var sb = new StringBuilder();
        sb.append("TYP=");
        sb.append(getMsgType(chunk));
        sb.append(" ID=");
        sb.append(getMsgId(chunk));
        sb.append(" FLAG=");
        sb.append(getMsgFlag(chunk));
        sb.append(" LEN=");
        sb.append(getMsgDataLen(chunk));
        sb.append(" CUR=");
        sb.append(Long.toHexString(Pointer.nativeValue(chunk)));
        sb.append(" FST=");
        sb.append(Long.toHexString(Pointer.nativeValue(getMsgFirst(chunk))));
        sb.append(" NXT=");
        sb.append(Long.toHexString(Pointer.nativeValue(getMsgNext(chunk))));
        sb.append("\n");
        if (withData) {
            var dataPtr = getDataPtr(chunk);
            var dataLen = getMsgDataLen(chunk);
            sb.append("--------DataStart--------\n");
            int  v;
            char c1, c2;
            for (int i = 0; i < dataLen; i++) {
                v = dataPtr.getByte(i) & 0xFF;
                c1 = HEX_ARRAY[v >>> 4];
                c2 = HEX_ARRAY[v & 0x0F];
                sb.append(c1);
                sb.append(c2);
                if (i % 4 == 3) sb.append(" ");
            }
            sb.append("\n---------DataEnd---------\n");
        }
        return sb.toString();
    }
}

//public interface NativeSmq extends Library {
//    NativeSmq INSTANCE = (NativeSmq) Native.load("smq", NativeSmq.class);
//    public void SMQ_Test(String name);
//}
