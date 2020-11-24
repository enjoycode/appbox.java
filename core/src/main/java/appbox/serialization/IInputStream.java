package appbox.serialization;

import java.util.UUID;

public interface IInputStream {

    /**
     * 流内剩余字节数
     */
    int remaining();

    /** 是否流内有剩余字节 */
    default boolean hasRemaining() {
        return remaining() > 0;
    }

    byte readByte();

    void read(byte[] dest, int offset, int count);

    /**
     * 跳过指定字节数
     */
    default void skip(int size) {
        for (int i = 0; i < size; i++) {
            readByte();
        }
    }

    default boolean readBool() {
        return readByte() == 1;
    }

    default short readShort() {
        return (short) (Byte.toUnsignedInt(readByte()) | Byte.toUnsignedInt(readByte()) << 8);
    }

    default int readInt() {
        return Byte.toUnsignedInt(readByte()) | Byte.toUnsignedInt(readByte()) << 8
                | Byte.toUnsignedInt(readByte()) << 16 | Byte.toUnsignedInt(readByte()) << 24;
    }

    default long readLong() {
        return Byte.toUnsignedLong(readByte()) | Byte.toUnsignedLong(readByte()) << 8
                | Byte.toUnsignedLong(readByte()) << 16 | Byte.toUnsignedLong(readByte()) << 24
                | Byte.toUnsignedLong(readByte()) << 32 | Byte.toUnsignedLong(readByte()) << 40
                | Byte.toUnsignedLong(readByte()) << 48 | Byte.toUnsignedLong(readByte()) << 56;
    }

    default UUID readUUID(){
        return new UUID(readLong(),readLong());
    }

    default int readVariant() {
        int data = readNativeVariant();
        return -(data & 1) ^ ((data >>> 1) & 0x7fffffff);
    }

    default int readNativeVariant() {
        int data = readByte();
        if ((data & 0x80) == 0) {
            return data;
        }
        data &= 0x7F;
        int num2 = readByte();
        data |= (num2 & 0x7F) << 7;
        if ((num2 & 0x80) == 0) {
            return data;
        }
        num2 = readByte();
        data |= (num2 & 0x7F) << 14;
        if ((num2 & 0x80) == 0) {
            return data;
        }
        num2 = readByte();
        data |= (num2 & 0x7F) << 0x15;
        if ((num2 & 0x80) == 0) {
            return data;
        }
        num2 = readByte();
        data |= num2 << 0x1C;
        if ((num2 & 240) != 0) {
            throw new RuntimeException("out of range");
        }
        return data;
    }

    /** 读取带长度信息的字节数组 */
    default byte[] readByteArray() {
        int len = readVariant();
        if (len < 0) return null;
        var bytes = new byte[len];
        read(bytes, 0, len);
        return bytes;
    }

    default String readString() {
        int len = readVariant();
        if (len == -1) {
            return null;
        } else if (len == 0) {
            return "";
        } else {
            return readUtf8(len);
        }
    }

    private String readUtf8(int chars) {
        var  dst = new char[chars]; //TODO:是否能优化
        int  dp  = 0;
        int  b1, b2, b3, b4, uc;
        char c;

        while (dp < chars) {
            b1 = readByte();
            if (b1 >= 0) {
                // 1 byte, 7 bits: 0xxxxxxx
                dst[dp++] = (char) b1;
            } else if ((b1 >> 5) == -2 && (b1 & 0x1e) != 0) {
                // 2 bytes, 11 bits: 110xxxxx 10xxxxxx
                b2 = readByte();
                if ((b2 & 0xc0) != 0x80) // isNotContinuation(b2)
                    throw new RuntimeException("utf8 code error");
                else
                    dst[dp++] = (char) (((b1 << 6) ^ b2) ^ (((byte) 0xC0 << 6) ^ ((byte) 0x80)));
            } else if ((b1 >> 4) == -2) {
                // 3 bytes, 16 bits: 1110xxxx 10xxxxxx 10xxxxxx
                b2 = readByte();
                b3 = readByte();
                if ((b1 == (byte) 0xe0 && (b2 & 0xe0) == 0x80) //
                        || (b2 & 0xc0) != 0x80 //
                        || (b3 & 0xc0) != 0x80) { // isMalformed3(b1, b2, b3)
                    throw new RuntimeException("utf8 code error");
                } else {
                    c = (char) ((b1 << 12) ^ (b2 << 6) ^ (b3 ^ (((byte) 0xE0 << 12) ^ ((byte) 0x80 << 6) ^ ((byte) 0x80))));
                    if (c >= '\uD800' && c < ('\uDFFF' + 1))
                        throw new RuntimeException("utf8 code error");
                    else
                        dst[dp++] = c;
                }
            } else if ((b1 >> 3) == -2) {
                // 4 bytes, 21 bits: 11110xxx 10xxxxxx 10xxxxxx 10xxxxxx
                b2 = readByte();
                b3 = readByte();
                b4 = readByte();
                uc = ((b1 << 18) ^ (b2 << 12) ^ (b3 << 6) ^ (b4 ^ (((byte) 0xF0 << 18) ^ ((byte) 0x80 << 12) ^ ((byte) 0x80 << 6) ^ ((byte) 0x80))));
                if (((b2 & 0xc0) != 0x80 || (b3 & 0xc0) != 0x80 || (b4 & 0xc0) != 0x80) // isMalformed4
                        ||
                        // shortest form check
                        !Character.isSupplementaryCodePoint(uc)) {
                    throw new RuntimeException("utf8 code error");
                } else {
                    dst[dp++] = Character.highSurrogate(uc);
                    dst[dp++] = Character.lowSurrogate(uc);
                }
            } else {
                throw new RuntimeException("utf8 code error");
            }
        }

        return new String(dst);
    }
}
