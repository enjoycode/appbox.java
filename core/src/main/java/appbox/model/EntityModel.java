package appbox.model;

import appbox.data.PersistentState;
import appbox.logging.Log;
import appbox.model.entity.*;
import appbox.serialization.*;
import appbox.utils.IdUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static appbox.model.entity.EntityMemberModel.EntityMemberType;

public final class EntityModel extends ModelBase implements IJsonSerializable {
    private static final short MAX_MEMBER_ID = 512;

    private short _devMemberIdSeq;
    private short _usrMemberIdSeq;

    private final ArrayList<EntityMemberModel> _members = new ArrayList<>(); //注意已按memberId排序
    private       IEntityStoreOption           _storeOptions; //null表示DTO

    /** only for Serialization */
    public EntityModel() {}

    public EntityModel(long id, String name) {
        super(id, name);
    }

    //region ====Properties====
    @Override
    public ModelType modelType() {
        return ModelType.Entity;
    }

    public List<EntityMemberModel> getMembers() {
        return _members;
    }

    public IEntityStoreOption storeOptions() { return _storeOptions; }

    public SysStoreOptions sysStoreOptions() {
        return _storeOptions != null && _storeOptions instanceof SysStoreOptions ?
                (SysStoreOptions) _storeOptions : null;
    }

    public SqlStoreOptions sqlStoreOptions() {
        return _storeOptions != null && _storeOptions instanceof SqlStoreOptions ?
                (SqlStoreOptions) _storeOptions : null;
    }

