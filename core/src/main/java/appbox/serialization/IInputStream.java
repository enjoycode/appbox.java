package appbox.serialization;

import appbox.data.Entity;
import appbox.data.EntityId;
import appbox.utils.DateTimeUtil;
import appbox.utils.IdUtil;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.IntFunction;
import java.util.function.Supplier;

public interface IInputStream extends IEntityMemberReader {

    //region ====Abstract====
    byte readByte();

    void readBytes(byte[] dest, int offset, int count);

    /**
     * 流内剩余字节数
     */
    int remaining();

    /** 获取反序列化时的上下文 */
    DeserializeContext getContext();

    //endregion

    //region ====Context====

    /** 用于反序列化时根据实体模型标识创建实体实例 */
    default void setEntityFactory(Map<Long, Supplier<? extends Entity>> factoryMap) {
        final var ctx = getContext();
        if(ctx != null)
            ctx.setEntityFactory(factoryMap);
    }

    /** 添加已反序列化列表，用于解决实体循环引用 */
    default void addToDeserialized(Entity obj) {
        final var ctx = getContext();
        if (ctx != null)
            ctx.addToDeserialized(obj);
    }

    /** 根据序号获取已序列化的实体 */
    default Entity getDeserialized(int index) {
        final var ctx = getContext();
        if (ctx == null)
            throw new RuntimeException("Can't get DeserializeContext");
        return ctx.getDeserialized(index);
    }

    //endregion

    //region ====Deserialize====
    default Object deserialize() {
        var payloadType = readByte();
        if (payloadType == PayloadType.Null) return null;
        else if (payloadType == PayloadType.BooleanTrue) return Boolean.TRUE;
        else if (payloadType == PayloadType.BooleanFalse) return Boolean.FALSE;
        else if (payloadType == PayloadType.ObjectRef) return getDeserialized(readVariant());

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

    default <T extends Entity> T deserializeEntity(Supplier<T> creator) {
        final var payloadType = readByte();
        if (payloadType == PayloadType.Null)
            return null;
        if (payloadType == PayloadType.Entity) {
            final var modelId = readLong(); //先读取模型标识号
            //TODO:creator == null 从流上下文获取,如果还没有解析为KVO
            final var obj = creator.get();
            if (modelId != obj.modelId())
                throw new RuntimeException("EntityModel's id not same");
            obj.readFrom(this);
            return obj;
        }
        if (payloadType == PayloadType.ObjectRef) {
            final var index = readVariant();
            return (T) getDeserialized(index);
        }
        throw new RuntimeException("PayloadType Error");
    }
    //endregion

    //region ====BaseType====

    /** 是否流内有剩余字节 */
    default boolean hasRemaining() {
        return remaining() > 0;
    }

    /** 读剩余字节，没有返回null */
    default byte[] readRemaining() {
        int left = remaining();
        if (left <= 0) {
            return null;
        }
        var data = new byte[left];
        readBytes(data, 0, data.length);
        return data;
    }

    /** 跳过指定字节数 */
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

    default UUID readUUID() {
        return new UUID(readLong(), readLong());
    }

    default EntityId readEntityId() {
        var res = new EntityId(); //TODO:new empty EntityId
        res.readFrom(this);
        return res;
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
        readBytes(bytes, 0, len);
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
    //endregion

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

    default <E extends IBinSerializable> E[] readArray(IntFunction<E[]> arrayMaker
            , Supplier<E> elementMaker, boolean elementNullabe) {
        var count = readVariant();
        if (count == -1)
            return null;
        //else if (count == -2)

        E[] array = arrayMaker.apply(count);
        for (int i = 0; i < count; i++) {
            if (!elementNullabe || readBool()) {
                array[i] = elementMaker.get();
                array[i].readFrom(this);
            }
        }
        return array;
    }

    default <E extends IBinSerializable> List<E> readList(Supplier<E> elementMaker, boolean elementNullable) {
        var count = readVariant();
        if (count == -1)
            return null;
        //else if (count == -2)

        var list = new ArrayList<E>(count);
        //addToObjectRefs(list);
        for (int i = 0; i < count; i++) {
            if (!elementNullable || readBool()) {
                var element = elementMaker.get();
                element.readFrom(this);
                list.add(element);
            } else {
                list.add(null);
            }
        }
        return list;
    }

    default List<String> readListString() {
        final var count = readVariant();
        if (count == -1)
            return null;

        final var list = new ArrayList<String>(count);
        for (int i = 0; i < count; i++) {
            list.add(readString());
        }
        return list;
    }

    default List<Short> readListShort() {
        var count = readVariant();
        if (count == -1)
            return null;

        var list = new ArrayList<Short>(count);
        for (int i = 0; i < count; i++) {
            list.add(readShort());
        }
        return list;
    }
    //endregion

    //region ====IEntityMemberReader====

    /** 读取与存储一致的3字节长度(小字节序) */
    default int readStoreVarLen() {
        var byte1 = readByte() & 255;
        var byte2 = readByte() & 255;
        var byte3 = readByte() & 255;
        return byte3 << 16 | byte2 << 8 | byte1;
    }

    @Override
    default String readStringMember(int flags) {
        if (flags == IEntityMemberWriter.SF_NONE)
            return readString();
        int size  = flags >>> 8; //TODO:优化读utf8
        var bytes = new byte[size];
        readBytes(bytes, 0, size);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    @Override
    default boolean readBoolMember(int flags) {
        if (flags == IEntityMemberWriter.SF_NONE)
            return readBool();
        return (flags & IdUtil.STORE_FIELD_BOOL_TRUE_FLAG) == IdUtil.STORE_FIELD_BOOL_TRUE_FLAG;
    }

    @Override
    default int readIntMember(int flags) {
        return readInt();
    }

    @Override
    default byte readByteMember(int flags) {
        return readByte();
    }

    @Override
    default long readLongMember(int flags) {
        return readLong();
    }

    @Override
    default UUID readUUIDMember(int flags) {
        return new UUID(readLong(), readLong());
    }

    @Override
    default byte[] readBinaryMember(int flags) {
        if (flags == IEntityMemberWriter.SF_NONE)
            return readByteArray();
        int size  = flags >>> 8;
        var bytes = new byte[size];
        if (size > 0)
            readBytes(bytes, 0, size);
        return bytes;
    }

    @Override
    default LocalDateTime readDateMember(int flags) {
        return DateTimeUtil.fromEpochMilli(readLong());
    }

    @Override
    default EntityId readEntityIdMember(int flags) {
        var id = new EntityId();
        id.readFrom(this);
        return id;
    }

    @Override
    default <T extends Entity> T readRefMember(int flags, Supplier<T> creator) {
        return deserializeEntity(creator);
    }

    @Override
    default <T extends Entity> List<T> readSetMember(int flags, Supplier<T> creator) {
        final var count = readVariant();
        final var list  = new ArrayList<T>(count);
        for (int i = 0; i < count; i++) {
            var obj = deserializeEntity(creator);
            list.add(obj);
        }
        return list;
    }

    //endregion

}
