package appbox.entities;

import appbox.data.Entity;
import appbox.data.EntityId;
import appbox.serialization.IEntityMemberReader;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

@SuppressWarnings("unchecked")
public final class EntityMemberValueSetter implements IEntityMemberReader {
    public Object value;

    @Override
    public String readStringMember(int flags) {
        return (String) value;
    }

    @Override
    public boolean readBoolMember(int flags) {
        return (boolean) value;
    }

    @Override
    public int readIntMember(int flags) {
        return (int) value;
    }

    @Override
    public byte readByteMember(int flags) {
        return (byte) value;
    }

    @Override
    public long readLongMember(int flags) {
        return (long) value;
    }

    @Override
    public UUID readUUIDMember(int flags) {
        return (UUID) value;
    }

    @Override
    public byte[] readBinaryMember(int flags) {
        return (byte[]) value;
    }

    @Override
    public LocalDateTime readDateMember(int flags) {
        return (LocalDateTime) value;
    }

    @Override
    public EntityId readEntityIdMember(int flags) {
        return (EntityId) value;
    }

    @Override
    public <T extends Entity> T readRefMember(int flags, Supplier<T> creator) {
        return (T) value;
    }

    @Override
    public <T extends Entity> List<T> readSetMember(int flags, Supplier<T> creator) {
        return (List<T>) value;
    }
}
