package appbox.data;

import appbox.serialization.*;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.ObjectSerializer;
import com.alibaba.fastjson.serializer.SerializeConfig;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

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
        registerType(SysEntityKVO.class);
        registerType(SqlEntityKVO.class);
        registerType(PermissionNode.class);
        registerType(BlobObject.class);
    }

    private static void registerType(Class<? extends IJsonSerializable> clazz) {
        config.put(clazz, customeSerializer);
    }

    public static void registerType(Class<?> clazz, ObjectSerializer serializer) {
        config.put(clazz, serializer);
    }

    private final Object result;
    private final int    resultType;

    public JsonResult(Object result) {
        this.result     = result;
        this.resultType = 0;
    }

    public JsonResult(Object result, boolean isRaw) {
        this.result     = result;
        this.resultType = 1;
    }

    public JsonResult(List<? extends IJsonSerializable> result) {
        this.result     = result;
        this.resultType = 2;
    }

    @Override
    public void writeTo(IOutputStream bs) {
        if (result == null) {
            bs.write(JSON_NULL, 0, JSON_NULL.length);
            return;
        }

        if (resultType == 1) {
            if (result instanceof byte[]) {
                var rawdata = (byte[]) result;
                bs.write(rawdata, 0, rawdata.length);
            } else if (result instanceof String) {
                bs.writeUtf8((String) result);
            } else {
                throw new RuntimeException("RawJson only support byte[] and String");
            }
            return;
        }

        try {
            if (resultType == 2) {
                final var list       = (List<? extends IJsonSerializable>) result;
                final var out        = new OutputStreamWriter((OutputStream) bs);
                final var jsonWriter = new FastJsonWriter(out);
                jsonWriter.startArray();
                for (var item : list) {
                    item.writeToJson(jsonWriter);
                }
                jsonWriter.endArray();
                jsonWriter.close();
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
