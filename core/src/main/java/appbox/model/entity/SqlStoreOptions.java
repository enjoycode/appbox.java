package appbox.model.entity;

import appbox.model.DataStoreModel;
import appbox.model.EntityModel;
import appbox.serialization.BinDeserializer;
import appbox.serialization.BinSerializer;
import appbox.serialization.IJsonWriter;

import java.util.List;

public final class SqlStoreOptions implements IEntityStoreOption {
    private static final short MAX_INDEX_ID = 32; //2的5次方, 2bit Layer，1bit惟一标志

    protected final EntityModel owner;              //不要序列化

    private byte _devIndexIdSeq;
    private byte _usrIndexIdSeq;

    private FieldWithOrder[] _primaryKeys;
    private boolean          _primaryKeysHasChanged;

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

    public long storeModelId() {
        return _storeModelId;
    }

    public DataStoreModel storeModel() { return _dataStoreModel_cached; }

    public boolean hasPrimaryKeys() { return _primaryKeys != null && _primaryKeys.length > 0; }

    public FieldWithOrder[] primaryKeys() { return _primaryKeys; }

    @Override
    public boolean hasIndexes() {
        return false;
    }

    @Override
    public List<? extends IndexModelBase> getIndexes() {
        return null;
    }

    @Override
    public void acceptChanges() {

    }

    //region ====Design Methods====

    public void setPrimaryKeys(FieldWithOrder[] fields) {
        owner.checkDesignMode();
        _primaryKeys           = fields;
        _primaryKeysHasChanged = true;
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

        writer.writeKey("StoreName");
        writer.writeValue(_dataStoreModel_cached.name());

        writer.writeKey("StoreKind");
        writer.writeValue(_dataStoreModel_cached.kind().value);

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
