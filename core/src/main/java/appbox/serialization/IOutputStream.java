package appbox.serialization;

import appbox.data.EntityId;
import appbox.utils.IdUtil;

import java.nio.charset.StandardCharsets;
import java.util.*;

public interface IOutputStream extends IEntityMemberWriter {

    //region ====abstract====
    void writeByte(byte value);

    /** 写入原始数据 */
    void write(byte[] src, int offset, int count);
    //endregion

    //region ====Serialize(写入类型信息头) Methods====
    default void serialize(Object obj, int fieldId) {
        writeVariant(fieldId);
        serialize(obj);
    }

    default void serialize(Object obj) {
        if (obj == null) {
            writeByte(PayloadType.Null);
            return;
        } else if (obj == Boolean.TRUE) {
            writeByte(PayloadType.BooleanTrue);
            return;
        } else if (obj == Boolean.FALSE) {
            writeByte(PayloadType.BooleanFalse);
            return;
        }

        var type       = obj.getClass();
        var serializer = TypeSerializer.getSerializer(type);
        if (serializer == null) {
            throw new RuntimeException("暂未实现未知类型的反射序列化: " + type.getName());
        }

        //if (checkNullOrSerialized(obj))
        //    return;

        //写入类型信息
        writeByte(serializer.payloadType);
        //TODO:写入附加类型信息

        //addToObjectRefs(obj);
        //写入数据
        serializer.write(this, obj);
    }

    default void serialize(int obj) {
        writeByte(PayloadType.Int32);
        writeInt(obj);
    }

    default void serialize(String obj) {
        if (obj == null) {
            writeByte(PayloadType.Null);
            return;
        }
        writeByte(PayloadType.String);
        writeString(obj);
    }
    //endregion

    //region ====Write Methods====
    default void writeByteField(byte value, int fieldId) {
        writeVariant(fieldId);
        writeByte(value);
    }

    default void writeBool(boolean value) {
        writeByte(value ? (byte) 1 : (byte) 0);
    }

    default void writeBoolField(boolean value, int fieldId) {
        writeVariant(fieldId);
        writeBool(value);
    }

    default void writeShort(short value) {
        writeByte((byte) (value & 0xFF));
        writeByte((byte) ((value >> 8) & 0xFF));
    }

    default void writeShortField(short value, int fieldId) {
        writeVariant(fieldId);
        writeShort(value);
    }

    default void writeInt(int value) {
        writeByte((byte) (value & 0xFF));
        writeByte((byte) ((value >> 8) & 0xFF));
        writeByte((byte) ((value >> 16) & 0xFF));
        writeByte((byte) ((value >> 24) & 0xFF));
    }

    default void writeIntField(int value, int fieldId) {
        writeVariant(fieldId);
        writeInt(value);
    }

