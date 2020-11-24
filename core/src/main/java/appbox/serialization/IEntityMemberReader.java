package appbox.serialization;

//注意不需要处理可为空的成员

import appbox.data.EntityId;

import java.util.Date;
import java.util.UUID;

public interface IEntityMemberReader {
    int SF_NONE = 0;

    String readStringMember(int flags);

    boolean readBoolMember(int flags);

    int readIntMember(int flags);

    byte readByteMember(int flags);

    UUID readUUIDMember(int flags);

    byte[] readBinaryMember(int flags);

    Date readDateMember(int flags);

    EntityId readEntityIdMember(int flags);
}
