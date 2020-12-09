package appbox.serialization;

import appbox.cache.ObjectPool;
import appbox.data.EntityId;
import appbox.utils.IdUtil;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

public final class BinSerializer extends OutputStream implements IEntityMemberWriter /*暂继承OutputStream方便写Json*/ {
    //region ====ObjectPool====
    private static final ObjectPool<BinSerializer> pool = new ObjectPool<>(BinSerializer::new, 32);

    public static BinSerializer rentFromPool(IOutputStream stream) {
        var obj = pool.rent();
        obj._stream = stream;
        return obj;
    }

    public static void backToPool(BinSerializer obj) {
        obj._stream      = null;
        obj._objRefItems = null;
        pool.back(obj);
    }
    //endregion

    //region ====static helper====
    public static ByteArrayOutputStream serializeTo(Object obj, boolean compress) {
        var output = new BytesOutputStream(1024);
        var bs = BinSerializer.rentFromPool(output);
        try {
            bs.serialize(obj);
        } finally {
            BinSerializer.backToPool(bs);
        }
        return output;
    }

    public static byte[] serialize(IBinSerializable obj, boolean compress) {
        return serializeTo(obj, compress).toByteArray();
    }
    //endregion

    private IOutputStream _stream;
    //已经序列化或反序列化的对象实例列表
    private List<Object>  _objRefItems;

    private BinSerializer() {
    }

    public void serialize(Object obj, int fieldId) {
        _stream.writeVariant(fieldId);
        serialize(obj);
    }

