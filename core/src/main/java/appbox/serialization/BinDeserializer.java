package appbox.serialization;

import appbox.cache.ObjectPool;
import appbox.data.EntityId;
import appbox.logging.Log;
import appbox.utils.IdUtil;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.function.IntFunction;
import java.util.function.Supplier;

public final class BinDeserializer implements IEntityMemberReader {
    //region ====ObjectPool====
    private static final ObjectPool<BinDeserializer> pool = new ObjectPool<>(BinDeserializer::new, 32);

    public static BinDeserializer rentFromPool(IInputStream stream) {
        var obj = pool.rent(); obj._stream = stream; return obj;
    }

    public static void backToPool(BinDeserializer obj) {
        obj._stream = null;
        pool.back(obj);
    }
    //endregion

    //region ====static helper====
    public static Object deserialize(byte[] data) {
        var stream = new BytesInputStream(data);
        var bs     = rentFromPool(stream);
        try {
            return bs.deserialize();
        } finally {
            backToPool(bs);
        }
    }
    //endregion

    private BinDeserializer() {
    }

    private IInputStream _stream;
    //已经序列化或反序列化的对象实例列表
    private List<Object> _objRefItems;

    public Object deserialize() {
        var payloadType = _stream.readByte(); if (payloadType == PayloadType.Null) return null;
        else if (payloadType == PayloadType.BooleanTrue) return Boolean.TRUE;
        else if (payloadType == PayloadType.BooleanFalse) return Boolean.FALSE;
        else if (payloadType == PayloadType.ObjectRef) throw new RuntimeException("TODO");

        TypeSerializer serializer = null;
        if (payloadType == PayloadType.ExtKnownType) throw new RuntimeException("TODO");
        else serializer = TypeSerializer.getSerializer(payloadType);
        if (serializer == null) throw new RuntimeException("待实现未知类型反序列化");

        //读取附加类型信息并创建实例
        if (serializer.creator == null && payloadType != PayloadType.Array //非数组类型
            /*&& serializer.genericTypeCount <= 0 //非范型类型*/) {
            return serializer.read(this, null);
        } else { //其他需要创建实例的类型
            Object result = null;
            //TODO: 先处理数组及范型类型
            result = serializer.creator.get();
            //addToObjectRefs(result);
            serializer.read(this, result); return result;
        }
    }

    public void skip(int size) {
        _stream.skip(size);
    }

    public boolean hasRemaining() {
        return _stream.hasRemaining();
    }

    /** 读剩余字节，没有返回null */
    public byte[] readRemaining() {
        int left = _stream.remaining();
        if (left <= 0) {
            return null;
        }
        var data = new byte[left];
        _stream.read(data, 0, data.length);
        return data;
    }

    public boolean readBool() {
        return _stream.readBool();
    }

    public byte readByte() {
        return _stream.readByte();
    }

    public short readShort() {
        return _stream.readShort();
    }

    public int readInt() {
        return _stream.readInt();
    }

    public long readLong() {
        return _stream.readLong();
    }

    public int readVariant() {
        return _stream.readVariant();
    }

    public int readNativeVariant() {
        return _stream.readNativeVariant();
    }

    public byte[] readByteArray() {
        return _stream.readByteArray();
    }

    public String readString() {
        return _stream.readString();
    }

    public UUID readUUID() {
        return _stream.readUUID();
    }

    public void read(byte[] buffer, int offset, int count) {
        _stream.read(buffer, offset, count);
    }

    //region ====Collections====
    //private static <E extends IBinSerializable> Supplier<E> getCreator(Class<E> clazz) {
    //    var serializer = TypeSerializer.getSerializer(clazz);
    //
    //    Supplier<E> creator = null;
    //    if (serializer != null) {
    //        creator = () -> (E) serializer.creator;
    //    } else {
    //        try {
    //            var ctor = clazz.getDeclaredConstructor();
    //            creator = () -> {
    //                try {
    //                    return ctor.newInstance();
    //                } catch (Exception e) {
    //                    throw new RuntimeException(e);
    //                }
    //            };
    //        } catch (Exception ex) {
    //            throw new RuntimeException(ex);
    //        }
    //    }
    //    return creator;
    //}

    public <E extends IBinSerializable> E[] readArray(IntFunction<E[]> arrayMaker, Supplier<E> elementMaker) {
        var count = readVariant();
        if (count == -1)
            return null;
        //else if (count == -2)

        E[] array = arrayMaker.apply(count);
        for (int i = 0; i < count; i++) {
            if (_stream.readBool()) {
                array[i] = elementMaker.get();
                array[i].readFrom(this);
            }
        }
        return array;
    }

    public <E extends IBinSerializable> List<E> readList(Supplier<E> elementMaker) {
        var count = readVariant();
        if (count == -1)
            return null;
        //else if (count == -2)

        var list    = new ArrayList<E>(count);
        //addToObjectRefs(list);
        for (int i = 0; i < count; i++) {
            if (_stream.readBool()) {
                var element = elementMaker.get();
                element.readFrom(this);
                list.add(element);
            } else {
                list.add(null);
            }
        }
        return list;
    }

    private void addToObjectRefs(Object obj) {
        if (_objRefItems == null)
            _objRefItems = new ArrayList<>();

        _objRefItems.add(obj);
    }
    //endregion

    //region ====IEntityMemberReader====

    /** 读取与存储一致的3字节长度(小字节序) */
    public int readStoreVarLen() {
        var byte1 = readByte() & 255;
        var byte2 = readByte() & 255;
        var byte3 = readByte() & 255;
        return byte3 << 16 | byte2 << 8 | byte1;
    }

    @Override
    public String readStringMember(int flags) {
        if (flags == 0)
            return _stream.readString();
        int size  = flags >>> 8; //TODO:优化读utf8
        var bytes = new byte[size];
        _stream.read(bytes, 0, size);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    @Override
    public boolean readBoolMember(int flags) {
        if (flags == 0)
            return _stream.readBool();
        return (flags & IdUtil.STORE_FIELD_BOOL_TRUE_FLAG) == IdUtil.STORE_FIELD_BOOL_TRUE_FLAG;
    }

    @Override
    public int readIntMember(int flags) {
        return _stream.readInt();
    }

    @Override
    public byte readByteMember(int flags) {
        return _stream.readByte();
    }

    @Override
    public long readLongMember(int flags) {
        return _stream.readLong();
    }

    @Override
    public UUID readUUIDMember(int flags) {
        return new UUID(_stream.readLong(), _stream.readLong());
    }

    @Override
    public byte[] readBinaryMember(int flags) {
        if (flags == 0)
            return _stream.readByteArray();
        int size  = flags >>> 8;
        var bytes = new byte[size];
        if (size > 0)
            _stream.read(bytes, 0, size);
        return bytes;
    }

    @Override
    public Date readDateMember(int flags) {
        return new Date(_stream.readLong());
    }

    @Override
    public EntityId readEntityIdMember(int flags) {
        var id = new EntityId();
        id.readFrom(this);
        return id;
    }

    //endregion

}
