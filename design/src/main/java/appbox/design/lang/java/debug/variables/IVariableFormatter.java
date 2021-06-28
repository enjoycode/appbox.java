package appbox.design.lang.java.debug.variables;

import appbox.design.lang.java.debug.formatter.ITypeFormatter;
import appbox.design.lang.java.debug.formatter.IValueFormatter;
import com.sun.jdi.Type;
import com.sun.jdi.Value;

import java.util.Map;

public interface IVariableFormatter {
    /**
     * Register a type formatter. Be careful about the priority of formatters, the formatter with the largest
     * priority which accepts the type will be used.
     * @param typeFormatter the type formatter
     * @param priority      the priority for this formatter
     */
    void registerTypeFormatter(ITypeFormatter typeFormatter, int priority);

    /**
     * Register a value formatter. Be careful about the priority of formatters, the formatter with the largest
     * priority which accepts the type will be used.
     * @param formatter the value formatter
     * @param priority  the priority for this formatter
     */
    void registerValueFormatter(IValueFormatter formatter, int priority);

    /**
     * Get the default options for all formatters registered.
     * @return The default options.
     */
    Map<String, Object> getDefaultOptions();

    /**
     * Get display text of the value.
     * @param value   the value.
     * @param options additional information about expected format
     * @return the display text of the value
     */
    String valueToString(Value value, Map<String, Object> options);

    /**
     * Get the JDI value of a String.
     * @param stringValue the text of the value need to be converted.
     * @param options     additional information about expected format
     * @return the jdi value
     */
    Value stringToValue(String stringValue, Type type, Map<String, Object> options);

    /**
     * Get display name of type.
     * @param type    the JDI type
     * @param options additional information about expected format
     * @return display name of type of the value.
     */
    String typeToString(Type type, Map<String, Object> options);
}