    /**
     * 大字节序写入
     */
    default void writeIntBE(int value) {
        writeByte((byte) (value >>> 24));
        writeByte((byte) (value >>> 16));
        writeByte((byte) (value >>> 8));
        writeByte((byte) (value));
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

    default void writeLongField(long value, int fieldId) {
        writeVariant(fieldId);
        writeLong(value);
    }

    /**
     * 大字序写入
     */
    default void writeLongBE(long value) {
        writeByte((byte) (value >>> 56));
        writeByte((byte) (value >>> 48));
        writeByte((byte) (value >>> 40));
        writeByte((byte) (value >>> 32));
        writeByte((byte) (value >>> 24));
        writeByte((byte) (value >>> 16));
        writeByte((byte) (value >>> 8));
        writeByte((byte) (value));
    }

    default void writeUUID(UUID value) {
        writeLong(value.getMostSignificantBits());
        writeLong(value.getLeastSignificantBits());
    }

    default void writeUUIDField(UUID value, int fieldId) {
        writeVariant(fieldId);
        writeUUID(value);
    }

    default void writeVariant(int value) {
        value = (value << 1) ^ (value >> 0x1F);
        writeNativeVariant(value);
    }

    default void writeVariantField(int value, int fieldId) {
        writeVariant(fieldId);
        writeVariant(value);
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

    default void writeStringField(String value, int fieldId) {
        writeVariant(fieldId);
        writeString(value);
    }

    /** 写入不带长度信息的Utf8编码的字符串 */
    default void writeUtf8(String value) {
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

    default void finishWriteFields() {
        writeVariant(0);
    }
    //endregion

    //region ====Collections====
    default <E extends IBinSerializable> void writeArray(E[] array, int field, boolean elementNullable) {
        writeVariant(field);
        if (checkNullOrSerialized(array))
            return;

        //addToObjectRefs(array);
        writeVariant(array.length);
        for (E element : array) {
            if (elementNullable)
                writeBool(element != null);
            if (element != null) {
                element.writeTo(this);
            }
        }
    }

    default <E extends IBinSerializable> void writeList(List<E> list, int fieldId, boolean elementNullable) {
        writeVariant(fieldId);
        if (checkNullOrSerialized(list))
            return;

        //addToObjectRefs(list);
        writeVariant(list.size());
        for (E element : list) {
            if (elementNullable)
                writeBool(element != null);
            if (element != null) {
                element.writeTo(this);
            }
        }
    }

    default boolean checkNullOrSerialized(Object obj) {
        if (obj == null) {
            writeVariant(-1);
            return true;
        }

        //int index = indexOfObjectRefs(obj);
        //if (index > -1) {
        //    write(-2);
        //    write(index);
        //    return true;
        //}

        //注意：不能在这里AddToObjectRefs(obj);
        return false;
    }

    //endregion

    //region ====IEntityMemberWriter====

    /** 写入与存储一致的3字节长度(小字节序) */
    default void writeStoreVarLen(int len) {
        writeByte((byte) (len & 0xFF));
        writeByte((byte) ((len >>> 8) & 0xFF));
        writeByte((byte) ((len >>> 16) & 0xFF));
    }

    @Override
    default void writeMember(short id, String value, byte flags) {
        if (flags != 0) {
            if (value != null) {
                writeShort((short) (id | IdUtil.STORE_FIELD_VAR_FLAG));
                //TODO:优化写utf8,另判断长度超出范围
                var bytes = value.getBytes(StandardCharsets.UTF_8);
                writeStoreVarLen(bytes.length);
                write(bytes, 0, bytes.length);
            } else if ((flags & SF_WRITE_NULL) == SF_WRITE_NULL) {
                writeShort((short) (id | IdUtil.STORE_FIELD_NULL_FLAG));
            }
        } else if (value != null) {
            writeShort(id);
            writeString(value);
        }
    }

    @Override
    default void writeMember(short id, byte value, byte flags) {
        writeShort(flags == 0 ? id : (short) (id | 1));
        writeByte(value);
    }

    @Override
    default void writeMember(short id, int value, byte flags) {
        writeShort(flags == 0 ? id : (short) (id | 4));
        writeInt(value);
    }

    @Override
    default void writeMember(short id, Optional<Integer> value, byte flags) {
        if (value.isPresent()) {
            writeMember(id, value.get(), flags);
        } else if (flags != 0 && (flags & SF_WRITE_NULL) == SF_WRITE_NULL) {
            writeShort((short) (id | IdUtil.STORE_FIELD_NULL_FLAG));
        }
    }

    @Override
    default void writeMember(short id, long value, byte flags) {
        writeShort(flags == 0 ? id : (short) (id | 8));
        writeLong(value);
    }

    @Override
    default void writeMember(short id, UUID value, byte flags) {
        if (flags != 0) {
            if (value != null) {
                writeShort((short) (id | IdUtil.STORE_FIELD_16_LEN_FLAG));
                writeLong(value.getMostSignificantBits());
                writeLong(value.getLeastSignificantBits());
            } else if ((flags & SF_WRITE_NULL) == SF_WRITE_NULL) {
                writeShort((short) (id | IdUtil.STORE_FIELD_NULL_FLAG));
            }
        } else if (value != null) {
            writeShort(id);
            writeLong(value.getMostSignificantBits());
            writeLong(value.getLeastSignificantBits());
        }
    }

    @Override
    default void writeMember(short id, EntityId value, byte flags) {
        if (flags != 0) {
            if (value != null) {
                writeShort((short) (id | IdUtil.STORE_FIELD_16_LEN_FLAG));
                value.writeTo(this);
            } else if ((flags & SF_WRITE_NULL) == SF_WRITE_NULL) {
                writeShort((short) (id | IdUtil.STORE_FIELD_NULL_FLAG));
            }
        } else if (value != null) {
            writeShort(id);
            value.writeTo(this);
        }
    }

    @Override
    default void writeMember(short id, byte[] value, byte flags) {
        if (flags != 0) {
            if (value != null) {
                writeShort((short) (id | IdUtil.STORE_FIELD_VAR_FLAG));
                writeStoreVarLen(value.length);
                write(value, 0, value.length);
            } else if ((flags & SF_WRITE_NULL) == SF_WRITE_NULL) {
                writeShort((short) (id | IdUtil.STORE_FIELD_NULL_FLAG));
            }
        } else if (value != null) {
            writeShort(id);
            writeByteArray(value);
        }
    }

    @Override
    default void writeMember(short id, boolean value, byte flags) {
        if (flags != 0) {
            if (value) {
                writeShort((short) (id | IdUtil.STORE_FIELD_BOOL_TRUE_FLAG));
            } else {
                writeShort((short) (id | IdUtil.STORE_FIELD_BOOL_FALSE_FLAG));
            }
        } else {
            writeShort(id);
            writeBool(value);
        }
    }

    @Override
    default void writeMember(short id, Date value, byte flags) {
        if (flags != 0) {
            if (value != null) {
                writeShort((short) (id | 8));
                writeLong(value.getTime());
            } else if ((flags & SF_WRITE_NULL) == SF_WRITE_NULL) {
                writeShort((short) (id | IdUtil.STORE_FIELD_NULL_FLAG));
            }
        } else if (value != null) {
            writeShort(id);
            writeLong(value.getTime());
        }
    }
    //endregion

}
