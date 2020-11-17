package appbox.channel.messages;

import appbox.channel.KVRowReader;
import appbox.data.IKVRow;
import appbox.serialization.BinDeserializer;

public final class KVGetIndexResponse<T extends IKVRow> extends KVGetResponse {

    private final Class<T> _indexClass;
    private       T        _indexRow;

    public KVGetIndexResponse(Class<T> indexClass) {
        _indexClass = indexClass;
    }

    public T getRow() { return _indexRow; }

    @Override
    public void readFrom(BinDeserializer bs) throws Exception {
        reqId     = bs.readInt();
        errorCode = bs.readInt();

        if (errorCode == 0 && bs.hasRemaining()) {
            //创建索引对象实例
            _indexRow = _indexClass.getDeclaredConstructor().newInstance();
            //读取索引字段
            KVRowReader.readFields(bs, _indexRow);
        }
    }
}
