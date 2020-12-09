package appbox.data;

import appbox.serialization.BinDeserializer;
import appbox.serialization.BinSerializer;
import appbox.serialization.IBinSerializable;
import appbox.serialization.IJsonSerializable;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONWriter;
import com.alibaba.fastjson.serializer.ObjectSerializer;
import com.alibaba.fastjson.serializer.SerializeConfig;

import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;

/**
 * 用于包装服务端返回给前端的Json序列化后的结果
 */
public final class JsonResult implements IBinSerializable {
    private static final SerializeConfig config    = new SerializeConfig();
    private static final byte[]          JSON_NULL = "null".getBytes(StandardCharsets.UTF_8);

    static {
        //TODO:待重新实现IJsonSerializable接口及自定义JSONWriter
        registerType(SysEntityKVO.class, (serializer, object, fieldName, fieldType, features) -> {
            var jsonWriter = new JSONWriter(serializer.getWriter());
            var instance   = (IJsonSerializable) object;
            instance.writeToJson(jsonWriter);
            jsonWriter.close();
        });
    }

    public static void registerType(Class<?> clazz, ObjectSerializer serializer) {
        config.put(clazz, serializer);
    }

    private final Object result;

    public JsonResult(Object result) {
        this.result = result;
    }

    @Override
    public void writeTo(BinSerializer bs) {
        try {
            if (result == null) {
                bs.write(JSON_NULL, 0, JSON_NULL.length);
                return;
            }

            //TODO:fast for primitive types

            //直接写Json
            if (result instanceof IJsonSerializable) {
                var out        = new OutputStreamWriter(bs);
                var jsonWriter = new JSONWriter(out);
                ((IJsonSerializable) result).writeToJson(jsonWriter);
                jsonWriter.close();
                return;
            }

            JSON.writeJSONString(bs, StandardCharsets.UTF_8, result,
                    config, null, null, JSON.DEFAULT_GENERATE_FEATURE);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public void readFrom(BinDeserializer bs) {
        throw new UnsupportedOperationException();
    }
}
