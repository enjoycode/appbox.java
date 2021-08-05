package appbox.serialization;

import appbox.data.Entity;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/** 反序列化时的上下文,主要管理循环引用及实体创建 */
public final class DeserializeContext {
    private List<Entity>                          _deserialized;
    private Map<Long, Supplier<? extends Entity>> _entityFactory;

    public void addToDeserialized(@Nonnull Entity obj) {
        if (_deserialized == null)
            _deserialized = new ArrayList<>();
        _deserialized.add(obj);
    }

    public Entity getDeserialized(int index) {
        return _deserialized.get(index);
    }

    public void setEntityFactory(Map<Long, Supplier<? extends Entity>> factory) {
        _entityFactory = factory;
    }

    public Supplier<? extends Entity> getEntityFactory(long modelId) {
        if (_entityFactory == null) return null;
        return _entityFactory.get(modelId);
    }
}
