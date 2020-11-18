package appbox.channel.messages;

import appbox.model.ModelBase;
import appbox.serialization.BinDeserializer;

public final class KVScanModelsResponse extends KVScanResponse {
    public ModelBase[] models;

    @Override
    public void readFrom(BinDeserializer bs) {
        reqId     = bs.readInt();
        errorCode = bs.readInt();

        if (errorCode == 0) {
            skipped = bs.readInt();
            length  = bs.readInt();

            models = new ModelBase[length];
            for (int i = 0; i < length; i++) {
                bs.skip(bs.readNativeVariant()); //跳过Row's Key
                bs.readNativeVariant(); //跳过Row's Value size;
                models[i] = ModelBase.makeModelByType(bs.readByte());
                models[i].readFrom(bs);
            }
        }
    }
}
