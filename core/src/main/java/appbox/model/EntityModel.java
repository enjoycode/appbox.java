package appbox.model;

import appbox.data.PersistentState;
import appbox.model.entity.IEntityStoreOption;
import appbox.model.entity.SysStoreOptions;

public final class EntityModel extends ModelBase {
    private static final short MAX_MEMBER_ID = 512;

    private short _devMemberIdSeq;
    private short _usrMemberIdSeq;

    private IEntityStoreOption _storeOptions; //null表示DTO

    //region ====Properties====
    @Override
    public ModelType modelType() {
        return ModelType.Entity;
    }

    public SysStoreOptions sysStoreOptions() {
        return _storeOptions != null && _storeOptions instanceof SysStoreOptions ?
                (SysStoreOptions)_storeOptions : null;
    }
    //endregion

    //region ====Design Methods====
    @Override
    public void acceptChanges() {
        super.acceptChanges();

        //TODO: members

        if (_storeOptions != null) {
            _storeOptions.acceptChanges();
        }
    }

    public void changeSchemaVersion() {
        if (persistentState() != PersistentState.Detached && sysStoreOptions() != null) {
            ((SysStoreOptions)_storeOptions).changeSchemaVersion();
        }
    }
    //endregion
}
