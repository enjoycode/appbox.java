package appbox.model.entity;

import appbox.data.PersistentState;
import appbox.model.EntityModel;
import appbox.serialization.BinDeserializer;
import appbox.serialization.BinSerializer;
import com.alibaba.fastjson.JSONWriter;

public final class DataFieldModel extends EntityMemberModel {
    //region ====DataFieldType====
    public enum DataFieldType {
        EntityId(0),
        String(1),
        DateTime(2),
        Short(4),
        Int(6),
        Long(8),
        Decimal(9),
        Bool(10),
        Guid(11),
        Byte(12),
        Binary(13),
        Enum(14),
        Float(15),
        Double(16);

        public final byte value;

        DataFieldType(int value) {
            this.value = (byte) value;
        }

        public static DataFieldType fromValue(byte v) {
            for (DataFieldType item : DataFieldType.values()) {
                if (item.value == v) {
                    return item;
                }
            }
            throw new RuntimeException("Unknown value: " + v);
        }
    }
    //endregion

    private boolean       _isForeignKey; //是否引用外键
    private boolean       _isDataTypeChanged; //字段类型、AllowNull及DefaultValue变更均视为DataTypeChanged
    //----以下change must call onPropertyChanged----
    private long          _enumModelId; //如果DataType = Enum,则必须设置相应的EnumModel.ModelId
    //----以下change must call onDataTypeChanged----
    private DataFieldType _dataType;
    private int           _length; //仅用于Sql存储设置字符串最大长度(0=无限制)或Decimal整数部分长度
    private int           _decimals; //仅用于Sql存储设置Decimal小数部分长度
    //TODO:默认值

    /** Only for serialization */
    public DataFieldModel(EntityModel owner) {
        super(owner);
    }

    public DataFieldModel(EntityModel owner, String name, DataFieldType dataType,
                          boolean allowNull) {
        this(owner, name, dataType, allowNull, false);
    }

    public DataFieldModel(EntityModel owner, String name, DataFieldType dataType,
                          boolean allowNull, boolean isFK) {
        super(owner, name, allowNull);

        _dataType     = dataType;
        _isForeignKey = isFK;
    }

    //region ====Properties====
    @Override
    public EntityMemberType type() {
        return EntityMemberType.DataField;
    }

    /** 数据类型 */
    public DataFieldType dataType() {
        return _dataType;
    }

    /**
     * 保留用于根据规则生成Sql列的名称, eg:相同前缀、命名规则等
     */
    public String sqlColName() {
        return name();
    }

    public String sqlColOriginalName() {
        return originalName();
    }

    /**
     * 是否系统存储的分区键
     */
    public boolean isPartitionKey() {
        return owner.sysStoreOptions() != null && owner.sysStoreOptions().isPartitionKey(memberId());
    }

    /** 是否引用外键 */
    public boolean isForeignKey() { return _isForeignKey; }
    //endregion

    //region ====Design Methods====
    protected void onDataTypeChanged() {
        if (persistentState() == PersistentState.Unchnaged) {
            _isDataTypeChanged = true;
            onPropertyChanged();
        }
    }

    @Override
    public void setAllowNull(boolean value) {
        _allowNull = value;
        onDataTypeChanged();
    }

    public void setLength(int value) {
        _length = value;
        onDataTypeChanged();
    }
    //endregion

    //region ====Serialization====
    @Override
    public void writeTo(BinSerializer bs) {
        super.writeTo(bs);

        bs.writeByte(_dataType.value, 1);
        bs.writeBool(_isForeignKey, 2);
        if (_dataType == DataFieldType.Enum) {
            bs.writeLong(_enumModelId, 3);
        } else if (_dataType == DataFieldType.String) {
            bs.writeVariant(_length, 5);
        } else if (_dataType == DataFieldType.Decimal) {
            bs.writeVariant(_length, 5);
            bs.writeVariant(_decimals, 6);
        }

        //TODO:写入默认值

        bs.writeBool(_isDataTypeChanged, 7);

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
                    _dataType = DataFieldType.fromValue(bs.readByte());
                    break;
                case 2:
                    _isForeignKey = bs.readBool();
                    break;
                case 3:
                    _enumModelId = bs.readLong();
                    break;
                case 5:
                    _length = bs.readVariant();
                    break;
                case 6:
                    _decimals = bs.readVariant();
                    break;
                case 7:
                    _isDataTypeChanged = bs.readBool();
                    break;
                case 0:
                    break;
                default:
                    throw new RuntimeException("Unknown field id: " + propIndex);
            }
        } while (propIndex != 0);
    }

    @Override
    protected void writeJsonMembers(JSONWriter writer) {
        writer.writeKey("DataType");
        writer.writeValue(_dataType.value);

        if (_dataType == DataFieldType.Enum) {
            writer.writeKey("EnumModelId");
            writer.writeValue(Long.toUnsignedString(_enumModelId));
        } else if (_dataType == DataFieldType.String) {
            writer.writeKey("Length");
            writer.writeValue(_length);
        } else if (_dataType == DataFieldType.Decimal) {
            writer.writeKey("Length");
            writer.writeValue(_length);

            writer.writeKey("Decimals");
            writer.writeValue(_decimals);
        }
    }
    //endregion

}
