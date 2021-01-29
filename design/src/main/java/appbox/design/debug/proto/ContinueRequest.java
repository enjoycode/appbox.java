package appbox.design.debug.proto;

/** Continue request; value of command field is 'continue'. The request starts the debuggee to run again. */
public class ContinueRequest extends Request {
    public ContinueArguments arguments;
}
