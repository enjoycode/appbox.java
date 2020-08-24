package appbox.channel.messages;

import appbox.channel.MessageType;
import appbox.model.ModelBase;
import appbox.serialization.BinDeserializer;
import appbox.serialization.BinSerializer;

public final class KVGetResponse extends StoreResponse {

    public Object result;

    @Override
    public byte MessageType() {
        return MessageType.KVGetResponse;
    }

    @Override
    public void writeTo(BinSerializer bs) throws Exception {

    }

    @Override
    public void readFrom(BinDeserializer bs) throws Exception {
        reqId     = bs.readInt();
        errorCode = bs.readInt();

        //直接反序列化数据，以避免内存复制
        if (errorCode == 0) {
            //读取数据类型
            byte dataType = bs.readByte();
            if (dataType == 1) {
                bs.readNativeVariant(); //跳过长度
                //注意直接反序列化为模型，以减少内存复制
                var model = ModelBase.makeModelByType(bs.readByte());
                model.readFrom(bs);
                result = model;
            } else {
                throw new RuntimeException("暂未实现");
            }
        }
    }
}
