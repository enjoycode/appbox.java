package appbox.entities;

import appbox.data.Entity;
import appbox.data.EntityId;
import appbox.serialization.IEntityMemberWriter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/** 用于读取实体指定成员的值(Boxed) */
public final class EntityMemberValueGetter implements IEntityMemberWriter {
    public Object value;

    @Override
    public void writeMember(short id, EntityId value, byte flags) {
        this.value = value;
    }

    @Override
    public void writeMember(short id, String value, byte flags) {
        this.value = value;
    }

    @Override
    public void writeMember(short id, byte value, byte flags) {
        this.value = value;
    }

    @Override
    public void writeMember(short id, int value, byte flags) {
        this.value = value;
    }

    @Override
    public void writeMember(short id, Integer value, byte flags) {
        this.value = value;
    }

    @Override
    public void writeMember(short id, long value, byte flags) {
        this.value = value;
    }

    @Override
    public void writeMember(short id, UUID value, byte flags) {
        this.value = value;
    }

    @Override
    public void writeMember(short id, byte[] value, byte flags) {
        this.value = value;
    }

    @Override
    public void writeMember(short id, boolean value, byte flags) {
        this.value = value;
    }

    @Override
    public void writeMember(short id, LocalDateTime value, byte flags) {
        this.value = value;
    }

    @Override
    public void writeMember(short id, Entity value, byte flags) {
        this.value = value;
    }

    @Override
    public void writeMember(short id, List<? extends Entity> value, byte flags) {
        this.value = value;
    }
}
