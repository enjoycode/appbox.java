package appbox.serialization;

import appbox.data.Entity;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

/** 序列化时的上下文,主要管理循环引用 */
public final class SerializeContext {
    private List<Entity> _serializedList;

    public void addToSerialized(@Nonnull Entity obj) {
        if (_serializedList == null)
            _serializedList = new ArrayList<>();
        _serializedList.add(obj);
    }

    public int getSerializedIndex(@Nonnull Entity obj) {
        if (_serializedList == null || _serializedList.size() == 0)
            return -1;
        for (int i = _serializedList.size() - 1; i >= 0; i--) {
            if (_serializedList.get(i) == obj)
                return i;
        }
        return -1;
    }

}
