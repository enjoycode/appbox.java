package appbox.core.model.entity;

import appbox.core.model.EntityModel;
import appbox.core.serialization.IBinSerializable;

import java.util.List;

/**
 * Entity映射的目标存储的相关选项，如索引等
 */
public interface IEntityStoreOption extends IBinSerializable {
    enum StoreType {
        SysStore, SqlStore, CqlStore;
    }

    StoreType storeType();

    boolean hasIndexes();

    List<? extends IndexModelBase> getIndexes();

    void acceptChanges();

    //void importFrom(EntityModel owner);

    //void updateFrom(EntityModel owner, IEntityStoreOption other);
}
