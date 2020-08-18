package appbox.server.channel;

public final class MessageType {
    public static final byte RawData            = 0;
    public static final byte InvalidModelsCache = 1; //TODO: remove it

    public static final byte InvokeRequire  = 10;
    public static final byte InvokeResponse = 11;

    public static final byte MetaNewAppRequire      = 20;
    public static final byte MetaNewAppResponse     = 21;
    public static final byte MetaGenModelIdRequire  = 22;
    public static final byte MetaGenModelIdResponse = 23;

    public static final byte KVBeginTxnRequire  = 26;
    public static final byte KVBeginTxnResponse = 27;
    public static final byte KVEndTxnRequire    = 28;
    public static final byte KVCommandResponse  = 30;
    public static final byte KVInsertRequire    = 31;
    public static final byte KVUpdateRequire    = 32;
    public static final byte KVDeleteRequire    = 33;

    public static final byte ExitReadLoop = -128;
}
