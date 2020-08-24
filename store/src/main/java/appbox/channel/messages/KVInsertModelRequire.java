package appbox.channel.messages;

import appbox.model.ModelBase;
import appbox.serialization.BinSerializer;
import appbox.store.KeyUtil;

public final class KVInsertModelRequire extends KVInsertRequire {

    public ModelBase model;

    public KVInsertModelRequire() {
        raftGroupId = KeyUtil.META_RAFTGROUP_ID;
        schemaVersion = 0;
        dataCF = -1;
    }

    @Override
    public void writeTo(BinSerializer bs) throws Exception {
        super.writeTo(bs);

        //key
        KeyUtil.writeModelKey(bs, model.id());
        //refs
        bs.writeVariant(0);
        //data
        bs.writeByte(model.modelType().value); //注意写入模型类型信息
        model.writeTo(bs);
    }
}
