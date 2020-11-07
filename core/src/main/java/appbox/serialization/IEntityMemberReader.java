package appbox.serialization;

//注意不需要处理可为空的成员

public interface IEntityMemberReader {
    int SF_NONE = 0;

    String readStringMember(int storeFlags) throws Exception;

    boolean readBoolMemeber(int storeFlags) throws Exception;

    int readIntMember(int storeFlags) throws Exception;

}
