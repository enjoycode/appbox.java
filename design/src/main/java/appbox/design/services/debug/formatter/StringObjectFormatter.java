package appbox.design.services.debug.formatter;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.sun.jdi.StringReference;
import com.sun.jdi.Type;
import com.sun.jdi.Value;

public class StringObjectFormatter extends ObjectFormatter implements IValueFormatter {
    public static final  String MAX_STRING_LENGTH_OPTION  = "max_string_length";
    private static final int    DEFAULT_MAX_STRING_LENGTH = 0;
    private static final String QUOTE_STRING              = "\"";

    public StringObjectFormatter() {
        super(null);
    }

    @Override
    public Map<String, Object> getDefaultOptions() {
        Map<String, Object> options = new HashMap<>();
        options.put(MAX_STRING_LENGTH_OPTION, DEFAULT_MAX_STRING_LENGTH);
        return options;
    }

    @Override
    public String toString(Object value, Map<String, Object> options) {
        int maxLength = getMaxStringLength(options);
        return String.format("\"%s\"",
                maxLength > 0 ? StringUtils.abbreviate(((StringReference) value).value(), maxLength) : ((StringReference) value).value());
    }

    @Override
    public boolean acceptType(Type type, Map<String, Object> options) {
        return type != null && (type.signature().charAt(0) == TypeIdentifiers.STRING
                || type.signature().equals(TypeIdentifiers.STRING_SIGNATURE));
    }

    @Override
    public Value valueOf(String value, Type type, Map<String, Object> options) {
        if (value == null || NullObjectFormatter.NULL_STRING.equals(value)) {
            return null;
        }
        if (value.length() >= 2
                && value.startsWith(QUOTE_STRING)
                && value.endsWith(QUOTE_STRING)) {
            return type.virtualMachine().mirrorOf(StringUtils.substring(value, 1, -1));
        }
        return type.virtualMachine().mirrorOf(value);
    }

    private static int getMaxStringLength(Map<String, Object> options) {
        return options.containsKey(MAX_STRING_LENGTH_OPTION)
                ? (int) options.get(MAX_STRING_LENGTH_OPTION) : DEFAULT_MAX_STRING_LENGTH;
    }
}