    /**
     * 序列化对象，写入类型信息
     */
    public void serialize(Object obj) {
        if (obj == null) {
            _stream.writeByte(PayloadType.Null);
            return;
        } else if (obj == Boolean.TRUE) {
            _stream.writeByte(PayloadType.BooleanTrue);
            return;
        } else if (obj == Boolean.FALSE) {
            _stream.writeByte(PayloadType.BooleanFalse);
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
        _stream.writeByte(serializer.payloadType);
        //TODO:写入附加类型信息
        addToObjectRefs(obj);
        //写入数据
        serializer.write(this, obj);
    }

    /**
     * 写入原始数据
     */
    @Override
    public void write(byte[] src, int offset, int count) {
        _stream.write(src, offset, count);
    }

    public void writeBool(boolean value) {
        _stream.writeBool(value);
    }

    public void writeBool(boolean value, int fieldId) {
        _stream.writeVariant(fieldId);
        _stream.writeBool(value);
    }

    public void writeByte(byte value) {
        _stream.writeByte(value);
    }

    public void writeByte(byte value, int fieldId) {
        _stream.writeVariant(fieldId);
        _stream.writeByte(value);
    }

    public void writeShort(short value) {
        _stream.writeShort(value);
    }

    public void writeShort(short value, int fieldId) {
        _stream.writeVariant(fieldId);
        _stream.writeShort(value);
    }

    public void writeInt(int value) {
        _stream.writeInt(value);
    }

    public void writeInt(int value, int fieldId) {
        _stream.writeVariant(fieldId);
        _stream.writeInt(value);
    }

    /**
     * 大字节序写入
     */
    public void writeIntBE(int value) {
        _stream.writeByte((byte) (value >>> 24));
        _stream.writeByte((byte) (value >>> 16));
        _stream.writeByte((byte) (value >>> 8));
        _stream.writeByte((byte) (value));
    }

    public void writeLong(long value) {
        _stream.writeLong(value);
    }

    public void writeLong(long value, int fieldId) {
        _stream.writeVariant(fieldId);
        _stream.writeLong(value);
    }

    /**
     * 大字序写入
     */
    public void writeLongBE(long value) {
        _stream.writeByte((byte) (value >>> 56));
        _stream.writeByte((byte) (value >>> 48));
        _stream.writeByte((byte) (value >>> 40));
        _stream.writeByte((byte) (value >>> 32));
        _stream.writeByte((byte) (value >>> 24));
        _stream.writeByte((byte) (value >>> 16));
        _stream.writeByte((byte) (value >>> 8));
        _stream.writeByte((byte) (value));
    }

    public void writeVariant(int value) {
        _stream.writeVariant(value);
    }

    public void writeVariant(int value, int fieldId) {
        _stream.writeVariant(fieldId);
        _stream.writeVariant(value);
    }

    public void writeNativeVariant(int value) {
        _stream.writeNativeVariant(value);
    }

    /** 写入带长度信息的字节数组 */
    public void writeByteArray(byte[] value) {
        _stream.writeByteArray(value);
    }

    public void writeString(String value) {
        _stream.writeString(value);
    }

    public void writeString(String value, int fieldId) {
        _stream.writeVariant(fieldId);
        _stream.writeString(value);
    }

    /** 写入不带长度信息的utf8 */
    public void writeUtf8(String value) {
        _stream.writeUtf8(value);
    }

    public void writeUUID(UUID value) {
        _stream.writeUUID(value);
    }

    public void writeUUID(UUID value, int fieldId) {
        _stream.writeVariant(fieldId);
        _stream.writeUUID(value);
    }

    public void finishWriteFields() {
        _stream.writeVariant(0);
    }

    //region ===OutputStream====
    @Override
    public void write(int i) {
        writeByte((byte) i);
    }
    //endregion

    //region ====IEntityMemberWriter====

    /** 写入与存储一致的3字节长度(小字节序) */
    public void writeStoreVarLen(int len) {
        _stream.writeByte((byte) (len & 0xFF));
        _stream.writeByte((byte) ((len >>> 8) & 0xFF));
        _stream.writeByte((byte) ((len >>> 16) & 0xFF));
    }

    @Override
    public void writeMember(short id, String value, byte flags) {
        if (flags != 0) {
            if (value != null) {
                _stream.writeShort((short) (id | IdUtil.STORE_FIELD_VAR_FLAG));
                //TODO:优化写utf8,另判断长度超出范围
                var bytes = value.getBytes(StandardCharsets.UTF_8);
                writeStoreVarLen(bytes.length);
                _stream.write(bytes, 0, bytes.length);
            } else if ((flags & SF_WRITE_NULL) == SF_WRITE_NULL) {
                _stream.writeShort((short) (id | IdUtil.STORE_FIELD_NULL_FLAG));
            }
        } else if (value != null) {
            _stream.writeShort(id);
            _stream.writeString(value);
        }
    }

    @Override
    public void writeMember(short id, byte value, byte flags) {
        _stream.writeShort(flags == 0 ? id : (short) (id | 1));
        _stream.writeByte(value);
    }

    @Override
    public void writeMember(short id, int value, byte flags) {
        _stream.writeShort(flags == 0 ? id : (short) (id | 4));
        _stream.writeInt(value);
    }

    @Override
    public void writeMember(short id, Optional<Integer> value, byte flags) {
        if (value.isPresent()) {
            writeMember(id, value.get(), flags);
        } else if (flags != 0 && (flags & SF_WRITE_NULL) == SF_WRITE_NULL) {
            _stream.writeShort((short) (id | IdUtil.STORE_FIELD_NULL_FLAG));
        }
    }

    @Override
    public void writeMember(short id, long value, byte flags) {
        _stream.writeShort(flags == 0 ? id : (short) (id | 8));
        _stream.writeLong(value);
    }

    @Override
    public void writeMember(short id, UUID value, byte flags) {
        if (flags != 0) {
            if (value != null) {
                _stream.writeShort((short) (id | IdUtil.STORE_FIELD_16_LEN_FLAG));
                _stream.writeLong(value.getMostSignificantBits());
                _stream.writeLong(value.getLeastSignificantBits());
            } else if ((flags & SF_WRITE_NULL) == SF_WRITE_NULL) {
                _stream.writeShort((short) (id | IdUtil.STORE_FIELD_NULL_FLAG));
            }
        } else if (value != null) {
            _stream.writeShort(id);
            _stream.writeLong(value.getMostSignificantBits());
            _stream.writeLong(value.getLeastSignificantBits());
        }
    }

    @Override
    public void writeMember(short id, EntityId value, byte flags) {
        if (flags != 0) {
            if (value != null) {
                _stream.writeShort((short) (id | IdUtil.STORE_FIELD_16_LEN_FLAG));
                value.writeTo(this);
            } else if ((flags & SF_WRITE_NULL) == SF_WRITE_NULL) {
                _stream.writeShort((short) (id | IdUtil.STORE_FIELD_NULL_FLAG));
            }
        } else if (value != null) {
            _stream.writeShort(id);
            value.writeTo(this);
        }
    }

    @Override
    public void writeMember(short id, byte[] value, byte flags) {
        if (flags != 0) {
            if (value != null) {
                _stream.writeShort((short) (id | IdUtil.STORE_FIELD_VAR_FLAG));
                writeStoreVarLen(value.length);
                _stream.write(value, 0, value.length);
            } else if ((flags & SF_WRITE_NULL) == SF_WRITE_NULL) {
                _stream.writeShort((short) (id | IdUtil.STORE_FIELD_NULL_FLAG));
            }
        } else if (value != null) {
            _stream.writeShort(id);
            _stream.writeByteArray(value);
        }
    }

    @Override
    public void writeMember(short id, boolean value, byte flags) {
        if (flags != 0) {
            if (value) {
                _stream.writeShort((short) (id | IdUtil.STORE_FIELD_BOOL_TRUE_FLAG));
            } else {
                _stream.writeShort((short) (id | IdUtil.STORE_FIELD_BOOL_FALSE_FLAG));
            }
        } else {
            _stream.writeShort(id);
            _stream.writeBool(value);
        }
    }

    @Override
    public void writeMember(short id, Date value, byte flags) {
        if (flags != 0) {
            if (value != null) {
                _stream.writeShort((short) (id | 8));
                _stream.writeLong(value.getTime());
            } else if ((flags & SF_WRITE_NULL) == SF_WRITE_NULL) {
                _stream.writeShort((short) (id | IdUtil.STORE_FIELD_NULL_FLAG));
            }
        } else if (value != null) {
            _stream.writeShort(id);
            _stream.writeLong(value.getTime());
        }
    }

    public void writeList(List list, int fieldId) {
        _stream.writeVariant(fieldId);
        if (checkNullOrSerialized(list))
            return;

        addToObjectRefs(list);
        write(list.size());
        writeCollection(list);
    }

    private void writeCollection(List list) {
        if (list.size() == 0)
            return;

        var type = list.get(0).getClass();
        //尝试获取elementType有没有相应的序列化实现存在
        var serializer = TypeSerializer.getSerializer(type);
        if (serializer == null || type != String.class) //引用类型，注意：elementType == typeof(Object)没有序列化实现
        {
            for (int i = 0; i < list.size(); i++) {
                serialize(list.get(i));
            }
        } else //值类型
        {
            for (int i = 0; i < list.size(); i++) {
                serializer.write(this, list.get(i));
            }
        }
    }

    private boolean checkNullOrSerialized(Object obj) {
        if (obj == null) {
            write(-1);
            return true;
        }

        int index = indexOfObjectRefs(obj);
        if (index > -1) {
            write(-2);
            write(index);
            return true;
        }

        //注意：不能在这里AddToObjectRefs(obj);
        return false;
    }

    private void addToObjectRefs(Object obj) {
        if (_objRefItems == null)
            _objRefItems = new ArrayList<>();

        _objRefItems.add(obj);
    }

    private int indexOfObjectRefs(Object obj) {
        if (_objRefItems == null || _objRefItems.size() == 0)
            return -1;

        for (int i = 0; i < _objRefItems.size(); i++) {
            if (_objRefItems.get(i).equals(obj))
                return i;
        }
        return -1;
    }

    //endregion

}
