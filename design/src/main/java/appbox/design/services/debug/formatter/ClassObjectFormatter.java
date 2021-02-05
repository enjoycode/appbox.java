package appbox.design.services.debug.formatter;

import java.util.Map;
import java.util.function.BiFunction;

import com.sun.jdi.ClassObjectReference;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.Type;

public class ClassObjectFormatter extends ObjectFormatter {
    public ClassObjectFormatter(BiFunction<Type, Map<String, Object>, String> typeStringFunction) {
        super(typeStringFunction);
    }

    @Override
    protected String getPrefix(ObjectReference value, Map<String, Object> options) {
        Type classType = ((ClassObjectReference) value).reflectedType();
        return String.format("%s (%s)", super.getPrefix(value, options),
                typeToStringFunction.apply(classType, options));
    }

    @Override
    public boolean acceptType(Type type, Map<String, Object> options) {
        return super.acceptType(type, options) && (type.signature().charAt(0) == TypeIdentifiers.CLASS_OBJECT
                || type.signature().equals(TypeIdentifiers.CLASS_SIGNATURE));
    }
}
