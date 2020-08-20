package appbox.model;

import appbox.data.PersistentState;
import appbox.model.entity.EntityMemberModel;
import appbox.model.entity.IEntityStoreOption;
import appbox.model.entity.SysStoreOptions;
import appbox.utils.IdUtil;

import java.util.ArrayList;
import java.util.List;

public final class EntityModel extends ModelBase {
    private static final short MAX_MEMBER_ID = 512;

    private short _devMemberIdSeq;
    private short _usrMemberIdSeq;

    private final ArrayList<EntityMemberModel> _members = new ArrayList<>(); //注意已按memberId排序
    private       IEntityStoreOption           _storeOptions; //null表示DTO

    /**
     * only for Serialization
     */
    public EntityModel() {}

    /**
     * New EntityModel for SysStore
     */
    public EntityModel(long id, String name, boolean mvcc, boolean orderByDesc) {
        super(id, name);

        _storeOptions = new SysStoreOptions(mvcc, orderByDesc);
    }

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
    public EntityMemberModel tryGetMember(String name) {
        for (EntityMemberModel member : _members) {
            if (member.name().equals(name)) {
                return member;
            }
        }
        return null;
    }

    public EntityMemberModel getMember(String name) throws Exception {
        var m = tryGetMember(name);
        if (m == null) {
            throw new Exception("Member with name: " + name + " not exists");
        }
        return m;
    }

    public EntityMemberModel tryGetMember(short id) {
        return binarySearch(_members, id);
    }

    public EntityMemberModel getMember(short id) throws Exception {
        var m = tryGetMember(id);
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

    public void addMember(EntityMemberModel member, boolean byImport) throws Exception {
        checkDesignMode();
        member.canAddTo(this);

        if (!byImport) { //非导入的需要生成成员标识
            //TODO:通过设计时上下文获取ApplicationModel是否导入，从而确认当前Layer
            var layer = ModelLayer.DEV;
            var seq   = layer == ModelLayer.DEV ? ++_devMemberIdSeq : ++_usrMemberIdSeq;
            if (seq >= MAX_MEMBER_ID) { //TODO:尝试找空的
                throw new Exception("Member id out of range");
            }
            member.initMemberId(IdUtil.makeMemberId(layer, seq));
        }
        _members.add(member);
        _members.sort((m1, m2) -> Short.compare(m1.memberId(), m2.memberId()));

        if (!member.allowNull()) { //仅None nullable
            changeSchemaVersion();
        }
        onPropertyChanged();
    }

    /**
     * Only for StoreInitiator
     */
    public void addSysMember(EntityMemberModel member, short id) throws Exception {
        checkDesignMode();
        member.canAddTo(this);

        member.initMemberId(id); //已处理Layer标记
        _members.add(member);
        _members.sort((m1, m2) -> Short.compare(m1.memberId(), m2.memberId()));
    }
    //endregion
}
