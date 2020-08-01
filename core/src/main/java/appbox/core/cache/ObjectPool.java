package appbox.core.cache;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.function.*;

/**
 * A lock-free object pool.
 */
public final class ObjectPool<E> {

    private final Supplier<E> alloc;

    private final Consumer<E> free;

    /**
     * The cached object stack.
     */
    private final AtomicReferenceArray<E> objects;
    /**
     * The index in {@link #objects} of the first empty element.
     */
    private final AtomicInteger top = new AtomicInteger(0);


    /**
     * Constructor.
     *
     * @param cap the maximum number of objects cached.
     */
    public ObjectPool(Supplier<E> alloc, Consumer<E> free, int cap) {
        this.alloc = alloc;
        this.free = free;
        this.objects = new AtomicReferenceArray<>(cap);
    }

    public E rent() {
        while (true) {
            // Try reserve a cached object in objects
            int n;
            do {
                n = top.get();
                if (n == 0) {
                    // No cached objects, allocate a new one
                    return alloc.get();
                }
            } while (!top.compareAndSet(n, n - 1));
            // Try fetch the cached object
            E e = objects.getAndSet(n, null);
            if (e != null) {
                return e;
            }
            // It is possible that the reserved object was extracted before
            // the current thread tried to get it. Let's start over again.
        }
    }

    public void back(E e) {
        while (true) {
            // Try reserve a place in this.objects for e.
            int n;
            do {
                n = top.get();
                if (n == objects.length()) {
                    // the pool is full, e is not cached.
                    if (free != null) {
                        free.accept(e);
                    }
                }
            } while (!top.compareAndSet(n, n + 1));
            // Try put e at the reserved place.
            if (objects.compareAndSet(n + 1, null, e)) {
                return;
            }
            // It is possible that the reserved place was occupied before
            // the current thread tried to put e in it. Let's start over again.
        }
    }
}