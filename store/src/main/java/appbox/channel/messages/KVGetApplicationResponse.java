package appbox.channel.messages;

import appbox.model.ApplicationModel;
import appbox.serialization.BinDeserializer;

public final class KVGetApplicationResponse extends KVGetResponse {

    private ApplicationModel _applicationModel;

    public ApplicationModel getApplicationModel() { return _applicationModel; }

    @Override
    public void readFrom(BinDeserializer bs) {
        reqId     = bs.readInt();
        errorCode = bs.readInt();
        checkStoreError();

        var size = bs.readNativeVariant(); //跳过长度
        if (size > 0) {
            var appStoreId = bs.readByte();
            var devIdSeq   = bs.readInt();
            var usrIdSeq   = bs.readInt();

            _applicationModel = new ApplicationModel();
            _applicationModel.readFrom(bs);
            _applicationModel.setAppStoreId(appStoreId);
            _applicationModel.setDevModelIdSeq(devIdSeq);
        }
    }
}
