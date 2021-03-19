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
        registerType(SqlEntityKVO.class, customeSerializer);
        registerType(PermissionNode.class, customeSerializer);
        registerType(BlobObject.class, customeSerializer);
    }

    public static void registerType(Class<?> clazz, ObjectSerializer serializer) {
        config.put(clazz, serializer);
    }

    private final Object  result;
    private final boolean isRawJson;

    public JsonResult(Object result) {
        this.result    = result;
        this.isRawJson = false;
    }

    public JsonResult(Object result, boolean isRaw) {
        this.isRawJson = isRaw;
        this.result    = result;
    }

    @Override
    public void writeTo(IOutputStream bs) {
        if (result == null) {
            bs.write(JSON_NULL, 0, JSON_NULL.length);
            return;
        }

        if (isRawJson) {
            if (result instanceof byte[]) {
                var rawdata = (byte[]) result;
                bs.write(rawdata, 0, rawdata.length);
            } else if (result instanceof String) {
                bs.writeUtf8((String) result);
            } else {
                throw new RuntimeException("RawJson only support byte[] and String");
            }
        }

        try {
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
