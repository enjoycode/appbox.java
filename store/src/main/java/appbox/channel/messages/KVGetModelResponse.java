package appbox.channel.messages;

import appbox.model.ModelBase;
import appbox.serialization.BinDeserializer;

public final class KVGetModelResponse extends KVGetResponse {

    private ModelBase _model;

    public ModelBase getModel() { return _model; }

    @Override
    public void readFrom(BinDeserializer bs) {
        reqId     = bs.readInt();
        errorCode = bs.readInt();
        checkStoreError();

        var size = bs.readNativeVariant(); //跳过长度
        if (size > 0) {
            _model = ModelBase.makeModelByType(bs.readByte());
            _model.readFrom(bs);
        }
    }
}
