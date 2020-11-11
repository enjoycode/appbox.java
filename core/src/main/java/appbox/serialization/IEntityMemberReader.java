package appbox.serialization;

//注意不需要处理可为空的成员

public interface IEntityMemberReader {
    int SF_NONE = 0;

    String readStringMember(int flags) throws Exception;

    boolean readBoolMemeber(int flags) throws Exception;

    int readIntMember(int flags) throws Exception;

}
