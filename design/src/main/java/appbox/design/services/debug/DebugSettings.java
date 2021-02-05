package appbox.design.services.debug;

public class DebugSettings {
    private static final DebugSettings current = new DebugSettings();

    public int maxStringLength = 0;
    public int numericPrecision = 0;
    public boolean showQualifiedNames = false;
    public boolean showHex = false;
    public int limitOfVariablesPerJdwpRequest = 100;

    public static DebugSettings getCurrent() {
        return current;
    }
}
