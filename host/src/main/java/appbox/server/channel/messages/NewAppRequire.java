package appbox.server.channel.messages;

import appbox.core.model.ApplicationModel;
import appbox.core.serialization.BinDeserializer;
import appbox.core.serialization.BinSerializer;
import appbox.server.channel.MessageType;

public final class NewAppRequire implements IMessage {

    public final ApplicationModel Application;

    public NewAppRequire(ApplicationModel app) {
        Application = app;
    }

    @Override
    public byte MessageType() {
        return MessageType.NewAppRequire;
    }

    //region ====Serialization====
    @Override
    public void writeTo(BinSerializer bs) throws Exception {

    }

    @Override
    public void readFrom(BinDeserializer bs) throws Exception {

    }
    //endregion
}