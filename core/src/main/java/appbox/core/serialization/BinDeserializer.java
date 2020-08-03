package appbox.core.serialization;

import appbox.core.cache.ObjectPool;

public final class BinDeserializer {

    public static final ObjectPool<BinDeserializer> pool = new ObjectPool<>(BinDeserializer::new, null, 32);

    private BinDeserializer() {
    }

    private IInputStream _stream;

    public void reset(IInputStream stream) {
        _stream = stream;
    }

    public short readShort() throws Exception {
        return _stream.readShort();
    }
}
