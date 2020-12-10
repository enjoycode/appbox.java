package appbox.model.entity;

import appbox.data.PersistentState;
import appbox.model.EntityModel;
import appbox.serialization.BinDeserializer;
import appbox.serialization.IOutputStream;

/**
 * 系统存储的二级索引
 */
public final class SysIndexModel extends IndexModelBase {
    //region ====SysIndexState====
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

        public final byte value;

        SysIndexState(byte value) {
            this.value = value;
        }

        static SysIndexState fromValue(byte value) {
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
    //endregion

    private boolean       _global;  //是否全局索引,不支持非Mvcc表全局索引无法保证一致性
    private SysIndexState _state;   //索引异步构建状态

    /**
     * Only for serialization
     */
    SysIndexModel(EntityModel owner) {
        super(owner);
    }

    public SysIndexModel(EntityModel owner, String name, boolean unique,
                         FieldWithOrder[] fields, short[] storingFields) {
        super(owner, name, unique, fields, storingFields);
        _global = false; //TODO:暂不支持全局索引
        _state  = owner.persistentState() == PersistentState.Detached ? SysIndexState.Ready : SysIndexState.Building;
    }

    public boolean isGlobal() { return _global; }

    //region ====Serialization====
    @Override
    public void writeTo(IOutputStream bs) {
        super.writeTo(bs);

        bs.writeBoolField(_global, 1);
        bs.writeByteField(_state.value, 2);
        bs.finishWriteFields();
    }

    @Override
    public void readFrom(BinDeserializer bs) {
        super.readFrom(bs);

        int propIndex;
        do {
            propIndex = bs.readVariant();
            switch (propIndex) {
                case 1:
                    _global = bs.readBool();
                    break;
                case 2:
                    _state = SysIndexState.fromValue(bs.readByte());
                    break;
                case 0:
                    break;
                default:
                    throw new RuntimeException("Unknown field id: " + propIndex);
            }
        } while (propIndex != 0);
    }
    //endregion
}
