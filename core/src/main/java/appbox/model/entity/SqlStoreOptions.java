package appbox.model.entity;

import appbox.model.EntityModel;
import appbox.serialization.BinDeserializer;
import appbox.serialization.BinSerializer;

import java.util.List;

public final class SqlStoreOptions implements IEntityStoreOption {
    private static final short MAX_INDEX_ID = 32; //2的5次方, 2bit Layer，1bit惟一标志

    protected final EntityModel owner;              //不要序列化

    private byte _devIndexIdSeq;
    private byte _usrIndexIdSeq;

    private long _storeModelId; //映射的DataStoreModel的标识

    /** only for serialization */
    public SqlStoreOptions(EntityModel owner) {
        this.owner = owner;
    }

    public SqlStoreOptions(EntityModel owner, long storeModelId) {
        this.owner    = owner;
        _storeModelId = storeModelId;
    }

    public long getStoreModelId() {
        return _storeModelId;
    }

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

    //region ====Serialization====
    @Override
    public void writeTo(BinSerializer bs) throws Exception {

    }

    @Override
    public void readFrom(BinDeserializer bs) throws Exception {

    }
    //endregion

}
