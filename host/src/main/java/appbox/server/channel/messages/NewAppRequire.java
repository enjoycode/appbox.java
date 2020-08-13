package appbox.server.channel.messages;

import appbox.core.model.ApplicationModel;
import appbox.core.serialization.BinDeserializer;
import appbox.core.serialization.BinSerializer;
import appbox.server.channel.MessageType;
import appbox.store.KeyUtil;

public final class NewAppRequire implements IMessage {

    public final ApplicationModel application;

    public NewAppRequire(ApplicationModel app) {
        application = app;
    }

    @Override
    public byte MessageType() {
        return MessageType.NewAppRequire;
    }

    //region ====Serialization====
    @Override
    public void writeTo(BinSerializer bs) throws Exception {
        //写入５字节Key
        bs.writeByte(KeyUtil.METACF_APP_PREFIX);
        bs.writeInt(application.Id());
        //写入模型数据,注意不需要写入头部9字节，由读取端处理
        application.writeTo(bs);
    }

    @Override
    public void readFrom(BinDeserializer bs) throws Exception {

    }
    //endregion
}