package appbox.channel.messages;

import appbox.serialization.BinDeserializer;

public final class KVGetAssemblyResponse extends KVGetResponse {

    private byte[] asmData;

    public byte[] getAssemblyData() { return asmData; }

    @Override
    public void readFrom(BinDeserializer bs) {
        reqId     = bs.readInt();
        errorCode = bs.readInt();

        if (errorCode == 0 && bs.hasRemaining()) {
            asmData = bs.readRemaining();
        }
    }
}
