package appbox.channel.messages;

import appbox.model.DataStoreModel;
import appbox.serialization.IOutputStream;
import appbox.store.KVTxnId;
import appbox.store.KVUtil;

public final class KVInsertDataStoreRequest extends KVInsertRequire {

    private final DataStoreModel _dataStore;

    public KVInsertDataStoreRequest(KVTxnId txnId, DataStoreModel dataStore) {
        super(txnId);

        _dataStore = dataStore;

        raftGroupId   = KVUtil.META_RAFTGROUP_ID;
        schemaVersion = 0;
        dataCF        = -1;
    }

    @Override
    public void writeTo(IOutputStream bs) {
        super.writeTo(bs);

        //key
        KVUtil.writeDataStoreKey(bs, _dataStore.id(), true);
        //refs
        bs.writeVariant(0);
        //data
        _dataStore.writeTo(bs);
    }

}
