package appbox.server.channel;

public final class MessageType {
    public static final byte RawData            = 0;
    public static final byte InvalidModelsCache = 1;

    public static final byte InvokeRequire  = 10;
    public static final byte InvokeResponse = 11;

    public static final byte ExitReadLoop = -128;
}
