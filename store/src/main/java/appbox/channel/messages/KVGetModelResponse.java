package appbox.channel.messages;

import appbox.model.ModelBase;
import appbox.serialization.BinDeserializer;

public final class KVGetModelResponse extends KVGetResponse {

    public ModelBase model;

    @Override
    public void readFrom(BinDeserializer bs) throws Exception {
        reqId     = bs.readInt();
        errorCode = bs.readInt();

        if (errorCode == 0) {
            bs.readNativeVariant(); //跳过长度
            model = ModelBase.makeModelByType(bs.readByte());
            model.readFrom(bs);
        }
    }
}
