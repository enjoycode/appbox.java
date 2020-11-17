package appbox.data;

public abstract class SysEntity extends Entity implements IKVRow {
    private final EntityId _id = new EntityId();

    public SysEntity(long modelId) {
        super(modelId);
    }

    public EntityId id() {
        return _id;
    }
}
