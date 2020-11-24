package appbox.channel.messages;

import appbox.channel.KVRowReader;
import appbox.data.SysEntity;
import appbox.serialization.BinDeserializer;

public final class KVGetEntityResponse<T extends SysEntity> extends KVGetResponse {

    private final Class<T> clazz;
    private T entity;

    public KVGetEntityResponse(Class<T> clazz) {
        this.clazz = clazz;
    }

    public T getEntity() { return entity; }

    @Override
    public void readFrom(BinDeserializer bs) {
        reqId     = bs.readInt();
        errorCode = bs.readInt();

        if (errorCode == 0 && bs.hasRemaining()) {
            //创建实体对象实例
            try {
                entity = clazz.getDeclaredConstructor().newInstance();
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
            //读取实体字段
            KVRowReader.readFields(bs, entity);
        }
    }
}
