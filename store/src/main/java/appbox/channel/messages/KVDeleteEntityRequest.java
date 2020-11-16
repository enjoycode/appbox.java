package appbox.channel.messages;

import appbox.data.EntityId;
import appbox.model.EntityModel;
import appbox.serialization.BinSerializer;
import appbox.store.KVTxnId;
import appbox.store.KeyUtil;

public final class KVDeleteEntityRequest extends KVDeleteRequest {

    private final EntityId _id;

    public KVDeleteEntityRequest(KVTxnId txnId, EntityId id, EntityModel model) {
        super(txnId);

        _id = id;

        raftGroupId   = id.raftGroupId();
        schemaVersion = model.sysStoreOptions().schemaVersion();
        returnExists  = model.sqlStoreOptions().hasIndexes() /*|| refs != null*/; //TODO:****
        dataCF        = -1;
    }

    @Override
    public void writeTo(BinSerializer bs) throws Exception {
        super.writeTo(bs);

        //refs
        bs.writeVariant(-1); //TODO:
        //key 不带长度信息
        KeyUtil.writeEntityKey(bs, _id, false);
    }
}
