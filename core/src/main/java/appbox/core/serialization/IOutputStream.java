package appbox.core.serialization;

public interface IOutputStream {

    void writeByte(byte value);

    void write(byte[] value);

    default void writeVariant(int value) {
        value = (value << 1) ^ (value >> 0x1F);
        do {
            byte temp = (byte) ((value & 0x7F) | 0x80);
            if ((value >>= 7) != 0) {
                writeByte(temp);
            } else {
                temp = (byte) (temp & 0x7F);
                writeByte(temp);
                break;
            }
        } while (true);
    }

    default void writeString(String value) throws Exception {
        if (value == null) {
            writeVariant(-1);
        } else if (value.isEmpty()) {
            writeVariant(0);
        } else {
            //TODO:暂简单实现，待优化
            var uft8Bytes = value.getBytes("UTF-8");
            writeVariant(uft8Bytes.length); //写入字节数
            write(uft8Bytes);
        }
    }

}
