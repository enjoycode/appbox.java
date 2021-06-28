package appbox.design.lang.java.debug.formatter;

import java.util.Map;
import java.util.function.BiFunction;

import com.sun.jdi.ArrayReference;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.Type;
import com.sun.jdi.Value;

public class ArrayObjectFormatter extends ObjectFormatter {
    public ArrayObjectFormatter(BiFunction<Type, Map<String, Object>, String> typeStringFunction) {
        super(typeStringFunction);
    }

    @Override
    protected String getPrefix(ObjectReference value, Map<String, Object> options) {
        String arrayTypeWithLength = String.format("[%s]",
                NumericFormatter.formatNumber(arrayLength(value), options));
        return super.getPrefix(value, options).replaceFirst("\\[]", arrayTypeWithLength);
    }

    @Override
    public boolean acceptType(Type type, Map<String, Object> options) {
        return type != null && type.signature().charAt(0) == TypeIdentifiers.ARRAY;
    }

    private static int arrayLength(Value value) {
        return ((ArrayReference) value).length();
    }
}
