package appbox.serialization;

import java.io.IOException;
import java.io.Writer;

import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.serializer.JSONSerializer;
import com.alibaba.fastjson.serializer.SerializeWriter;
import com.alibaba.fastjson.serializer.SerializerFeature;

public final class FastJsonWriter implements IJsonWriter {
    //region ====JSONStreamContext====
    static class JSONStreamContext {

        final static int StartObject   = 1001;
        final static int PropertyKey   = 1002;
        final static int PropertyValue = 1003;
        final static int StartArray    = 1004;
        final static int ArrayValue    = 1005;

        protected final JSONStreamContext parent;

        protected int state;

        public JSONStreamContext(JSONStreamContext parent, int state) {
            this.parent = parent;
            this.state  = state;
        }
    }
    //endregion

    private final SerializeWriter   writer;
    private final JSONSerializer    serializer;
    private       JSONStreamContext context;

    public FastJsonWriter(JSONSerializer serializer) {
        this.writer     = serializer.getWriter();
        this.serializer = serializer;
    }

    public FastJsonWriter(Writer out) {
        writer     = new SerializeWriter(out);
        serializer = new JSONSerializer(writer);
    }

    public void config(SerializerFeature feature, boolean state) {
        this.writer.config(feature, state);
    }

    public void startObject() {
        if (context != null) {
            beginStructure();
        }
        context = new JSONStreamContext(context, JSONStreamContext.StartObject);
        writer.write('{');
    }

    public void endObject() {
        writer.write('}');
        endStructure();
    }

    public void writeKey(String key) {
        writeObject(key);
    }

    public void writeValue(Object object) {
        writeObject(object);
    }

    public void writeObject(String object) {
        beforeWrite();

        serializer.write(object);

        afterWrite();
    }

    public void writeObject(Object object) {
        beforeWrite();
        serializer.write(object);
        afterWrite();
    }

    public void startArray() {
        if (context != null) {
            beginStructure();
        }

        context = new JSONStreamContext(context, JSONStreamContext.StartArray);
        writer.write('[');
    }

    private void beginStructure() {
        final int state = context.state;
        switch (context.state) {
            case JSONStreamContext.PropertyKey:
                writer.write(':');
                break;
            case JSONStreamContext.ArrayValue:
                writer.write(',');
                break;
            case JSONStreamContext.StartObject:
                break;
            case JSONStreamContext.StartArray:
                break;
            default:
                throw new JSONException("illegal state : " + state);
        }
    }

    public void endArray() {
        writer.write(']');
        endStructure();
    }

    private void endStructure() {
        context = context.parent;

        if (context == null) {
            return;
        }

        int newState = -1;
        switch (context.state) {
            case JSONStreamContext.PropertyKey:
                newState = JSONStreamContext.PropertyValue;
                break;
            case JSONStreamContext.StartArray:
                newState = JSONStreamContext.ArrayValue;
                break;
            case JSONStreamContext.ArrayValue:
                break;
            case JSONStreamContext.StartObject:
                newState = JSONStreamContext.PropertyKey;
                break;
            default:
                break;
        }
        if (newState != -1) {
            context.state = newState;
        }
    }

    private void beforeWrite() {
        if (context == null) {
            return;
        }

        switch (context.state) {
            case JSONStreamContext.StartObject:
            case JSONStreamContext.StartArray:
                break;
            case JSONStreamContext.PropertyKey:
                writer.write(':');
                break;
            case JSONStreamContext.PropertyValue:
                writer.write(',');
                break;
            case JSONStreamContext.ArrayValue:
                writer.write(',');
                break;
            default:
                break;
        }
    }

    private void afterWrite() {
        if (context == null) {
            return;
        }

        final int state    = context.state;
        int       newState = -1;
        switch (state) {
            case JSONStreamContext.PropertyKey:
                newState = JSONStreamContext.PropertyValue;
                break;
            case JSONStreamContext.StartObject:
            case JSONStreamContext.PropertyValue:
                newState = JSONStreamContext.PropertyKey;
                break;
            case JSONStreamContext.StartArray:
                newState = JSONStreamContext.ArrayValue;
                break;
            case JSONStreamContext.ArrayValue:
                break;
            default:
                break;
        }

        if (newState != -1) {
            context.state = newState;
        }
    }

    public void flush() throws IOException {
        writer.flush();
    }

    public void close() throws IOException {
        writer.close();
    }

}
