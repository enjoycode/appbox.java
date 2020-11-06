package appbox.serialization;

import appbox.cache.ObjectPool;
import appbox.utils.IdUtil;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

public final class BinSerializer extends OutputStream implements IEntityMemberWriter /*暂继承OutputStream方便写Json*/ {
    //region ====ObjectPool====
    private static final ObjectPool<BinSerializer> pool = new ObjectPool<>(BinSerializer::new, 32);

    public static BinSerializer rentFromPool(IOutputStream stream) {
        var obj = pool.rent();
        obj._stream = stream;
        return obj;
    }

    public static void backToPool(BinSerializer obj) {
        obj._stream = null;
        pool.back(obj);
    }
    //endregion

    private IOutputStream _stream;

    private BinSerializer() {
    }

    public void serialize(Object obj, int fieldId) throws Exception {
        _stream.writeVariant(fieldId);
        serialize(obj);
    }

    /**
     * 序列化对象，写入类型信息
     */
    public void serialize(Object obj) throws Exception {
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
            throw new Exception("暂未实现未知类型的反射序列化: " + type.getName());
        }

        //TODO:是否已序列化过

        //写入类型信息
        _stream.writeByte(serializer.payloadType);
        //TODO:写入附加类型信息

        //写入数据 TODO:先加入已序列化列表
        serializer.write(this, obj);
    }

    /**
     * 写入原始数据
     */
    @Override
    public void write(byte[] src, int offset, int count) {
        _stream.write(src, offset, count);
    }

    public void writeBool(boolean value) throws Exception {
        _stream.writeBool(value);
    }

    public void writeBool(boolean value, int fieldId) throws Exception {
        _stream.writeVariant(fieldId);
        _stream.writeBool(value);
    }

    public void writeByte(byte value) throws Exception {
        _stream.writeByte(value);
    }

    public void writeByte(byte value, int fieldId) throws Exception {
        _stream.writeVariant(fieldId);
        _stream.writeByte(value);
    }

    public void writeShort(short value) throws Exception {
        _stream.writeShort(value);
    }

    public void writeShort(short value, int fieldId) throws Exception {
        _stream.writeVariant(fieldId);
        _stream.writeShort(value);
    }

    public void writeInt(int value) throws Exception {
        _stream.writeInt(value);
    }

    public void writeInt(int value, int fieldId) throws Exception {
        _stream.writeVariant(fieldId);
        _stream.writeInt(value);
    }

    /**
     * 大字节序写入
     */
    public void writeIntBE(int value) throws Exception {
        _stream.writeByte((byte) (value >>> 24));
        _stream.writeByte((byte) (value >>> 16));
        _stream.writeByte((byte) (value >>> 8));
        _stream.writeByte((byte) (value));
    }

    public void writeLong(long value) throws Exception {
        _stream.writeLong(value);
    }

    public void writeLong(long value, int fieldId) throws Exception {
        _stream.writeVariant(fieldId);
        _stream.writeLong(value);
    }

    /**
     * 大字序写入
     */
    public void writeLongBE(long value) throws Exception {
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

    public void writeVariant(int value, int fieldId) throws Exception {
        _stream.writeVariant(fieldId);
        _stream.writeVariant(value);
    }

    public void writeNativeVariant(int value) {
        _stream.writeNativeVariant(value);
    }

    /** 写入带长度信息的字节数组 */
    public void writeByteArray(byte[] value) throws Exception {
        _stream.writeByteArray(value);
    }

    public void writeString(String value) throws Exception {
        _stream.writeString(value);
    }

    public void writeString(String value, int fieldId) throws Exception {
        _stream.writeVariant(fieldId);
        _stream.writeString(value);
    }

    public void finishWriteFields() throws Exception {
        _stream.writeVariant(0);
    }

    //region ===OutputStream====
    @Override
    public void write(int i) throws IOException {
        try {
            writeByte((byte) i);
        } catch (Exception e) {
            throw new IOException(e.getMessage());
        }
    }
    //endregion

    //region ====IEntityMemberWriter====
    private static final byte SF_WRITE_NULL    = 2;
    private static final byte SF_ORDER_BY_DESC = 4;

    /** 写入与存储一致的3字节长度(小字节序) */
    private void writeStoreVarLen(int len) {
        _stream.writeByte((byte) (len & 0xFF));
        _stream.writeByte((byte) ((len >>> 8) & 0xFF));
        _stream.writeByte((byte) ((len >>> 16) & 0xFF));
    }

    @Override
    public void writeMember(short id, String value, byte storeFlags) throws Exception {
        if (storeFlags != 0) {
            if (value != null) {
                writeShort((short) (id | IdUtil.STORE_FIELD_VAR_FLAG));
                //TODO:优化写utf8,另判断长度超出范围
                var bytes = value.getBytes(StandardCharsets.UTF_8);
                writeStoreVarLen(bytes.length);
                write(bytes);
            } else if ((storeFlags & SF_WRITE_NULL) == SF_WRITE_NULL) {
                writeShort((short) (id | IdUtil.STORE_FIELD_NULL_FLAG));
            }
        } else {
            writeShort(id);
            writeString(value);
        }
    }

    @Override
    public void writeMember(short id, int value, byte storeFlags) throws Exception {
        writeShort(storeFlags == 0 ? id : (short) (id | 4));
        writeInt(value);
    }

    @Override
    public void writeMember(short id, Optional<Integer> value, byte storeFlags) throws Exception {
        if (value.isPresent()) {
            writeMember(id, value.get(), storeFlags);
        } else if (storeFlags != 0 && (storeFlags & SF_WRITE_NULL) == SF_WRITE_NULL) {
            writeShort((short) (id | IdUtil.STORE_FIELD_NULL_FLAG));
        }
    }
    //endregion

}
