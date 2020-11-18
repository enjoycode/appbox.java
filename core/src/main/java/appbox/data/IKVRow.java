package appbox.data;

import appbox.serialization.IEntityMemberReader;

public interface IKVRow {
    void readMember(short id, IEntityMemberReader bs, int flags);
}
