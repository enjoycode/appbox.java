package appbox.design.debug.proto;

/**
 * StackTrace request; value of command field is 'stackTrace'. The request returns a stacktrace from the current
 * execution state.
 */
public class StackTraceRequest extends Request {
    public StackTraceArguments arguments;
}
