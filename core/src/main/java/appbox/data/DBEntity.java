package appbox.data;

import appbox.serialization.IInputStream;
import appbox.serialization.IOutputStream;

import java.util.ArrayList;
import java.util.List;

/** 映射至存储的实体基类 */
public abstract class DBEntity extends Entity {

    private PersistentState _persistentState = PersistentState.Detached;
    private List<Short>     _changedMembers  = null;

    public DBEntity(long modelId) {
        super(modelId);
    }

    public final PersistentState persistentState() { return _persistentState; }

    protected final void onPropertyChanged(short memberId) {
        if (_persistentState == PersistentState.Unchnaged) {
            _persistentState = PersistentState.Modified;

            //track member changes
            if (_changedMembers == null) {
                _changedMembers = new ArrayList<>();
                _changedMembers.add(memberId);
            } else {
                if (_changedMembers.stream().noneMatch(t -> t == memberId)) {
                    _changedMembers.add(memberId);
                }
            }
        }
    }

    @Override
    public void writeTo(IOutputStream bs) {
        super.writeTo(bs);

        bs.writeByte(_persistentState.value);
        bs.writeListShort(_changedMembers);
    }

    @Override
    public void readFrom(IInputStream bs) {
        super.readFrom(bs);

        _persistentState = PersistentState.fromValue(bs.readByte());
        _changedMembers  = bs.readListShort();
    }
}
