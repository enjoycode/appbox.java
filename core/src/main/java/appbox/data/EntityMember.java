package appbox.data;

public final class EntityMember {
    private long _lowBits;
    private long _highBits;
    private Object _objRef;
    private short _id;
    private byte _type;  //EntityMemberType << 3 | DataFieldType
    private byte _flag;
}
