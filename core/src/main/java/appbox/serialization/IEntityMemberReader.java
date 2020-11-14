package appbox.serialization;

//注意不需要处理可为空的成员

import java.util.Date;
import java.util.UUID;

public interface IEntityMemberReader {
    int SF_NONE = 0;

    String readStringMember(int flags) throws Exception;

    boolean readBoolMember(int flags) throws Exception;

    int readIntMember(int flags) throws Exception;

    byte readByteMember(int flags) throws Exception;

    UUID readUUIDMember(int flags) throws Exception;

    byte[] readBinaryMember(int flags) throws Exception;

    Date readDateMember(int flags) throws Exception;
}
