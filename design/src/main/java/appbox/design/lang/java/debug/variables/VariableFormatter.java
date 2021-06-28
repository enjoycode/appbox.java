package appbox.design.lang.java.debug.variables;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import appbox.design.lang.java.debug.formatter.*;
import com.sun.jdi.Type;
import com.sun.jdi.Value;

public class VariableFormatter implements IVariableFormatter {

    public static final VariableFormatter DEFAULT;

    static {
        DEFAULT = new VariableFormatter();
        DEFAULT.registerTypeFormatter(new SimpleTypeFormatter(), 1);
        DEFAULT.registerValueFormatter(new BooleanFormatter(), 1);
        DEFAULT.registerValueFormatter(new CharacterFormatter(), 1);
        DEFAULT.registerValueFormatter(new NumericFormatter(), 1);
        DEFAULT.registerValueFormatter(new ObjectFormatter(DEFAULT::typeToString), 1);
        DEFAULT.registerValueFormatter(new NullObjectFormatter(), 1);

        DEFAULT.registerValueFormatter(new StringObjectFormatter(), 2);
        DEFAULT.registerValueFormatter(new ArrayObjectFormatter(DEFAULT::typeToString), 2);
        DEFAULT.registerValueFormatter(new ClassObjectFormatter(DEFAULT::typeToString), 2);
    }

    private Map<IValueFormatter, Integer> valueFormatterMap;
    private Map<ITypeFormatter, Integer>  typeFormatterMap;

    /**
     * Creates a variable formatter.
     */
    public VariableFormatter() {
        valueFormatterMap = new HashMap<>();
        typeFormatterMap  = new HashMap<>();
    }

    private static IFormatter getFormatter(Map<? extends IFormatter, Integer> formatterMap, Type type,
                                           Map<String, Object> options) {
        List<? extends IFormatter> formatterList =
                formatterMap.keySet().stream().filter(t -> t.acceptType(type, options))
                        .sorted((a, b) ->
                                -Integer.compare(formatterMap.get(a), formatterMap.get(b))).collect(Collectors.toList());
        if (formatterList.isEmpty()) {
            throw new UnsupportedOperationException(String.format("There is no related formatter for type %s.",
                    type == null ? "null" : type.name()));
        }
        return formatterList.get(0);
    }

    /**
     * Get display name of type.
     * @param type    the JDI type
     * @param options additional information about expected format
     * @return display name of type of the value.
     */
    @Override
    public String typeToString(Type type, Map<String, Object> options) {
        IFormatter formatter = getFormatter(this.typeFormatterMap, type, options);
        return formatter.toString(type, options);
    }

    /**
     * Get the default options for all formatters registered.
     * @return The default options.
     */
    @Override
    public Map<String, Object> getDefaultOptions() {
        Map<String, Object> defaultOptions = new HashMap<>();
        int count1 = valueFormatterMap.keySet().stream().mapToInt(
                formatter -> this.mergeDefaultOptions(formatter, defaultOptions)).sum();
        int count2 = typeFormatterMap.keySet().stream().mapToInt(
                formatter -> this.mergeDefaultOptions(formatter, defaultOptions)).sum();
        if (count1 + count2 != defaultOptions.size()) {
            throw new IllegalStateException("There is some configuration conflicts on type and value formatters.");
        }
        return defaultOptions;
    }


    /**
     * Get display text of the value.
     * @param value   the value.
     * @param options additional information about expected format
     * @return the display text of the value
     */
    @Override
    public String valueToString(Value value, Map<String, Object> options) {
        Type       type      = value == null ? null : value.type();
        IFormatter formatter = getFormatter(this.valueFormatterMap, type, options);
        return formatter.toString(value, options);
    }

    @Override
    public Value stringToValue(String stringValue, Type type, Map<String, Object> options) {
        IValueFormatter formatter = (IValueFormatter) getFormatter(this.valueFormatterMap, type, options);
        return formatter.valueOf(stringValue, type, options);
    }

    public void registerValueFormatter(IValueFormatter formatter, int priority) {
        valueFormatterMap.put(formatter, priority);
    }

    public void registerTypeFormatter(ITypeFormatter typeFormatter, int priority) {
        typeFormatterMap.put(typeFormatter, priority);
    }

    private int mergeDefaultOptions(IFormatter formatter, Map<String, Object> options) {
        int count = 0;
        for (Map.Entry<String, Object> entry : formatter.getDefaultOptions().entrySet()) {
            options.put(entry.getKey(), entry.getValue());
            count++;
        }
        return count;
    }
}
