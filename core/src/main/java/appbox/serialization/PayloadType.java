package appbox.serialization;

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

    /** 扩展类型 */
    public static final byte ExtKnownType = 21;
    /** 对象引用 */
    public static final byte ObjectRef    = 22;
    /** 未知类型 */
    public static final byte UnknownType  = 23;

    //----模型相关----
    public static final byte DataStoreModel  = 45;
    public static final byte ServiceModel    = 49;
    public static final byte EntityModel     = 50;
    public static final byte EntityModelInfo = 51; //专用于封送给前端

    //----表达式相关----
    public static final byte BinaryExpression    = 63;
    public static final byte PrimitiveExpression = 64;
    public static final byte KVFieldExpression   = 78;

    public static final byte Entity   = 90;
    public static final byte EntityId = 92;

}
