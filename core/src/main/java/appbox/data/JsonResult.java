package appbox.data;

import appbox.serialization.BinDeserializer;
import appbox.serialization.BinSerializer;
import appbox.serialization.IBinSerializable;
import com.alibaba.fastjson.JSON;

/**
 * 用于包装服务端返回给前端的Json序列化后的结果
 */
public final class JsonResult implements IBinSerializable {
    private final Object result;

    public JsonResult(Object result) {
        this.result = result;
    }

    @Override
    public void writeTo(BinSerializer bs) throws Exception {
        //直接写Json
        JSON.writeJSONString(bs, result);
    }

    @Override
    public void readFrom(BinDeserializer bs) throws Exception {
        throw new RuntimeException("Not supported.");
    }
}
