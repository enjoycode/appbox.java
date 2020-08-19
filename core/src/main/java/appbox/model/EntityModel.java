package appbox.model;

import appbox.data.PersistentState;
import appbox.model.entity.EntityMemberModel;
import appbox.model.entity.IEntityStoreOption;
import appbox.model.entity.SysStoreOptions;

import java.util.ArrayList;
import java.util.List;

public final class EntityModel extends ModelBase {
    private static final short MAX_MEMBER_ID = 512;

    private short _devMemberIdSeq;
    private short _usrMemberIdSeq;

    private final ArrayList<EntityMemberModel> _members = new ArrayList<>(); //注意已按memberId排序
    private       IEntityStoreOption           _storeOptions; //null表示DTO

    //region ====Properties====
    @Override
    public ModelType modelType() {
        return ModelType.Entity;
    }

    public SysStoreOptions sysStoreOptions() {
        return _storeOptions != null && _storeOptions instanceof SysStoreOptions ?
                (SysStoreOptions) _storeOptions : null;
    }
    //endregion

    //region ====GetMember Methods====
    public EntityMemberModel tryGetMember(short id) {
        return binarySearch(_members, id);
    }

    public EntityMemberModel getMember(short id) throws Exception {
        var m = binarySearch(_members, id);
        if (m == null) {
            throw new Exception("Member with id: " + id + " not exists");
        }
        return m;
    }

    private static EntityMemberModel binarySearch(List<EntityMemberModel> l, short id) {
        int low  = 0;
        int high = l.size() - 1;

        while (low <= high) {
            int               mid    = low + high >>> 1;
            EntityMemberModel midVal = l.get(mid);
            int               cmp    = Short.compare(midVal.memberId(), id);
            if (cmp < 0) {
                low = mid + 1;
            } else {
                if (cmp <= 0) {
                    return midVal;
                }

                high = mid - 1;
            }
        }

        return null;
    }
    //endregion

    //region ====Design Methods====
    @Override
    public void acceptChanges() {
        super.acceptChanges();

        for (int i = _members.size() - 1; i >= 0; i--) {
            if (_members.get(i).persistentState() == PersistentState.Deleted) {
                _members.remove(i);
            } else {
                _members.get(i).acceptChanges();
            }
        }

        if (_storeOptions != null) {
            _storeOptions.acceptChanges();
        }
    }

    public void changeSchemaVersion() {
        if (persistentState() != PersistentState.Detached && sysStoreOptions() != null) {
            ((SysStoreOptions) _storeOptions).changeSchemaVersion();
        }
    }
    //endregion
}
