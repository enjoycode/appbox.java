import appbox.serialization.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestMsgSerialization {

    /**
     * 保存客户端请求至文件方便Post测试
     */
    @Test
    public void saveClientInvokeRequire() throws Exception {
        var output = new BytesOutputStream(8192);
        output.writeString("sys.TestService.sayHello");
        output.serialize("Future");

        output.saveToFile(0, "src/test/java/InvokeRequire.bin");
    }
}
