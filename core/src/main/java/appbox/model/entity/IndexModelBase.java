package appbox.model.entity;

import appbox.data.PersistentState;
import appbox.model.EntityModel;
import appbox.serialization.BinDeserializer;
import appbox.serialization.IBinSerializable;
import appbox.serialization.IOutputStream;

/**
 * 系统存储及Sql存储的索引模型基类
 */
public abstract class IndexModelBase implements IBinSerializable {
    public final EntityModel owner;          //不需要序列化

    private byte             _indexId;
    private String           _name;
    private boolean          _unique;
    private FieldWithOrder[] _fields;
    private short[]          _storingFields; //索引覆盖字段集合
    private PersistentState  _persistentState;

    /**
     * Only for serialization
     */
    IndexModelBase(EntityModel owner) {
        this.owner = owner;
    }

    public IndexModelBase(EntityModel owner, String name, boolean unique,
                          FieldWithOrder[] fields, short[] storingFields) {
        this.owner       = owner;
        _name            = name;
        _unique          = unique;
        _fields          = fields;
        _storingFields   = storingFields;
        _persistentState = PersistentState.Detached;
    }

    //region ====Properties====
    public PersistentState persistentState() {
        return _persistentState;
    }

    public FieldWithOrder[] fields() { return _fields; }

    public boolean hasStoringFields() { return _storingFields != null && _storingFields.length > 0; }

    public short[] storingFields() { return _storingFields; }

    public byte indexId() { return _indexId; }

    public boolean unique() { return _unique; }
    //endregion

    //region ====Design Methods====
    public void canAddTo(EntityModel owner) {
        if (this.owner != owner) {
            throw new RuntimeException();
        }
    }

    public void initIndexId(byte id) {
        if (_indexId == 0) {
            _indexId = id;
        } else {
            throw new RuntimeException("Index's id has init.");
        }
    }

    public void acceptChanges() {
        _persistentState = PersistentState.Unchnaged;
    }

    public void markDeleted() {
        _persistentState = PersistentState.Deleted;
        owner.onPropertyChanged();
        owner.changeSchemaVersion();
    }
    //endregion

    //region ====Serialization====
    @Override
    public void writeTo(IOutputStream bs) {
        bs.writeByteField(_indexId, 2);
        bs.writeStringField(_name, 3);
        bs.writeBoolField(_unique, 4);

        //fields
        bs.writeVariant(6);
        bs.writeVariant(_fields.length);
        for (FieldWithOrder field : _fields) {
            field.writeTo(bs);
        }

        //storing fields
        if (_storingFields != null && _storingFields.length > 0) {
            bs.writeVariant(7);
            bs.writeVariant(_storingFields.length);
            for (short storingField : _storingFields) {
                bs.writeShort(storingField);
            }
        }

        if (owner.designMode()) {
            bs.writeByteField(_persistentState.value, 9);
        }

        bs.finishWriteFields();
    }

    @Override
    public void readFrom(BinDeserializer bs) {
        int propIndex;
        do {
            propIndex = bs.readVariant();
            switch (propIndex) {
                case 2:
                    _indexId = bs.readByte();
                    break;
                case 3:
                    _name = bs.readString();
                    break;
                case 4:
                    _unique = bs.readBool();
                    break;
                case 6: {
                    var count = bs.readVariant();
                    _fields = new FieldWithOrder[count];
                    for (int i = 0; i < count; i++) {
                        _fields[i] = new FieldWithOrder();
                        _fields[i].readFrom(bs);
                    }
                    break;
                }
                case 7: {
                    var count = bs.readVariant();
                    _storingFields = new short[count];
                    for (int i = 0; i < count; i++) {
                        _storingFields[i] = bs.readShort();
                    }
                    break;
                }
                case 9:
                    _persistentState = PersistentState.fromValue(bs.readByte());
                    break;
                case 0:
                    break;
                default:
                    throw new RuntimeException("Unknown field id:" + propIndex);
            }
        } while (propIndex != 0);
    }
    //endregion
}
