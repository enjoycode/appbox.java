package appbox.channel.messages;

import appbox.model.ApplicationModel;
import appbox.serialization.BinDeserializer;

public final class KVScanAppsResponse extends KVScanResponse {

    public ApplicationModel[] apps;

    @Override
    public void readFrom(BinDeserializer bs) throws Exception {
        reqId     = bs.readInt();
        errorCode = bs.readInt();

        if (errorCode == 0) {
            skipped = bs.readInt();
            length  = bs.readInt();

            apps = new ApplicationModel[length];
            for (int i = 0; i < length; i++) {
                bs.skip(bs.readNativeVariant()); //跳过Row's Key
                bs.readNativeVariant(); //跳过Row's Value size;
                apps[i] = new ApplicationModel();
                var appStoreId = bs.readByte();
                var devIdSeq   = bs.readInt();
                var usrIdSeq   = bs.readInt();
                apps[i].readFrom(bs);
                apps[i].setAppStoreId(appStoreId);
                apps[i].setDevModelIdSeq(devIdSeq);
            }
        }
    }
}
