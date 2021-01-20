package appbox.serialization;

//注意不需要处理可为空的成员

import appbox.data.Entity;
import appbox.data.EntityId;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

public interface IEntityMemberReader {
    int SF_NONE = 0;

    String readStringMember(int flags);

    boolean readBoolMember(int flags);

    int readIntMember(int flags);

    byte readByteMember(int flags);

    long readLongMember(int flags);

    UUID readUUIDMember(int flags);

    byte[] readBinaryMember(int flags);

    LocalDateTime readDateMember(int flags);

    EntityId readEntityIdMember(int flags);

    /** EntityRef成员 */
    <T extends Entity> T readRefMember(int flags, Supplier<T> creator);

    /** EntitySet成员 */
    <T extends Entity> List<T> readSetMember(int flags, Supplier<T> creator);

}
