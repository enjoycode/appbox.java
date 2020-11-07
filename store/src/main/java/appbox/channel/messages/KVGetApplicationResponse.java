package appbox.channel.messages;

import appbox.model.ApplicationModel;
import appbox.serialization.BinDeserializer;

public final class KVGetApplicationResponse extends KVGetResponse {

    public final ApplicationModel applicationModel = new ApplicationModel();

    @Override
    public void readFrom(BinDeserializer bs) throws Exception {
        reqId     = bs.readInt();
        errorCode = bs.readInt();

        if (errorCode == 0) {
            bs.readNativeVariant(); //跳过长度
            var appStoreId = bs.readByte();
            var devIdSeq   = bs.readInt();
            var usrIdSeq   = bs.readInt();
            applicationModel.readFrom(bs);
            applicationModel.setAppStoreId(appStoreId);
            applicationModel.setDevModelIdSeq(devIdSeq);
        }
    }
}
