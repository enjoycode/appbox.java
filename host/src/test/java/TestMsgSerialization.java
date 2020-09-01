import appbox.channel.messages.InvokeRequire;
import appbox.serialization.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestMsgSerialization {

    @Test
    public void testSerializationOfInvokeRequire() throws Exception {
        var src = InvokeRequire.rentFromPool();
        src.shard   = 1;
        src.service = "sys.OrderService.SayHello";
        src.addArg(12345);

        var output = new BytesOutputStream(8192);
        var os     = BinSerializer.rentFromPool(output);
        src.writeTo(os); //直接写
        BinSerializer.backToPool(os);

        var input = output.copyToInput();
        var is    = BinDeserializer.rentFromPool(input);
        var dst   = InvokeRequire.rentFromPool();
        dst.readFrom(is); //直接读
        assertEquals(src.shard, dst.shard);
        assertEquals(src.service, dst.service);
        assertEquals(src.getArgsCount(), dst.getArgsCount());
        assertEquals(src.getArg(0).getInt(), dst.getArg(0).getInt());
        BinDeserializer.backToPool(is);

        InvokeRequire.backToPool(src);
        InvokeRequire.backToPool(dst);
    }

    /**
     * 保存客户端请求至文件方便Post测试
     */
    @Test
    public void saveClientInvokeRequire() throws Exception {
        var req = InvokeRequire.rentFromPool();
        req.shard     = 0;
        req.sessionId = 0;
        req.service   = "sys.OrderService.SayHello";
        req.addArg(12345);

        var output = new BytesOutputStream(8192);
        var os     = BinSerializer.rentFromPool(output);
        req.writeTo(os);
        BinSerializer.backToPool(os);

        output.saveToFile(10/*偏移shard + sessionId*/, "src/test/java/InvokeRequire.bin");
    }
}
