package appbox.model.entity;

import appbox.data.PersistentState;
import appbox.model.EntityModel;
import appbox.serialization.BinDeserializer;
import appbox.serialization.BinSerializer;
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

    private boolean                  _isMVCC;        //是否MVCC存储格式
    private PartitionKey[]           _partitionKeys; //null表示不分区
    private ArrayList<SysIndexModel> _indexes;
    private boolean                  _orderByDesc;   //主键是否按时间倒序
    private boolean                  _hasChangedSchema; //不用序列化
    private int                      _oldSchemaVersion;
    private int                      _schemaVersion;

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

    //region ====Serialization====
    @Override
    public void writeTo(BinSerializer bs) {
        bs.writeBool(_isMVCC, 1);
        bs.writeBool(_orderByDesc, 2);
        bs.writeInt(_schemaVersion, 3);

        //写入索引集合
        if (hasIndexes()) {
            bs.writeVariant(4);
            bs.writeVariant(_indexes.size());
            for (SysIndexModel index : _indexes) {
                index.writeTo(bs);
            }
        }

        //写入分区键
        if (hasPartitionKeys()) {
            bs.writeVariant(5);
            bs.writeVariant(_partitionKeys.length);
            for (PartitionKey partitionKey : _partitionKeys) {
                partitionKey.writeTo(bs);
            }
        }

        bs.writeByte(_devIndexIdSeq, 6);
        bs.writeByte(_usrIndexIdSeq, 7);
        if (_hasChangedSchema) {
            bs.writeInt(_oldSchemaVersion, 8);
        }

        bs.finishWriteFields();
    }

    @Override
    public void readFrom(BinDeserializer bs) {
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
                case 4: {
                    var count = bs.readVariant();
                    for (int i = 0; i < count; i++) {
                        var index = new SysIndexModel(owner);
                        index.readFrom(bs);
                        getIndexes().add(index);
                    }
                    break;
                }
                case 5: {
                    var count = bs.readVariant();
                    _partitionKeys = new PartitionKey[count];
                    for (int i = 0; i < count; i++) {
                        _partitionKeys[i] = new PartitionKey();
                        _partitionKeys[i].readFrom(bs);
                    }
                    break;
                }
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
    //endregion
}
