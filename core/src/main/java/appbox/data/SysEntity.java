package appbox.data;

public abstract class SysEntity extends Entity implements IKVRow {
    private final EntityId        _id              = new EntityId();
    private       PersistentState _persistentState = PersistentState.Detached;

    public SysEntity(long modelId) {
        super(modelId);
    }

    public final EntityId id() {
        return _id;
    }

    protected final PersistentState persistentState() { return _persistentState; }

    protected final void onPropertyChanged(short memberId) {
        //TODO: track member changes
        if (_persistentState == PersistentState.Unchnaged) {
            _persistentState = PersistentState.Modified;
        }
    }
}
