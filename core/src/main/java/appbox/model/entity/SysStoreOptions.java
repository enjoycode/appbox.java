package appbox.model.entity;

import appbox.data.PersistentState;
import appbox.model.EntityModel;
import appbox.serialization.IInputStream;
import appbox.serialization.IJsonWriter;
import appbox.serialization.IOutputStream;
import appbox.utils.IdUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * 系统存储的选项，包括分区与二级索引
 */
public final class SysStoreOptions implements IEntityStoreOption {
    private static final short MAX_INDEX_ID = 32; //2的5次方, 2bit Layer，1bit惟一标志

    private byte _devIndexIdSeq;
    private byte _usrIndexIdSeq;

    protected final EntityModel owner;              //不要序列化

    private boolean             _isMVCC;        //是否MVCC存储格式
    private PartitionKey[]      _partitionKeys; //null表示不分区
    private List<SysIndexModel> _indexes;
    private boolean             _orderByDesc;   //主键是否按时间倒序
    private boolean             _hasChangedSchema; //不用序列化
    private int                 _oldSchemaVersion;
    private int                 _schemaVersion;

    /**
     * Only for serialization
     */
    public SysStoreOptions(EntityModel owner) {
        this.owner = owner;
    }

    public SysStoreOptions(EntityModel owner, boolean mvcc, boolean orderByDesc) {
        this.owner   = owner;
        _isMVCC      = mvcc;
        _orderByDesc = orderByDesc;
    }

    //region ====Properties====
    public int schemaVersion() {
        return _schemaVersion;
    }

    public int oldSchemaVersion() { return _oldSchemaVersion; }

    public boolean hasSchemaChanged() { return _hasChangedSchema; }

    public boolean isMVCC() { return _isMVCC; }

    public boolean hasPartitionKeys() {
        return _partitionKeys != null && _partitionKeys.length > 0;
    }

    @Override
    public boolean hasIndexes() {
        return _indexes != null && _indexes.size() > 0;
    }

    @Override
    public List<SysIndexModel> getIndexes() {
        if (_indexes == null) {
            _indexes = new ArrayList<>();
        }
        return _indexes;
    }
    //endregion

    //region ====Design Methods====
    public boolean isPartitionKey(short memberId) {
        if (hasPartitionKeys()) {
            for (PartitionKey partitionKey : _partitionKeys) {
                if (partitionKey.memberId == memberId) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void acceptChanges() {
        if (hasIndexes()) {
            for (int i = _indexes.size() - 1; i >= 0; i--) {
                if (_indexes.get(i).persistentState() == PersistentState.Deleted) {
                    _indexes.remove(i);
                } else {
                    _indexes.get(i).acceptChanges();
                }
            }
        }

        _hasChangedSchema = false;
        _oldSchemaVersion = 0;
    }

    public void changeSchemaVersion() {
        if (_hasChangedSchema) {
            return;
        }

        _oldSchemaVersion = _schemaVersion;
        _schemaVersion += 1;
    }

    /**
     * only for StoreInitiator
     */
    public void addSysIndex(EntityModel owner, SysIndexModel index, byte id) {
        owner.checkDesignMode();
        index.canAddTo(owner);

        index.initIndexId(id);
        getIndexes().add(index);
    }
    //endregion

    //region ====Store Flags====

    /**
     * 2bit RaftType + 1bit MvccFlag + 1bit OrderFlag
     */
    public byte tableFlags() {
        return (byte) (IdUtil.RAFT_TYPE_TABLE << IdUtil.RAFTGROUPID_FLAGS_TYPE_OFFSET
                | (_isMVCC ? 1 : 0) << IdUtil.RAFTGROUPID_FLAGS_MVCC_OFFSET
                | (_orderByDesc ? 1 : 0));
    }

    /**
     * 2bit RaftType + 1bit MvccFlag + 1bit OrderFlag(无用）
     */
    public byte indexFlags() {
        return (byte) (IdUtil.RAFT_TYPE_INDEX << IdUtil.RAFTGROUPID_FLAGS_TYPE_OFFSET
                | (_isMVCC ? 1 : 0) << IdUtil.RAFTGROUPID_FLAGS_MVCC_OFFSET);
    }
    //endregion

    //region ====Serialization====
    @Override
    public void writeTo(IOutputStream bs) {
        bs.writeBoolField(_isMVCC, 1);
        bs.writeBoolField(_orderByDesc, 2);
        bs.writeIntField(_schemaVersion, 3);

        //写入索引集合
        if (hasIndexes()) {
            bs.writeList(_indexes, 4, false);
        }

        //写入分区键
        if (hasPartitionKeys()) {
            bs.writeArray(_partitionKeys, 5, false);
        }

        bs.writeByteField(_devIndexIdSeq, 6);
        bs.writeByteField(_usrIndexIdSeq, 7);
        if (_hasChangedSchema) {
            bs.writeIntField(_oldSchemaVersion, 8);
        }

        bs.finishWriteFields();
    }

    @Override
    public void readFrom(IInputStream bs) {
        int propIndex;
        do {
            propIndex = bs.readVariant();
            switch (propIndex) {
                case 1:
                    _isMVCC = bs.readBool();
                    break;
                case 2:
                    _orderByDesc = bs.readBool();
                    break;
                case 3:
                    _schemaVersion = bs.readInt();
                    break;
                case 4:
                    _indexes = bs.readList(() -> new SysIndexModel(owner), false);
                    break;
                case 5:
                    _partitionKeys = bs.readArray(PartitionKey[]::new, PartitionKey::new, false);
                    break;
                case 6:
                    _devIndexIdSeq = bs.readByte();
                    break;
                case 7:
                    _usrIndexIdSeq = bs.readByte();
                    break;
                case 8:
                    _hasChangedSchema = true;
                    _oldSchemaVersion = bs.readInt();
                    break;
                case 0:
                    break;
                default:
                    throw new RuntimeException("Unknown field id:" + propIndex);
            }
        } while (propIndex != 0);
    }

    @Override
    public void writeToJson(IJsonWriter writer) {
        writer.startObject();

        writer.writeKey("OrderByDesc");
        writer.writeValue(_orderByDesc);

        //写入索引
        writer.writeKey("Indexes");
        writer.startArray();
        if (hasIndexes()) {
            //TODO:
        }
        writer.endArray();

        //写入分区键列表
        writer.writeKey("PartitionKeys");
        writer.startArray();
        //TODO:
        writer.endArray();

        writer.endObject();
    }
    //endregion
}
