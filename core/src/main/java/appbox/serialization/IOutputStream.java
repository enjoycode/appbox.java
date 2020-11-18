package appbox.serialization;

public interface IOutputStream {

    void writeByte(byte value);

    void write(byte[] src, int offset, int count);

    default void writeBool(boolean value) {
        writeByte(value ? (byte) 1 : (byte) 0);
    }

    default void writeShort(short value) {
        writeByte((byte) (value & 0xFF));
        writeByte((byte) ((value >> 8) & 0xFF));
    }

    default void writeInt(int value) {
        writeByte((byte) (value & 0xFF));
        writeByte((byte) ((value >> 8) & 0xFF));
        writeByte((byte) ((value >> 16) & 0xFF));
        writeByte((byte) ((value >> 24) & 0xFF));
    }

    default void writeLong(long value) {
        writeByte((byte) (value & 0xFF));
        writeByte((byte) ((value >> 8) & 0xFF));
        writeByte((byte) ((value >> 16) & 0xFF));
        writeByte((byte) ((value >> 24) & 0xFF));
        writeByte((byte) ((value >> 32) & 0xFF));
        writeByte((byte) ((value >> 40) & 0xFF));
        writeByte((byte) ((value >> 48) & 0xFF));
        writeByte((byte) ((value >> 56) & 0xFF));
    }

    default void writeVariant(int value) {
        value = (value << 1) ^ (value >> 0x1F);
        writeNativeVariant(value);
    }

    /** 写入与C++一致的可变长度整数 */
    default void writeNativeVariant(int value) {
        do {
            byte temp = (byte) ((value & 0x7F) | 0x80);
            if ((value >>>= 7) != 0) {
                writeByte(temp);
            } else {
                temp = (byte) (temp & 0x7F);
                writeByte(temp);
                break;
            }
        } while (true);
    }

    /**
     * 写入带长度信息的字节数组
     */
    default void writeByteArray(byte[] value) {
        if (value == null) {
            writeVariant(-1);
        } else {
            writeVariant(value.length);
            if (value.length > 0) {
                write(value, 0, value.length);
            }
        }
    }

    /**
     * 写入带长度信息(字符数)且Utf8编码的字符串
     */
    default void writeString(String value) {
        if (value == null) {
            writeVariant(-1);
        } else if (value.isEmpty()) {
            writeVariant(0);
        } else {
            writeVariant(value.length()); //注意写入字符数量，非编码后的字节数量
            writeUtf8(value);
        }
    }

    private void writeUtf8(String value) {
        int  srcPos = 0;
        int  srcLen = value.length();
        char c, d;
        int  uc, ip;
        while (srcPos < srcLen) {
            c = value.charAt(srcPos++);
            if (c < 0x80) {
                // Have at most seven bits
                writeByte((byte) c);
            } else if (c < 0x800) {
                // 2 bytes, 11 bits
                writeByte((byte) (0xc0 | (c >> 6)));
                writeByte((byte) (0x80 | (c & 0x3f)));
            } else if (Character.isSurrogate(c)) {
                ip = srcPos - 1;
                if (Character.isHighSurrogate(c)) {
                    if (srcLen - ip < 2) {
                        uc = -1;
                    } else {
                        d = value.charAt(ip + 1);
                        if (Character.isLowSurrogate(d)) {
                            uc = Character.toCodePoint(c, d);
                        } else {
                            throw new RuntimeException("utf8 code error");
                        }
                    }
                } else {
                    if (Character.isLowSurrogate(c)) {
                        throw new RuntimeException("utf8 code error");
                    } else {
                        uc = c;
                    }
                }

                if (uc < 0) {
                    writeByte((byte) '?');
                } else {
                    writeByte((byte) (0xf0 | ((uc >> 18))));
                    writeByte((byte) (0x80 | ((uc >> 12) & 0x3f)));
                    writeByte((byte) (0x80 | ((uc >> 6) & 0x3f)));
                    writeByte((byte) (0x80 | (uc & 0x3f)));
                    srcPos++; // 2 chars
                }
            } else {
                // 3 bytes, 16 bits
                writeByte((byte) (0xe0 | ((c >> 12))));
                writeByte((byte) (0x80 | ((c >> 6) & 0x3f)));
                writeByte((byte) (0x80 | (c & 0x3f)));
            }
        }
    }

}
