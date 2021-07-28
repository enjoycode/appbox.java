package appbox.compression;

public enum CompressType {
    None((byte) 0),
    Brotli((byte) 1),
    GZip((byte) 2);

    public final byte value;

    CompressType(byte v) {
        value = v;
    }
}
