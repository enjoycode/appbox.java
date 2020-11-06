package appbox.channel.messages;

import appbox.channel.MessageType;
import appbox.model.ApplicationModel;
import appbox.model.ModelBase;
import appbox.serialization.BinDeserializer;
import appbox.serialization.BinSerializer;

public final class KVScanResponse extends StoreResponse {

    public Object result;
    public int    skipped;
    public int    length;

    @Override
    public byte MessageType() {
        return MessageType.KVScanResponse;
    }

    @Override
    public void writeTo(BinSerializer bs) throws Exception {

    }

    @Override
    public void readFrom(BinDeserializer bs) throws Exception {
        reqId     = bs.readInt();
        errorCode = bs.readInt();

        if (errorCode == 0) {
            byte dataType = bs.readByte(); //读取数据类型
            skipped = bs.readInt();
            length  = bs.readInt();
            if (dataType == KVReadDataType.ApplicationModel.value) {
                result = readApps(bs, length);
            } else if (dataType == KVReadDataType.Model.value) {
                result = readModels(bs, length);
            } else {
                throw new RuntimeException("暂未实现");
            }
        }
    }

    private static ApplicationModel[] readApps(BinDeserializer bs, int length) throws Exception {
        var arrayOfApps = new ApplicationModel[length];
        for (int i = 0; i < length; i++) {
            bs.skip(bs.readNativeVariant()); //跳过Row's Key
            bs.readNativeVariant(); //跳过Row's Value size;
            arrayOfApps[i] = new ApplicationModel();
            var appStoreId = bs.readByte();
            var devIdSeq   = bs.readInt();
            var usrIdSeq   = bs.readInt();
            arrayOfApps[i].readFrom(bs);
            arrayOfApps[i].setAppStoreId(appStoreId);
            arrayOfApps[i].setDevModelIdSeq(devIdSeq);
        }
        return arrayOfApps;
    }

    private static ModelBase[] readModels(BinDeserializer bs, int length) throws Exception {
        var arrayOfModels = new ModelBase[length];
        for (int i = 0; i < length; i++) {
            bs.skip(bs.readNativeVariant()); //跳过Row's Key
            bs.readNativeVariant(); //跳过Row's Value size;
            arrayOfModels[i] = ModelBase.makeModelByType(bs.readByte());
            arrayOfModels[i].readFrom(bs);
        }
        return arrayOfModels;
    }
}
