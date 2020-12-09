package appbox.model.entity;

import appbox.data.PersistentState;
import appbox.model.DataStoreModel;
import appbox.model.EntityModel;
import appbox.model.ModelLayer;
import appbox.serialization.BinDeserializer;
import appbox.serialization.BinSerializer;
import appbox.serialization.IJsonWriter;
import appbox.utils.IdUtil;

import java.util.ArrayList;
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

    public void setPrimaryKeys(FieldWithOrder[] fields) {
        owner.checkDesignMode();
        _primaryKeys           = fields;
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
    public void writeTo(BinSerializer bs) {

    }

    @Override
    public void readFrom(BinDeserializer bs) {

    }

    @Override
    public void writeToJson(IJsonWriter writer) {
        writer.startObject();

        writer.writeKeyValue("StoreName", _dataStoreModel_cached.name());
        writer.writeKeyValue("StoreKind", _dataStoreModel_cached.kind().value);

        writer.writeKey("PrimaryKeys");
        writer.startArray();
        //TODO:
        writer.endArray();

        writer.writeKey("Indexes");
        writer.startArray();
        //TODO:
        writer.endArray();

        writer.endObject();
    }

    //endregion

}
