package appbox.store;

import appbox.core.serialization.BinDeserializer;
import appbox.core.serialization.BinSerializer;

/**
 * 系统存储的事务标识号
 */
public final class KVTxnId {
    public long  startTS;
    public short peerId;
    public short shardId;
    public byte  isoLevel;

    public void writeTo(BinSerializer bs) throws Exception {
        bs.writeLong(startTS);
        bs.writeShort(peerId);
        bs.writeShort(shardId);
        bs.writeByte(isoLevel);
    }

    public void readFrom(BinDeserializer bs) throws Exception {
        startTS  = bs.readLong();
        peerId   = bs.readShort();
        shardId  = bs.readShort();
        isoLevel = bs.readByte();
    }
}
