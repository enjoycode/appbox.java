package appbox.model.entity;

import appbox.data.PersistentState;
import appbox.model.DataStoreModel;
import appbox.model.EntityModel;
import appbox.model.ModelLayer;
import appbox.serialization.IInputStream;
import appbox.serialization.IJsonWriter;
import appbox.serialization.IOutputStream;
import appbox.utils.IdUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class SqlStoreOptions implements IEntityStoreOption {
    private static final short MAX_INDEX_ID = 32; //2的5次方, 2bit Layer，1bit惟一标志

    protected final EntityModel owner;  //不要序列化

    private byte _devIndexIdSeq;
    private byte _usrIndexIdSeq;

    private FieldWithOrder[]    _primaryKeys;
    private boolean             _primaryKeysHasChanged;
    private List<SqlIndexModel> _indexes;

    private long           _storeModelId; //映射的DataStoreModel的标识
    private DataStoreModel _dataStoreModel_cached; //仅用于缓存

    /** only for serialization */
    public SqlStoreOptions(EntityModel owner) {
        this.owner = owner;
    }

    public SqlStoreOptions(EntityModel owner, long storeModelId) {
        this.owner    = owner;
        _storeModelId = storeModelId;
    }

    //region ====Properties====
    public long storeModelId() {
        return _storeModelId;
    }

    public DataStoreModel storeModel() { return _dataStoreModel_cached; }

    public boolean hasPrimaryKeys() { return _primaryKeys != null && _primaryKeys.length > 0; }

    public FieldWithOrder[] primaryKeys() { return _primaryKeys; }

    public boolean isPrimaryKeysChanged() { return _primaryKeysHasChanged; }

    @Override
    public boolean hasIndexes() {
        return _indexes != null && _indexes.size() > 0;
    }

    @Override
    public List<? extends IndexModelBase> getIndexes() {
        return _indexes;
    }
    //endregion

    //region ====Design Methods====

    @Override
    public void acceptChanges() {
        _primaryKeysHasChanged = false;
        if (hasIndexes()) {
            for (int i = _indexes.size() - 1; i >= 0; i--) {
                if (_indexes.get(i).persistentState() == PersistentState.Deleted)
                    _indexes.remove(i);
                else
                    _indexes.get(i).acceptChanges();
            }
        }
    }

    public boolean isPrimaryKey(short memberId) {
        if (hasPrimaryKeys()) {
            for (var pk : _primaryKeys) {
                if (pk.memberId == memberId)
                    return true;
            }
        }
        return false;
    }

    public void setPrimaryKeys(FieldWithOrder[] fields) {
        owner.checkDesignMode();
        _primaryKeys = fields;

        //同时设置成员的AllowNull = false
        for (var pk : fields) {
            owner.getMember(pk.memberId).setAllowNull(false);
        }

        _primaryKeysHasChanged = true;
        owner.onPropertyChanged();
    }

    public void addIndex(SqlIndexModel index) {
        owner.checkDesignMode();

        //TODO:同AddMember获取当前Layer
        var layer = ModelLayer.DEV;
        var seq   = layer == ModelLayer.DEV ? ++_devIndexIdSeq : ++_usrIndexIdSeq;
        if (seq >= MAX_INDEX_ID) //TODO:找空的
            throw new RuntimeException("Index id out of range");
        byte indexId = (byte) (seq << 2 | layer.value);
        if (index.unique())
            indexId |= 1 << IdUtil.INDEXID_UNIQUE_OFFSET;
        index.initIndexId(indexId);
        if (_indexes == null)
            _indexes = new ArrayList<>();
        _indexes.add(index);

        owner.onPropertyChanged();
    }

    /** 仅用于设计时 */
    public void setDataStoreModel(DataStoreModel dataStoreModel) {
        _dataStoreModel_cached = dataStoreModel;
    }

    //endregion

    //region ====Serialization====
    @Override
    public void writeTo(IOutputStream bs) {
        bs.writeLongField(_storeModelId, 1);

        //写入主键
        if (hasPrimaryKeys()) {
            bs.writeArray(_primaryKeys, 2, false);
        }
        bs.writeBoolField(_primaryKeysHasChanged, 3);

        //写入索引
        if (hasIndexes()) {
            bs.writeList(_indexes, 4, false);
        }

        bs.writeByteField(_devIndexIdSeq, 6);
        bs.writeByteField(_usrIndexIdSeq, 7);

        bs.finishWriteFields();
    }

    @Override
    public void readFrom(IInputStream bs) {
        int propIndex;
        do {
            propIndex = bs.readVariant();
            switch (propIndex) {
                case 1:
                    _storeModelId = bs.readLong();
                    break;
                case 2:
                    _primaryKeys = bs.readArray(FieldWithOrder[]::new, FieldWithOrder::new, false);
                    break;
                case 3:
                    _primaryKeysHasChanged = bs.readBool();
                    break;
                case 4:
                    _indexes = bs.readList(() -> new SqlIndexModel(owner), false);
                    break;
                case 6:
                    _devIndexIdSeq = bs.readByte();
                    break;
                case 7:
                    _usrIndexIdSeq = bs.readByte();
                    break;
                case 0:
                    break;
                default:
                    throw new RuntimeException("Unknown field id:" + propIndex);
            }
        } while (propIndex != 0);
    }

    @Override
    public void writeToJson(IJsonWriter writer) {
        writer.startObject();

        writer.writeKeyValue("StoreName", _dataStoreModel_cached.name());
        writer.writeKeyValue("StoreKind", _dataStoreModel_cached.kind().value);

        writer.writeKey("PrimaryKeys");
        if (hasPrimaryKeys()) {
            //注意: 需要将主键成员中是EntityRef's的外键成员转换为EntityRef成员Id,以方便前端显示
            //eg: OrderId => Order，否则前端会找不到OrderId成员无法显示相应的名称
            var pks  = new ArrayList<>(Arrays.asList(_primaryKeys));
            var refs = new ArrayList<FieldWithOrder>();
            for (int i = pks.size() - 1; i >= 0; i--) {
                var memberModel    = (DataFieldModel) owner.getMember(pks.get(i).memberId);
                var refMemberModel = memberModel.getEntityRefModelByForeignKey();
                if (refMemberModel != null && refs.stream().noneMatch(t -> t.memberId == refMemberModel.memberId())) {
                    var item = new FieldWithOrder(refMemberModel.memberId(), pks.get(i).orderByDesc);
                    refs.add(0, item);
                    pks.remove(i);
                }
            }
            pks.addAll(refs);
            writer.writeList(pks);
        } else {
            writer.writeEmptyArray();
        }

        writer.writeKey("Indexes");
        writer.startArray();
        //TODO:
        writer.endArray();

        writer.endObject();
    }

    //endregion

}
