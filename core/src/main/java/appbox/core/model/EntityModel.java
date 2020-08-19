package appbox.core.model;

import appbox.core.data.PersistentState;
import appbox.core.model.entity.IEntityStoreOption;
import appbox.core.model.entity.SysStoreOptions;

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
        if (persistentState() != PersistentState.Detached && _storeOptions != null
                && _storeOptions.storeType() == IEntityStoreOption.StoreType.SysStore) {
            ((SysStoreOptions)_storeOptions).changeSchemaVersion();
        }
    }
    //endregion
}
