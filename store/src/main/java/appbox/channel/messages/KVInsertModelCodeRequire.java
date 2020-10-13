package appbox.channel.messages;

import appbox.serialization.BinSerializer;
import appbox.store.KeyUtil;
import appbox.utils.IdUtil;

public final class KVInsertModelCodeRequire extends KVInsertRequire {
    public long   modelId;
    public byte[] codeData;

    public KVInsertModelCodeRequire() {
        raftGroupId      = KeyUtil.META_RAFTGROUP_ID;
        schemaVersion    = 0;
        dataCF           = -1;
        overrideIfExists = true;
    }

    @Override
    public void writeTo(BinSerializer bs) throws Exception {
        super.writeTo(bs);

        //key
        KeyUtil.writeModelCodeKey(bs, modelId);
        //refs
        bs.writeVariant(0);
        //data
        bs.writeByte(IdUtil.getModelTypeFromModelId(modelId).value); //注意写入模型类型信息
        bs.write(codeData);
    }
}