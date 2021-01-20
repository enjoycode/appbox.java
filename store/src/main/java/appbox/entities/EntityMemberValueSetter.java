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
        throw new RuntimeException();
    }

    @Override
    public boolean readBoolMember(int flags) {
        throw new RuntimeException();
    }

    @Override
    public int readIntMember(int flags) {
        throw new RuntimeException();
    }

    @Override
    public byte readByteMember(int flags) {
        throw new RuntimeException();
    }

    @Override
    public long readLongMember(int flags) {
        throw new RuntimeException();
    }

    @Override
    public UUID readUUIDMember(int flags) {
        throw new RuntimeException();
    }

    @Override
    public byte[] readBinaryMember(int flags) {
        throw new RuntimeException();
    }

    @Override
    public LocalDateTime readDateMember(int flags) {
        throw new RuntimeException();
    }

    @Override
    public EntityId readEntityIdMember(int flags) {
        throw new RuntimeException();
    }

    @Override
    public <T extends Entity> T readRefMember(int flags, Supplier<T> creator) {
        throw new RuntimeException();
    }

    @Override
    public <T extends Entity> List<T> readSetMember(int flags, Supplier<T> creator) {
        return (List<T>) value;
    }
}