    /** 存储用的标识号，模型标识号后3字节,仅用于SysStore */
    public int tableId() { //TODO: rename to tableStoreId
        return (int) (_id & 0xFFFFFF);
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

    public EntityMemberModel getMember(String name) {
        var m = tryGetMember(name);
        if (m == null) {
            throw new RuntimeException("Member with name: " + name + " not exists");
        }
        return m;
    }

    public EntityMemberModel tryGetMember(short id) {
        return binarySearch(_members, id);
    }

    public EntityMemberModel getMember(short id) throws RuntimeException {
        var m = tryGetMember(id);
        if (m == null) {
            throw new RuntimeException("Member with id: " + id + " not exists");
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
    public void bindToSysStore(boolean mvcc, boolean orderByDesc) {
        _storeOptions = new SysStoreOptions(this, mvcc, orderByDesc);
    }

    public void bindToSqlStore(long storeId) {
        _storeOptions = new SqlStoreOptions(this, storeId);
    }

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

    public void addMember(EntityMemberModel member) {
        addMember(member, false);
    }

    public void addMember(EntityMemberModel member, boolean byImport) {
        checkDesignMode();
        member.canAddTo(this);

        if (!byImport) { //非导入的需要生成成员标识
            //TODO:通过设计时上下文获取ApplicationModel是否导入，从而确认当前Layer
            var layer = ModelLayer.DEV;
            var seq   = layer == ModelLayer.DEV ? ++_devMemberIdSeq : ++_usrMemberIdSeq;
            if (seq >= MAX_MEMBER_ID) { //TODO:尝试找空的
                throw new RuntimeException("Member id out of range");
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
    public void addSysMember(EntityMemberModel member, short id) {
        checkDesignMode();
        member.canAddTo(this);

        member.initMemberId(id); //已处理Layer标记
        _members.add(member);
        _members.sort((m1, m2) -> Short.compare(m1.memberId(), m2.memberId()));
    }

    /** 根据成员名称删除成员，如果是EntityRef成员同时删除相关隐藏成员 */
    public void removeMember(String memberName) {
        checkDesignMode();

        var m = getMember(memberName);
        //如果实体模型是新建的或成员是新建的直接移除
        if (persistentState() == PersistentState.Detached
                || m.persistentState() == PersistentState.Detached) {
            if (m.type() == EntityMemberType.EntityRef) {
                var refModel = (EntityRefModel)m;
                for(var fk : refModel.getFKMemberIds()) {
                    _members.remove(getMember(fk));
                }
                if (refModel.isAggregationRef())
                    _members.remove(getMember(refModel.typeMemberId()));
            }
            _members.remove(m);
            return;
        }

        //标为删除状态
        m.markDeleted();
        if (m.type() == EntityMemberType.EntityRef) {
            var refModel = (EntityRefModel)m;
            for(var fk : refModel.getFKMemberIds()) {
                getMember(fk).markDeleted();
            }
            if (refModel.isAggregationRef())
                getMember(refModel.typeMemberId()).markDeleted();
        }

        changeSchemaVersion();
        onPropertyChanged();
    }

    public void renameMember(String oldName, String newName) {
        checkDesignMode();

        if (oldName.equals(newName)) {
            Log.warn("Name is same");
            return;
        }

        final var m = getMember(oldName);
        m.renameTo(newName);
    }

    /**
     * 用于根据规则生成Sql表的名称, eg:相同前缀、命名规则等
     * @param original true表示设计时获取旧名称
     * @param ctx      null表示运行时
     */
    public String getSqlTableName(boolean original, /*IDesignContext*/Object ctx) {
        //TODO: 参考C#实现，暂简单返回名称
        return original ? originalName() : name();
    }
    //endregion

    //region ====Runtime Methods====

    /** 获取具有外键约束的EntityRefModel集合 */
    public List<EntityRefModel> getEntityRefsWithFKConstraint() {
        List<EntityRefModel> list = null;
        for (var m : _members) {
            if (m.type() == EntityMemberType.EntityRef) {
                var refModel = (EntityRefModel) m;
                if (!refModel.isReverse() && refModel.isForeignKeyConstraint()) {
                    if (list == null) list = new ArrayList<>();
                    list.add(refModel);
                }
            }
        }
        return list;
    }

    //endregion

    //region ====IComparable====
    public int compareTo(EntityModel other) {
        if (other == null)
            return 1;

        //判断当前对象有没有EntityRef引用成员至目标对象, 如果引用则大于other对象
        var refs = _members.stream()
                .filter(m -> m.type() == EntityMemberType.EntityRef)
                .collect(Collectors.toList());
        for (var m : refs) {
            var rm = (EntityRefModel) m;
            for (var refModelId : rm.getRefModelIds()) {
                if (refModelId == other._id) {
                    //注意：删除的需要倒过来排序
                    return other.persistentState() == PersistentState.Deleted ? -1 : 1;
                }
            }
        }

        return Long.compare(_id, other._id);
    }
    //endregion

    //region ====Serialization====
    private EntityMemberModel makeMemberByType(byte memberType) {
        if (memberType == EntityMemberType.DataField.value) {
            return new DataFieldModel(this);
        } else if (memberType == EntityMemberType.EntityRef.value) {
            return new EntityRefModel(this);
        } else if (memberType == EntityMemberType.EntitySet.value) {
            return new EntitySetModel(this);
        }
        throw new RuntimeException("Unknown EntityMember type: " + memberType);
    }

    private IEntityStoreOption makeStoreOptionsByType(byte type) {
        if (type == 1) {
            return new SysStoreOptions(this);
        } else if (type == 2) {
            return new SqlStoreOptions(this);
        }
        throw new RuntimeException("Unknown StoreOptions type: " + type);
    }

    @Override
    public void writeTo(IOutputStream bs) {
        super.writeTo(bs);

        //写入成员集合
        bs.writeVariant(1);
        bs.writeVariant(_members.size());
        for (EntityMemberModel member : _members) {
            //先写入类型
            bs.writeByte(member.type().value);
            //再写入成员
            member.writeTo(bs);
        }

        //写入存储选项
        if (_storeOptions != null) {
            bs.writeVariant(2);
            //先写入类型信息
            if (_storeOptions instanceof SysStoreOptions) {
                bs.writeByte((byte) 1);
            } else if (_storeOptions instanceof SqlStoreOptions) {
                bs.writeByte((byte) 2);
            } else {
                throw new RuntimeException("未实现");
            }
            _storeOptions.writeTo(bs);
        }

        if (designMode()) {
            bs.writeShortField(_devMemberIdSeq, 3);
            bs.writeShortField(_usrMemberIdSeq, 4);
        }

        bs.finishWriteFields();
    }

    @Override
    public void readFrom(IInputStream bs) {
        super.readFrom(bs);

        int propIndex;
        do {
            propIndex = bs.readVariant();
            switch (propIndex) {
                case 1: {
                    var count = bs.readVariant();
                    for (int i = 0; i < count; i++) {
                        var m = makeMemberByType(bs.readByte());
                        m.readFrom(bs);
                        _members.add(m);
                    }
                    //TODO:考虑强制重新排序
                    break;
                }
                case 2:
                    _storeOptions = makeStoreOptionsByType(bs.readByte());
                    _storeOptions.readFrom(bs);
                    break;
                case 3:
                    _devMemberIdSeq = bs.readShort();
                    break;
                case 4:
                    _usrMemberIdSeq = bs.readShort();
                    break;
                case 0:
                    break;
                default:
                    throw new RuntimeException("Unknown field id: " + propIndex);
            }
        } while (propIndex != 0);
    }

    @Override
    public void writeToJson(IJsonWriter writer) {
        writer.startObject();

        writer.writeKeyValue("IsNew", persistentState() == PersistentState.Detached);

        //写入成员列表,注意不向前端发送EntityRef的隐藏成员及标为删除的成员
        writer.writeKey("Members");
        var ms = _members.stream()
                .filter(t -> t.persistentState() != PersistentState.Deleted
                        && !(t instanceof DataFieldModel && ((DataFieldModel) t).isForeignKey()))
                .collect(Collectors.toList());
        writer.startArray();
        for (var m : ms) {
            m.writeToJson(writer);
        }
        writer.endArray();

        //写入存储选项
        if (_storeOptions != null) {
            writer.writeKey("StoreOptions");
            _storeOptions.writeToJson(writer);
        }

        writer.endObject();
    }
    //endregion

}
