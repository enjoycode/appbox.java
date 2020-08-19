package appbox.core.model.entity;

import appbox.core.data.PersistentState;
import appbox.core.model.EntityModel;
import appbox.core.serialization.BinDeserializer;
import appbox.core.serialization.BinSerializer;

/**
 * 系统存储的二级索引
 */
public final class SysIndexModel extends IndexModelBase {
    public enum SysIndexState {
        /**
         * 索引已构建完
         */
        Ready((byte) 0),
        /**
         * 索引正在构建中
         */
        Building((byte) 1),
        /**
         * 比如违反惟一性约束或超出长度限制导致异步生成失败
         */
        BuildFailed((byte) 2);

        private final byte _value;

        SysIndexState(byte value) {
            _value = value;
        }

        static SysIndexState fromValue(byte value) throws Exception {
            switch (value) {
                case 0:
                    return Ready;
                case 1:
                    return Building;
                case 2:
                    return BuildFailed;
                default:
                    throw new RuntimeException();
            }
        }
    }

    private boolean       _global;  //是否全局索引,不支持非Mvcc表全局索引无法保证一致性
    private SysIndexState _state;   //索引异步构建状态

    public SysIndexModel(EntityModel owner, String name, boolean unique,
                         FieldWithOrder[] fields, short[] storingFields) {
        super(owner, name, unique, fields, storingFields);
        _global = false; //TODO:暂不支持全局索引
        _state  = owner.persistentState() == PersistentState.Detached ? SysIndexState.Ready : SysIndexState.Building;
    }

    @Override
    public void writeTo(BinSerializer bs) throws Exception {
        super.writeTo(bs);

        bs.writeBool(_global, 1);
        bs.writeByte(_state._value, 2);
        bs.finishWriteFields();
    }

    @Override
    public void readFrom(BinDeserializer bs) throws Exception {
        super.readFrom(bs);

        int propIndex;
        do {
            propIndex = bs.readVariant();
            switch (propIndex) {
                case 0:
                    _global = bs.readBool();
                    break;
                case 1:
                    _state = SysIndexState.fromValue(bs.readByte());
                    break;
                default:
                    throw new RuntimeException("Unknown field id: " + propIndex);
            }
        } while (propIndex != 0);
    }
}
