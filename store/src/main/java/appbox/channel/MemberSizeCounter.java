package appbox.channel;

import appbox.data.Entity;
import appbox.data.EntityId;
import appbox.serialization.IEntityMemberWriter;
import appbox.utils.StringUtil;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/** 用于存储层计算成员长度，如计算索引Key的长度 */
public final class MemberSizeCounter implements IEntityMemberWriter {
    private int size;

    public int getSize() { return size; }

    @Override
    public void writeMember(short id, EntityId value, byte flags) {
        size += 2;
        if (value != null)
            size += 16;
    }

    @Override
    public void writeMember(short id, String value, byte flags) {
        size += 2;
        if (value != null)
            size += 3 + StringUtil.getUtf8Size(value);
    }

    @Override
    public void writeMember(short id, byte value, byte flags) {
        size += 2 + 1;
    }

    @Override
    public void writeMember(short id, int value, byte flags) {
        size += 2 + 4;
    }

    @Override
    public void writeMember(short id, Integer value, byte flags) {
        size += 2 + (value != null ? 4 : 0);
    }

    @Override
    public void writeMember(short id, long value, byte flags) {
        size += 2 + 8;
    }

    @Override
    public void writeMember(short id, UUID value, byte flags) {
        size += 2;
        if (value != null)
            size += 16;
    }

    @Override
    public void writeMember(short id, byte[] value, byte flags) {
        size += 2 + 3;
        if (value != null)
            size += value.length;
    }

    @Override
    public void writeMember(short id, boolean value, byte flags) {
        size += 2;
    }

    @Override
    public void writeMember(short id, LocalDateTime value, byte flags) {
        size += 2;
        if (value != null)
            size += 8;
    }

    @Override
    public void writeMember(short id, Entity value, byte flags) {
        //do nothing
    }

    @Override
    public void writeMember(short id, List<? extends Entity> value, byte flags) {
        //do nothing
    }
}
