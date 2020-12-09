package appbox.channel.messages;

import appbox.channel.KVRowReader;
import appbox.data.SysEntity;
import appbox.serialization.BinDeserializer;
import appbox.store.KeyUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public final class KVScanEntityResponse<T extends SysEntity> extends KVScanResponse {

    public        List<T>     result;
    private final Supplier<T> creator; //实例创建

    public KVScanEntityResponse(Supplier<T> creator) {
        this.creator = creator;
    }

    @Override
    public void readFrom(BinDeserializer bs) {
        reqId     = bs.readInt();
        errorCode = bs.readInt();
        checkStoreError();

        skipped = bs.readInt();
        length  = bs.readInt();

        result = new ArrayList<>(length);
        for (int i = 0; i < length; i++) {
            var keySize = bs.readNativeVariant(); //Row's key size
            assert keySize == KeyUtil.ENTITY_KEY_SIZE;
            //创建对象实例并从RowKey读取Id
            T obj = creator.get();
            result.add(obj);
            obj.id().readFrom(bs);
            //开始读取当前行的各个字段
            KVRowReader.readFields(bs, obj);
        }
    }

}
