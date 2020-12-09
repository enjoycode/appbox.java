package appbox.serialization;

import java.io.Closeable;
import java.io.Flushable;

public interface IJsonWriter extends Closeable, Flushable {

    void startArray();

    void endArray();

    void startObject();

    void endObject();

    void writeKey(String key);

    void writeValue(Object object);

    default void writeKeyValue(String key, Object value) {
        writeKey(key);
        writeValue(value);
    }

}
