import appbox.compression.BrotliUtil;
import appbox.runtime.IService;
import appbox.runtime.InvokeArgs;
import appbox.serialization.BytesOutputStream;
import appbox.server.runtime.ServiceClassLoader;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class TestServiceClassLoader {

    @Test
    public void testLoadService() throws Exception {
        //测试前先生成测试文件 design/testServiceCodeGenerator
        var outPath        = Path.of("/", "tmp", "appbox", "TestService.data");
        var compressedData = Files.readAllBytes(outPath);

        var serviceClassLoader = new ServiceClassLoader();
        var clazz              = serviceClassLoader.loadServiceClass("TestService", compressedData);
        assertNotNull(clazz);

        var instance = (IService) clazz.getDeclaredConstructor().newInstance();
        var args     = InvokeArgs.make().add("Future").add(100).done();
        var res      = instance.invokeAsync("hello", args).get();
        assertEquals("Hello Future!", res.toString());
    }

    /** 测试加载执行引用第三方包的服务 */
    @Test
    public void testLoadServiceWith3rdLib() throws Exception {
        //先打包之前生成的class文件
        final var outPath = "/tmp/appbox/workspace/17871358918657/runtime_11428632783283552269/bin/";
        final var classFiles = new String[]{
                "HelloService",
                "HelloService$SYS_Student"
        };

        final var outStream = new BytesOutputStream(2048);
        outStream.writeVariant(classFiles.length); //.class文件数
        for (var classFile : classFiles) {
            outStream.writeString(classFile); //写入类名称
            final var filePath = Path.of(outPath, classFile + ".class");
            outStream.writeByteArray(Files.readAllBytes(filePath));
        }

        final var classData = BrotliUtil.compress(outStream.getBuffer(), 0, outStream.size());

        var serviceClassLoader = new ServiceClassLoader();
        var clazz              = serviceClassLoader.loadServiceClass(classFiles[0], classData);
        assertNotNull(clazz);

        var instance = (IService) clazz.getDeclaredConstructor().newInstance();
        //var res      = instance.invokeAsync("sayHello", null).get();
        //assertEquals("Hello Future2222!", res.toString());
        try {
            instance.invokeAsync("sayHello", null).whenComplete((r,ex) -> {
                assertNotNull(ex);
                //return null;
            }).join();
        } catch (Throwable ex) {
            ex.printStackTrace();
        }

        //Thread.sleep(3000);
    }

}
