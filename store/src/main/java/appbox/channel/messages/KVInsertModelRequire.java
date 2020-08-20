package appbox.channel.messages;

import appbox.model.ModelBase;
import appbox.serialization.BinSerializer;
import appbox.store.KeyUtil;

public final class KVInsertModelRequire extends KVInsertRequire {

    public ModelBase model;

    @Override
    public void writeTo(BinSerializer bs) throws Exception {
        super.writeTo(bs);

        //key
        bs.writeVariant(9);
        bs.writeByte(KeyUtil.METACF_MODEL_PREFIX);
        bs.writeLongBE(model.id()); //暂大字节序写入

        //refs
        bs.writeVariant(-1);

        //data
        bs.writeByte(model.modelType().value); //注意写入模型类型信息
        model.writeTo(bs);
    }
}
