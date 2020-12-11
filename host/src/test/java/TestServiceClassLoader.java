import appbox.runtime.IService;
import appbox.runtime.InvokeArgs;
import appbox.server.runtime.ServiceClassLoader;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class TestServiceClassLoader {

    @Test
    public void testLoadService() throws Exception {
        //测试前先生成测试文件 design/testServiceCodeGenerator
        var outPath = Path.of("/", "tmp", "appbox", "TestService.data");
        var compressedData = Files.readAllBytes(outPath);

        var serviceClassLoader = new ServiceClassLoader();
        var clazz = serviceClassLoader.loadServiceClass("TestService", compressedData);
        assertNotNull(clazz);

        var instance = (IService) clazz.getDeclaredConstructor().newInstance();
        var args = InvokeArgs.make().add("Future").add(100).done();
        var res = instance.invokeAsync("hello", args).get();
        assertEquals("Hello Future!", res.toString());
    }

}
