package appbox.model.entity;

import appbox.data.PersistentState;
import appbox.model.EntityModel;

public final class DataFieldModel extends EntityMemberModel {
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

        private final byte _value;

        DataFieldType(int value) {
            _value = (byte) value;
        }
    }

    private boolean       _isForeignKey; //是否引用外键
    private boolean       _isDataTypeChanged; //字段类型、AllowNull及DefaultValue变更均视为DataTypeChanged
    //----以下change must call onPropertyChanged----
    private long          _enumModelId; //如果DataType = Enum,则必须设置相应的EnumModel.ModelId
    //----以下change must call onDataTypeChanged----
    private DataFieldType _dataType;
    private int           _length; //仅用于Sql存储设置字符串最大长度(0=无限制)或Decimal整数部分长度
    private int           _decimals; //仅用于Sql存储设置Decimal小数部分长度
    //TODO:默认值

    public DataFieldModel() {
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
    //endregion

    //region ====Design Methods====
    protected void onDataTypeChanged() {
        if (persistentState() == PersistentState.Unchnaged) {
            _isDataTypeChanged = true;
            onPropertyChanged();
        }
    }
    //endregion

}
