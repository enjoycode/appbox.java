package appbox.design.services.debug.formatter;

import java.util.HashMap;
import java.util.Map;

import com.sun.jdi.Type;
import com.sun.jdi.Value;
import com.sun.jdi.VirtualMachine;

public class NumericFormatter implements IValueFormatter {
    public static final  String                         NUMERIC_FORMAT_OPTION     = "numeric_format";
    public static final  String                         NUMERIC_PRECISION_OPTION  = "numeric_precision";
    private static final NumericFormatEnum              DEFAULT_NUMERIC_FORMAT    = NumericFormatEnum.DEC;
    private static final int                            DEFAULT_NUMERIC_PRECISION = 0;
    private static final Map<NumericFormatEnum, String> enumFormatMap             = new HashMap<>();

    static {
        enumFormatMap.put(NumericFormatEnum.DEC, "%d");
        enumFormatMap.put(NumericFormatEnum.HEX, "%#x");
        enumFormatMap.put(NumericFormatEnum.OCT, "%#o");
    }

    /**
     * Get the string representations for an object.
     * @param obj     the value object
     * @param options extra information for printing
     * @return the string representations.
     */
    @Override
    public String toString(Object obj, Map<String, Object> options) {
        Value value      = (Value) obj;
        char  signature0 = value.type().signature().charAt(0);
        if (signature0 == TypeIdentifiers.LONG
                || signature0 == TypeIdentifiers.INT
                || signature0 == TypeIdentifiers.SHORT
                || signature0 == TypeIdentifiers.BYTE) {
            return formatNumber(Long.parseLong(value.toString()), options);
        } else if (hasFraction(signature0)) {
            return formatFloatDouble(Double.parseDouble(value.toString()), options);
        }

        throw new UnsupportedOperationException(String.format("%s is not a numeric type.", value.type().name()));
    }

    @Override
    public Value valueOf(String value, Type type, Map<String, Object> options) {
        VirtualMachine vm         = type.virtualMachine();
        char           signature0 = type.signature().charAt(0);
        if (signature0 == TypeIdentifiers.LONG
                || signature0 == TypeIdentifiers.INT
                || signature0 == TypeIdentifiers.SHORT
                || signature0 == TypeIdentifiers.BYTE) {
            long number = parseNumber(value);
            if (signature0 == TypeIdentifiers.LONG) {
                return vm.mirrorOf(number);
            } else if (signature0 == TypeIdentifiers.INT) {
                return vm.mirrorOf((int) number);
            } else if (signature0 == TypeIdentifiers.SHORT) {
                return vm.mirrorOf((short) number);
            } else if (signature0 == TypeIdentifiers.BYTE) {
                return vm.mirrorOf((byte) number);
            }
        } else if (hasFraction(signature0)) {
            double doubleNumber = parseFloatDouble(value);
            if (signature0 == TypeIdentifiers.DOUBLE) {
                return vm.mirrorOf(doubleNumber);
            } else {
                return vm.mirrorOf((float) doubleNumber);
            }
        }

        throw new UnsupportedOperationException(String.format("%s is not a numeric type.", type.name()));
    }


    /**
     * The conditional function for this formatter.
     * @param type the JDI type
     * @return whether or not this formatter is expected to work on this value.
     */
    @Override
    public boolean acceptType(Type type, Map<String, Object> options) {
        if (type == null) {
            return false;
        }
        char signature0 = type.signature().charAt(0);
        return signature0 == TypeIdentifiers.LONG
                || signature0 == TypeIdentifiers.INT
                || signature0 == TypeIdentifiers.SHORT
                || signature0 == TypeIdentifiers.BYTE
                || signature0 == TypeIdentifiers.FLOAT
                || signature0 == TypeIdentifiers.DOUBLE;
    }

    @Override
    public Map<String, Object> getDefaultOptions() {
        Map<String, Object> options = new HashMap<>();
        options.put(NUMERIC_FORMAT_OPTION, DEFAULT_NUMERIC_FORMAT);
        options.put(NUMERIC_PRECISION_OPTION, DEFAULT_NUMERIC_PRECISION);
        return options;
    }

    static String formatNumber(long value, Map<String, Object> options) {
        NumericFormatEnum formatEnum = getNumericFormatOption(options);
        return String.format(enumFormatMap.get(formatEnum), value);
    }

    private static long parseNumber(String number) {
        return Long.decode(number);
    }

    private static double parseFloatDouble(String number) {
        return Double.parseDouble(number);
    }

    private static String formatFloatDouble(double value, Map<String, Object> options) {
        int precision = getFractionPrecision(options);
        return String.format(precision > 0 ? String.format("%%.%df", precision) : "%f", value);
    }

    private static NumericFormatEnum getNumericFormatOption(Map<String, Object> options) {
        return options.containsKey(NUMERIC_FORMAT_OPTION)
                ? (NumericFormatEnum) options.get(NUMERIC_FORMAT_OPTION) : DEFAULT_NUMERIC_FORMAT;
    }

    private static boolean hasFraction(char signature0) {
        return signature0 == TypeIdentifiers.FLOAT || signature0 == TypeIdentifiers.DOUBLE;
    }

    private static int getFractionPrecision(Map<String, Object> options) {
        return options.containsKey(NUMERIC_PRECISION_OPTION)
                ? (int) options.get(NUMERIC_PRECISION_OPTION) : DEFAULT_NUMERIC_PRECISION;
    }
}
