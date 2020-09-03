package appbox.serialization;

public interface IInputStream {

    /**
     * 流内剩余字节数
     */
    int remaining();

    byte readByte() throws Exception;

    void read(byte[] dest, int offset, int count) throws Exception;

    /**
     * 跳过指定字节数
     */
    default void skip(int size) throws Exception {
        for (int i = 0; i < size; i++) {
            readByte();
        }
    }

    default boolean readBool() throws Exception {
        return readByte() == 1;
    }

    default short readShort() throws Exception {
        return (short) (Byte.toUnsignedInt(readByte()) | Byte.toUnsignedInt(readByte()) << 8);
    }

    default int readInt() throws Exception {
        return Byte.toUnsignedInt(readByte()) | Byte.toUnsignedInt(readByte()) << 8
                | Byte.toUnsignedInt(readByte()) << 16 | Byte.toUnsignedInt(readByte()) << 24;
    }

    default long readLong() throws Exception {
        return Byte.toUnsignedLong(readByte()) | Byte.toUnsignedLong(readByte()) << 8
                | Byte.toUnsignedLong(readByte()) << 16 | Byte.toUnsignedLong(readByte()) << 24
                | Byte.toUnsignedLong(readByte()) << 32 | Byte.toUnsignedLong(readByte()) << 40
                | Byte.toUnsignedLong(readByte()) << 48 | Byte.toUnsignedLong(readByte()) << 56;
    }

    default int readVariant() throws Exception {
        int data = readNativeVariant();
        return -(data & 1) ^ ((data >>> 1) & 0x7fffffff);
    }

    default int readNativeVariant() throws Exception {
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
            throw new Exception("out of range");
        }
        return data;
    }

    default String readString() throws Exception {
        int len = readVariant();
        if (len == -1) {
            return null;
        } else if (len == 0) {
            return "";
        } else {
            return readUtf8(len);
        }
    }

    private String readUtf8(int chars) throws Exception {
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
                    throw new Exception();
                else
                    dst[dp++] = (char) (((b1 << 6) ^ b2) ^ (((byte) 0xC0 << 6) ^ ((byte) 0x80)));
            } else if ((b1 >> 4) == -2) {
                // 3 bytes, 16 bits: 1110xxxx 10xxxxxx 10xxxxxx
                b2 = readByte();
                b3 = readByte();
                if ((b1 == (byte) 0xe0 && (b2 & 0xe0) == 0x80) //
                        || (b2 & 0xc0) != 0x80 //
                        || (b3 & 0xc0) != 0x80) { // isMalformed3(b1, b2, b3)
                    throw new Exception();
                } else {
                    c = (char) ((b1 << 12) ^ (b2 << 6) ^ (b3 ^ (((byte) 0xE0 << 12) ^ ((byte) 0x80 << 6) ^ ((byte) 0x80))));
                    if (c >= '\uD800' && c < ('\uDFFF' + 1))
                        throw new Exception();
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
                    throw new Exception();
                } else {
                    dst[dp++] = Character.highSurrogate(uc);
                    dst[dp++] = Character.lowSurrogate(uc);
                }
            } else {
                throw new Exception();
            }
        }

        return new String(dst);
    }
}
