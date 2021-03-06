package appbox.cache;

/** 托管的字节缓存块 */
public final class BytesSegment {
    public static final int FRAME_SIZE = 216; //等于MessageChunk的数据部分大小

    //region ====ObjectPool====
    private static final ObjectPool<BytesSegment> pool = new ObjectPool<>(BytesSegment::new, 16);

    public static BytesSegment rent() {
        var obj = pool.rent();
        obj._first = obj;
        return obj;
    }

    public static BytesSegment rent(BytesSegment prev) {
        if (prev == null)
            throw new IllegalArgumentException("prev is null");
        var obj = pool.rent();
        obj._first = prev._first;
        prev._next = obj;
        return obj;
    }

    public static void backOne(BytesSegment one) {
        if (one == null)
            return;
        one._first = null;
        one._next  = null;
        pool.back(one);
    }

    public static void backAll(BytesSegment item) {
        if (item == null)
            return;
        var          current = item._first;
        BytesSegment next;
        while (current != null) {
            next = current._next;
            backOne(current);
            current = next;
        }
    }
    //endregion

    public final byte[]       buffer;
    private      BytesSegment _first;
    private      BytesSegment _next;
    private      int          _dataSize;

    private BytesSegment() {
        buffer = new byte[FRAME_SIZE];
        _first = this;
        _next  = null;
    }

    public BytesSegment first() {
        return _first;
    }

    public BytesSegment next() { return _next; }

    public int getDataSize() {
        return _dataSize;
    }

    public void setDataSize(int size) {
        if (size < 0 || size > FRAME_SIZE)
            throw new IllegalArgumentException();
        _dataSize = size;
    }

}
