import appbox.channel.messages.InvokeRequire;
import appbox.core.serialization.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestSerialization {

    @Test
    public void testSerializationOfInvokeRequire() throws Exception {
        var src = InvokeRequire.pool.rent();
        src.shard = 1;
        src.service = "sys.OrderService.SayHello";
        src.addArg(12345);

        var output = new BytesOutputStream(8192);
        var os     = BinSerializer.rentFromPool(output);
        os.serialize(src);
        BinSerializer.backToPool(os);

        var input = output.copyTo();
        var is    = BinDeserializer.rentFromPool(input);
        var dst = (InvokeRequire) is.deserialize();
        assertEquals(src.shard, dst.shard);
        assertEquals(src.service, dst.service);
        assertEquals(src.getArgsCount(), dst.getArgsCount());
        assertEquals(src.getArg(0).getInt(), dst.getArg(0).getInt());
        BinDeserializer.backToPool(is);
    }
}
