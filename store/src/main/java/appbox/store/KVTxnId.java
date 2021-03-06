package appbox.store;

import appbox.serialization.IInputStream;
import appbox.serialization.IOutputStream;

/**
 * 系统存储的事务标识号
 */
public final class KVTxnId {
    public long  startTS;
    public short peerId;
    public short shardId;
    public byte  isoLevel;

    public void copyFrom(KVTxnId from) {
        startTS  = from.startTS;
        peerId   = from.peerId;
        shardId  = from.shardId;
        isoLevel = from.isoLevel;
    }

    public void writeTo(IOutputStream bs) {
        bs.writeLong(startTS);
        bs.writeShort(peerId);
        bs.writeShort(shardId);
        bs.writeByte(isoLevel);
    }

    public void readFrom(IInputStream bs) {
        startTS  = bs.readLong();
        peerId   = bs.readShort();
        shardId  = bs.readShort();
        isoLevel = bs.readByte();
    }
}
