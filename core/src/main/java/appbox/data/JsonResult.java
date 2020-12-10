package appbox.data;

import appbox.serialization.*;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.ObjectSerializer;
import com.alibaba.fastjson.serializer.SerializeConfig;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;

/**
 * 用于包装服务端返回给前端的Json序列化后的结果
 */
public final class JsonResult implements IBinSerializable {
    private static final SerializeConfig  config            = new SerializeConfig();
    private static final byte[]           JSON_NULL         = "null".getBytes(StandardCharsets.UTF_8);
    private static final ObjectSerializer customeSerializer = (serializer, object, fieldName, fieldType, features) -> {
        var jsonWriter = new FastJsonWriter(serializer);
        var instance   = (IJsonSerializable) object;
        instance.writeToJson(jsonWriter);
        //jsonWriter.close(); //Donot close
    };


    static {
        registerType(SysEntityKVO.class, customeSerializer);
    }

    public static void registerType(Class<?> clazz, ObjectSerializer serializer) {
        config.put(clazz, serializer);
    }

    private final Object result;

    public JsonResult(Object result) {
        this.result = result;
    }

    @Override
    public void writeTo(IOutputStream bs) {
        try {
            if (result == null) {
                bs.write(JSON_NULL, 0, JSON_NULL.length);
                return;
            }

            //TODO:fast for primitive types

            //fast for IJSonSerializable
            if (result instanceof IJsonSerializable) {
                var out        = new OutputStreamWriter((OutputStream) bs);
                var jsonWriter = new FastJsonWriter(out);
                ((IJsonSerializable) result).writeToJson(jsonWriter);
                jsonWriter.close();
                return;
            }

            JSON.writeJSONString((OutputStream) bs, StandardCharsets.UTF_8, result,
                    config, null, null, JSON.DEFAULT_GENERATE_FEATURE);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public void readFrom(IInputStream bs) {
        throw new UnsupportedOperationException();
    }
}
