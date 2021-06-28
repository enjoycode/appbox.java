package appbox.design.lang.java.debug.formatter;


import java.util.Map;

import com.sun.jdi.Type;
import com.sun.jdi.Value;

public class NullObjectFormatter implements IValueFormatter {
    public static final String NULL_STRING = "null";

    @Override
    public String toString(Object value, Map<String, Object> options) {
        return NULL_STRING;
    }

    @Override
    public boolean acceptType(Type type, Map<String, Object> options) {
        return type == null;
    }

    @Override
    public Value valueOf(String value, Type type, Map<String, Object> options) {
        if (value == null || NULL_STRING.equals(value)) {
            return null;
        }
        throw new UnsupportedOperationException("Set value is not supported by NullObjectFormatter.");
    }

}
