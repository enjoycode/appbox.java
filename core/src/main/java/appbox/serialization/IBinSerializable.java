package appbox.serialization;

import java.io.ByteArrayOutputStream;

public interface IBinSerializable {
    void writeTo(IOutputStream bs);

    void readFrom(BinDeserializer bs);

    static ByteArrayOutputStream serializeTo(Object obj, boolean compress) {
        var output = new BytesOutputStream(1024);
        output.serialize(obj);
        return output;
    }

    static byte[] serialize(IBinSerializable obj, boolean compress) {
        return serializeTo(obj, compress).toByteArray();
    }
}
