package appbox.design.lang.java.debug.formatter;

import java.util.Map;

import com.sun.jdi.Type;
import com.sun.jdi.Value;

public interface IValueFormatter extends IFormatter {
    /**
     * Create the value from string, this method is used in setValue feature
     * where converts user-input string to JDI value.
     * @param value   the string text.
     * @param type    the expected value type.
     * @param options additional information about expected format
     * @return the JDI value.
     */
    Value valueOf(String value, Type type, Map<String, Object> options);
}
