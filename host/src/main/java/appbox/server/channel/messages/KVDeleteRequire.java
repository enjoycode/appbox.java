package appbox.server.channel.messages;

import appbox.cache.ObjectPool;
import appbox.serialization.BinDeserializer;
import appbox.serialization.BinSerializer;
import appbox.server.channel.MessageType;
import appbox.store.KVTxnId;

public final class KVDeleteRequire implements IMessage {
    //region ====ObjectPool====
    private static final ObjectPool<KVDeleteRequire> pool = new ObjectPool<>(KVDeleteRequire::new, 32);

    public static KVDeleteRequire rentFromPool() {
        return pool.rent();
    }

    public static void backToPool(KVDeleteRequire obj) {
        pool.back(obj);
    }
    //endregion

    public final KVTxnId txnId = new KVTxnId();
    public       long    raftGroupId;
    public       int     schemaVersion;
    public       byte    dataCF;
    public       boolean returnExists;
    public       byte[]  key;
    public       byte[]  refs;

    @Override
    public byte MessageType() {
        return MessageType.KVDeleteRequire;
    }

    //region ====Serialization====
    @Override
    public void writeTo(BinSerializer bs) throws Exception {
        txnId.writeTo(bs);

        bs.writeLong(raftGroupId);
        bs.writeInt(schemaVersion);
        bs.writeByte(dataCF);
        bs.writeBool(returnExists);

        bs.writeByteArray(key);
        bs.writeByteArray(refs);
    }

    @Override
    public void readFrom(BinDeserializer bs) throws Exception {

    }
    //endregion
}
