package appbox.channel.messages;

import appbox.model.ModelType;
import appbox.serialization.BinDeserializer;
import appbox.store.utils.ModelCodeUtil;

public final class KVGetModelCodeResponse extends KVGetResponse {
    public Object sourceCode;

    @Override
    public void readFrom(BinDeserializer bs) throws Exception {
        reqId     = bs.readInt();
        errorCode = bs.readInt();

        bs.readNativeVariant(); //跳过长度
        var modelType = ModelType.fromValue(bs.readByte());
        var codeData  = bs.readRemaining();
        if (modelType == ModelType.Service) {
            sourceCode = ModelCodeUtil.decodeServiceCode(codeData);
        } else {
            throw new RuntimeException("暂未实现");
        }
    }
}
