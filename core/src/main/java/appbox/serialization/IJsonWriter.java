package appbox.serialization;

import java.io.Closeable;
import java.io.Flushable;
import java.util.List;

public interface IJsonWriter extends Closeable, Flushable {

    void startArray();

    void endArray();

    void startObject();

    void endObject();

    void writeKey(String key);

    void writeValue(Object object);

    void writeBase64Data(byte[] data);

    default void writeKeyValue(String key, Object value) {
        writeKey(key);
        writeValue(value);
    }

    default <T extends IJsonSerializable> void writeArray(T[] array) {
        startArray();
        for (T t : array) {
            t.writeToJson(this);
        }
        endArray();
    }

    default <T extends IJsonSerializable> void writeList(List<T> list) {
        startArray();
        for (T t : list) {
            t.writeToJson(this);
        }
        endArray();
    }

    default void writeEmptyArray() {
        startArray();
        endArray();
    }

}
