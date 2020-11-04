package appbox.channel.messages;

import appbox.channel.MessageType;
import appbox.model.ApplicationModel;
import appbox.model.ModelBase;
import appbox.model.ModelType;
import appbox.serialization.BinDeserializer;
import appbox.serialization.BinSerializer;
import appbox.store.utils.ModelCodeUtil;

public final class KVGetResponse extends StoreResponse {

    public Object result;

    @Override
    public byte MessageType() {
        return MessageType.KVGetResponse;
    }

    @Override
    public void writeTo(BinSerializer bs) throws Exception {
        throw new RuntimeException("Not supported.");
    }

    @Override
    public void readFrom(BinDeserializer bs) throws Exception {
        reqId     = bs.readInt();
        errorCode = bs.readInt();

        //直接反序列化数据，以减少内存复制
        if (errorCode == 0) {
            byte dataType = bs.readByte(); //读取数据类型
            if (dataType == KVReadDataType.ApplicationModel.value) {
                bs.readNativeVariant(); //跳过长度
                var appStoreId = bs.readByte();
                var devIdSeq   = bs.readInt();
                var usrIdSeq   = bs.readInt();
                var app = new ApplicationModel();
                app.readFrom(bs);
                app.setAppStoreId(appStoreId);
                app.setDevModelIdSeq(devIdSeq);
                result = app;
            } else if (dataType == KVReadDataType.Model.value) {
                bs.readNativeVariant(); //跳过长度
                var model = ModelBase.makeModelByType(bs.readByte());
                model.readFrom(bs);
                result = model;
            } else if (dataType == KVReadDataType.ModelCode.value) {
                bs.readNativeVariant(); //跳过长度
                var modelType = ModelType.fromValue(bs.readByte());
                var codeData  = bs.readRemaining();
                if (modelType == ModelType.Service) {
                    result = ModelCodeUtil.decodeServiceCode(codeData);
                } else {
                    throw new RuntimeException("暂未实现");
                }
            } else {
                throw new RuntimeException("暂未实现");
            }
        }
    }
}
