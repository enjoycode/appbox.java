package appbox.server.channel.messages;

import appbox.core.cache.ObjectPool;
import appbox.core.serialization.BinDeserializer;
import appbox.core.serialization.BinSerializer;
import appbox.server.channel.MessageType;
import appbox.store.KVTxnId;

import javax.naming.OperationNotSupportedException;

public final class KVInsertRequire implements IMessage {
    //region ====ObjectPool====
    private static final ObjectPool<KVInsertRequire> pool = new ObjectPool<>(KVInsertRequire::new, 32);

    public static KVInsertRequire rentFromPool() {
        return pool.rent();
    }

    public static void backToPool(KVInsertRequire obj) {
        pool.back(obj);
    }
    //endregion

    public final KVTxnId txnId = new KVTxnId();
    public       long    raftGroupId;
    public       int     schemaVersion;
    public       byte    dataCF;
    public       boolean overrideIfExists;
    public       byte[]  key;
    public       byte[]  refs;
    public       byte[]  data;

    @Override
    public byte MessageType() {
        return MessageType.KVInsertRequire;
    }

    //region ====Serialization====
    @Override
    public void writeTo(BinSerializer bs) throws Exception {
        txnId.writeTo(bs);

        bs.writeLong(raftGroupId);
        bs.writeInt(schemaVersion);
        bs.writeByte(dataCF);
        bs.writeBool(overrideIfExists);

        bs.writeByteArray(key);
        bs.writeByteArray(refs);
        bs.writeByteArray(data);
    }

    @Override
    public void readFrom(BinDeserializer bs) throws Exception {
        throw new OperationNotSupportedException();
    }
    //endregion
}
