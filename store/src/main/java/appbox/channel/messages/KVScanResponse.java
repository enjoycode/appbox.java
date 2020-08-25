package appbox.channel.messages;

import appbox.channel.MessageType;
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
            if (dataType == 1) {
                var arrayOfModels = new ModelBase[length];
                for (int i = 0; i < length; i++) {
                    bs.skip(bs.readNativeVariant()); //跳过Row's Key
                    bs.readNativeVariant(); //跳过Row's Value size;
                    arrayOfModels[i] = ModelBase.makeModelByType(bs.readByte());
                    arrayOfModels[i].readFrom(bs);
                }
                result = arrayOfModels;
            } else {
                throw new RuntimeException("暂未实现");
            }
        }
    }
}
