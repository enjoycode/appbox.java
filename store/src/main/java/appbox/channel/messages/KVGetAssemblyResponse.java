package appbox.channel.messages;

import appbox.serialization.BinDeserializer;

public final class KVGetAssemblyResponse extends KVGetResponse {

    private byte[] asmData;

    public byte[] getAssemblyData() { return asmData; }

    @Override
    public void readFrom(BinDeserializer bs) {
        reqId     = bs.readInt();
        errorCode = bs.readInt();
        checkStoreError();

        var size = bs.readNativeVariant(); //跳过长度
        if (size > 0) {
            asmData = bs.readRemaining();
        }
    }
}
