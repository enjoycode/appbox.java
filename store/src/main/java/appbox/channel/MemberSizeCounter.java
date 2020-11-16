package appbox.channel;

import appbox.serialization.IEntityMemberWriter;
import appbox.utils.StringUtil;

import java.util.Date;
import java.util.Optional;
import java.util.UUID;

/** 用于存储层计算成员长度，如计算索引Key的长度 */
public final class MemberSizeCounter implements IEntityMemberWriter {
    private int size;

    public int getSize() { return size; }

    @Override
    public void writeMember(short id, String value, byte flags) throws Exception {
        size += 2 + 3 + StringUtil.getUtf8Size(value);
    }

    @Override
    public void writeMember(short id, int value, byte flags) throws Exception {
        size += 2 + 4;
    }

    @Override
    public void writeMember(short id, Optional<Integer> value, byte flags) throws Exception {
        size += 2 + (value.isPresent() ? 4 : 0);
    }

    @Override
    public void writeMember(short id, UUID value, byte flags) throws Exception {
        size += 2 + 16;
    }

    @Override
    public void writeMember(short id, byte[] data, byte flags) throws Exception {
        size += 2 + 3;
        if (data != null)
            size += data.length;
    }

    @Override
    public void writeMember(short id, boolean value, byte flags) throws Exception {
        size += 2;
    }

    @Override
    public void writeMember(short id, Date birthday, byte flags) throws Exception {
        size += 2 + 8;
    }
}
