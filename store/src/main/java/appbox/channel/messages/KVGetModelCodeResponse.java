package appbox.channel.messages;

import appbox.model.ModelType;
import appbox.serialization.IInputStream;
import appbox.store.utils.ModelCodeUtil;

public final class KVGetModelCodeResponse extends KVGetResponse {
    public Object sourceCode;

    @Override
    public void readFrom(IInputStream bs) {
        reqId     = bs.readInt();
        errorCode = bs.readInt();
        checkStoreError();

        var size = bs.readNativeVariant(); //跳过长度
        if (size > 0) {
            var modelType = ModelType.fromValue(bs.readByte());
            var codeData  = bs.readRemaining();
            if (modelType == ModelType.Service) {
                sourceCode = ModelCodeUtil.decodeServiceCode(codeData); //TODO:直接从流中读取
            } else if (modelType == ModelType.View) {
                sourceCode = ModelCodeUtil.decodeViewCode(codeData);
            } else {
                throw new RuntimeException("暂未实现");
            }
        }
    }
}
