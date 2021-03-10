package appbox.channel.messages;

import appbox.model.DataStoreModel;
import appbox.serialization.IInputStream;

public class KVGetDataStoreResponse extends KVGetResponse {
    public DataStoreModel dataStore;

    @Override
    public void readFrom(IInputStream bs) {
        reqId     = bs.readInt();
        errorCode = bs.readInt();
        checkStoreError();

        var size = bs.readNativeVariant(); //跳过长度
        if (size > 0) {
            dataStore = new DataStoreModel();
            dataStore.readFrom(bs);
        }
    }

}
