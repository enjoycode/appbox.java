import appbox.channel.messages.InvokeRequire;
import appbox.serialization.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestMsgSerialization {

    @Test
    public void testSerializationOfInvokeRequire() {
        var src = InvokeRequire.rentFromPool();
        src.shard   = 1;
        src.service = "sys.OrderService.SayHello";
        src.addArg(12345);

        var output = new BytesOutputStream(8192);
        src.writeTo(output); //直接写

        var input = output.copyToInput();
        var dst   = InvokeRequire.rentFromPool();
        dst.readFrom(input); //直接读
        assertEquals(src.shard, dst.shard);
        assertEquals(src.service, dst.service);
        assertEquals(src.getArgsCount(), dst.getArgsCount());
        assertEquals(src.getArg(0).getInt(), dst.getArg(0).getInt());

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
        req.service   = "sys.TestService.sayHello";
        req.addArg("Future");

        var output = new BytesOutputStream(8192);
        req.writeTo(output);

        output.saveToFile(10/*偏移shard + sessionId*/, "src/test/java/InvokeRequire.bin");
    }
}
