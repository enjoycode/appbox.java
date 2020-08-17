package appbox.server.channel;

public final class MessageType {
    public static final byte RawData            = 0;
    public static final byte InvalidModelsCache = 1; //TODO: remove it

    public static final byte InvokeRequire  = 10;
    public static final byte InvokeResponse = 11;

    public static final byte NewAppRequire  = 20;
    public static final byte NewAppResponse = 21;

    public static final byte KVBeginTxnRequire  = 26;
    public static final byte KVBeginTxnResponse = 27;
    public static final byte KVEndTxnRequire    = 28;
    public static final byte KVCommandResponse  = 30;
    public static final byte KVInsertCommand    = 31;

    public static final byte ExitReadLoop = -128;
}
