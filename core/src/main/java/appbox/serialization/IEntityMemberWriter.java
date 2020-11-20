package appbox.serialization;

import appbox.data.EntityId;

import java.util.Date;
import java.util.Optional;
import java.util.UUID;

public interface IEntityMemberWriter {
    byte SF_NONE          = 0;
    byte SF_STORE         = 1;
    byte SF_WRITE_NULL    = 2;
    byte SF_ORDER_BY_DESC = 4;

    void writeMember(short id, EntityId value, byte flags);

    void writeMember(short id, String value, byte flags);

    void writeMember(short id, byte value, byte flags);

    void writeMember(short id, int value, byte flags);

    void writeMember(short id, Optional<Integer> value, byte flags);

    void writeMember(short id, long value, byte flags);

    void writeMember(short id, UUID value, byte flags);

    void writeMember(short id, byte[] data, byte flags);

    void writeMember(short id, boolean male, byte flags);

    void writeMember(short id, Date value, byte flags);
}
