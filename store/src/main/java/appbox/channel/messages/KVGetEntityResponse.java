package appbox.channel.messages;

import appbox.channel.KVRowReader;
import appbox.data.EntityId;
import appbox.data.SysEntity;
import appbox.serialization.BinDeserializer;

public final class KVGetEntityResponse<T extends SysEntity> extends KVGetResponse {

    private final Class<T> clazz;
    private final EntityId id;
    private       T        entity;

    public KVGetEntityResponse(Class<T> clazz, EntityId id) {
        this.clazz = clazz;
        this.id    = id;
    }

    public T getEntity() { return entity; }

    @Override
    public void readFrom(BinDeserializer bs) {
        reqId     = bs.readInt();
        errorCode = bs.readInt();
        checkStoreError();

        if (bs.hasRemaining()) {
            //创建实体对象实例
            try {
                entity = clazz.getDeclaredConstructor().newInstance();
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
            //复制EntityId
            entity.id().copyFrom(id);
            //读取实体字段
            KVRowReader.readFields(bs, entity);
        }
    }
}
