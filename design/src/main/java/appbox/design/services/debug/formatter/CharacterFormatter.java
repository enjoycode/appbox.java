package appbox.design.services.debug.formatter;

import java.util.Map;

import com.sun.jdi.Type;
import com.sun.jdi.Value;
import com.sun.jdi.VirtualMachine;

public class CharacterFormatter implements IValueFormatter {

    @Override
    public String toString(Object value, Map<String, Object> options) {
        return value == null ? NullObjectFormatter.NULL_STRING : value.toString();
    }

    @Override
    public boolean acceptType(Type type, Map<String, Object> options) {
        if (type == null) {
            return false;
        }
        char signature0 = type.signature().charAt(0);
        return signature0 == TypeIdentifiers.CHAR;
    }

    @Override
    public Value valueOf(String value, Type type, Map<String, Object> options) {
        VirtualMachine vm = type.virtualMachine();
        if (value == null) {
            return null;
        }
        if (value.length() == 3
                && value.startsWith("'")
                && value.endsWith("'")) {
            return type.virtualMachine().mirrorOf(value.charAt(1));
        }
        return vm.mirrorOf(value.charAt(0));
    }
}
