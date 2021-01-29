package appbox.design.debug.proto;

/** A client or debug adapter initiated request. */
public class Request extends ProtocolMessage {
    // type: 'request';
    /** The command to execute. */
    public String command;
}
