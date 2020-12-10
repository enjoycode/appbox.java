package appbox.channel.messages;

import appbox.channel.KVRowReader;
import appbox.data.IKVRow;
import appbox.serialization.IInputStream;

public final class KVGetIndexResponse<T extends IKVRow> extends KVGetResponse {

    private final Class<T> _indexClass;
    private       T        _indexRow;

    public KVGetIndexResponse(Class<T> indexClass) {
        _indexClass = indexClass;
    }

    public T getRow() { return _indexRow; }

    @Override
    public void readFrom(IInputStream bs) {
        reqId     = bs.readInt();
        errorCode = bs.readInt();
        checkStoreError();

        if (bs.hasRemaining()) {
            //创建索引对象实例
            try {
                _indexRow = _indexClass.getDeclaredConstructor().newInstance();
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
            //读取索引字段
            KVRowReader.readFields(bs, _indexRow);
        }
    }
}
