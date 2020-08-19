package appbox.cache;

import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.function.*;

/**
 * A lock-free object pool.
 */
public final class ObjectPool<E> {

    private final Supplier<E>             _factory;
    //Storage for the pool objects. The first item is stored in a dedicated field because we
    //expect to be able to satisfy most requests from it.
    private final AtomicReference<E>      _firstItem;
    private final AtomicReferenceArray<E> _items;

    public ObjectPool(Supplier<E> factory, int cap) {
        //TODO:注册监测指标(生产次数)
        _factory   = factory;
        _firstItem = new AtomicReference<>(_factory.get());
        _items     = new AtomicReferenceArray<>(cap - 1);
    }

    public E rent() {
        /// Search strategy is a simple linear probing which is chosen for it cache-friendliness.
        /// Note that Free will try to store recycled objects close to the start thus statistically
        /// reducing how far we will typically search.

        // PERF: Examine the first element. If that fails, AllocateSlow will look at the remaining elements.
        // Note that the initial read is optimistically not synchronized. That is intentional.
        // We will interlock only when we have a candidate. in a worst case we may miss some
        // recently returned objects. Not a big deal.
        E inst = _firstItem.get();
        if (inst == null || inst != _firstItem.compareAndExchange(inst, null)) {
            inst = allocateSlow();
        }
        return inst;
    }

    private E allocateSlow() {
        var items = _items;
        for (int i = 0; i < items.length(); i++) {
            // Note that the initial read is optimistically not synchronized. That is intentional.
            // We will interlock only when we have a candidate. in a worst case we may miss some
            // recently returned objects. Not a big deal.
            E inst = items.get(i);
            if (inst != null) {
                if (inst == items.compareAndExchange(i, inst, null)) {
                    return inst;
                }
            }
        }
        return _factory.get();
    }

    public void back(E e) {
        /// Search strategy is a simple linear probing which is chosen for it cache-friendliness.
        /// Note that Free will try to store recycled objects close to the start thus statistically
        /// reducing how far we will typically search in Allocate.

        if (_firstItem.get() == null) {
            // Intentionally not using interlocked here.
            // In a worst case scenario two objects may be stored into same slot.
            // It is very unlikely to happen and will only mean that one of the objects will get collected.
            _firstItem.set(e);
        } else {
            freeSlow(e);
        }
    }

    private void freeSlow(E obj) {
        var items = _items;
        for (int i = 0; i < items.length(); i++) {
            if (items.get(i) == null) {
                // Intentionally not using interlocked here.
                // In a worst case scenario two objects may be stored into same slot.
                // It is very unlikely to happen and will only mean that one of the objects will get collected.
                items.set(i, obj);
                break;
            }
        }
    }
}