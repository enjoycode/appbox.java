package appbox.serialization;

import java.io.ByteArrayOutputStream;

public interface IBinSerializable {
    void writeTo(IOutputStream bs);

    void readFrom(IInputStream bs);

    static ByteArrayOutputStream serializeTo(Object obj, boolean compress) {
        var output = new BytesOutputStream(1024);
        output.serialize(obj);
        return output;
    }

    static byte[] serialize(IBinSerializable obj, boolean compress) {
        return serializeTo(obj, compress).toByteArray();
    }

    static Object deserialize(byte[] data) {
        var stream = new BytesInputStream(data);
        return stream.deserialize();
    }

}
