package appbox.core.serialization;

public final class PayloadType {
    public static final byte Null         = 0;
    public static final byte BooleanTrue  = 1;
    public static final byte BooleanFalse = 2;
    public static final byte Byte         = 3;
    public static final byte Char         = 4;
    public static final byte Decimal      = 5;
    public static final byte Float        = 6;
    public static final byte Double       = 7;
    public static final byte Int16        = 8;
    public static final byte Int32        = 9;
    public static final byte Int64        = 10;
    public static final byte DateTime     = 15;
    public static final byte String       = 16;
    public static final byte Guid         = 17;

    public static final byte Map   = 18;
    public static final byte Array = 19;
    public static final byte List  = 20;

    /**
     * 扩展类型
     */
    public static final byte ExtKnownType = 21;
    /**
     * 对象引用
     */
    public static final byte ObjectRef    = 22;
    /**
     * 未知类型
     */
    public static final byte UnknownType  = 23;

}
