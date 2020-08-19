package appbox.model.entity;

import appbox.data.PersistentState;
import appbox.serialization.BinDeserializer;
import appbox.serialization.BinSerializer;

import java.util.ArrayList;
import java.util.List;

/**
 * 系统存储的选项，包括分区与二级索引
 */
public final class SysStoreOptions implements IEntityStoreOption {
    private static final short MAX_INDEX_ID = 32; //2的5次方, 2bit Layer，1bit惟一标志

    private byte _devIndexIdSeq;
    private byte _usrIndexIdSeq;

    private boolean                  _isMVCC;        //是否MVCC存储格式
    private PartitionKey[]           _partitionKeys; //null表示不分区
    private ArrayList<SysIndexModel> _indexes;
    private boolean                  _orderByDesc;   //主键是否按时间倒序
    private boolean                  _hasChangedSchema; //不用序列化
    private int                      _oldSchemaVersion;
    private int                      _schemaVersion;

    //region ====Properties====
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
    public void writeTo(BinSerializer bs) throws Exception {

    }

    @Override
    public void readFrom(BinDeserializer bs) throws Exception {

    }
    //endregion
}
