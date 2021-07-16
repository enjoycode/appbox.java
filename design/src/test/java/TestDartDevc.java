import appbox.design.MockDeveloperSession;
import appbox.runtime.MockRuntimeContext;
import appbox.runtime.RuntimeContext;
import org.apache.commons.lang3.time.StopWatch;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/** 测试预览文件生成 */
public class TestDartDevc {

    @Test
    public void testGenJsCode() {
        System.setProperty("user.dir", Path.of(System.getProperty("user.dir")).getParent().toString());

        var ctx = new MockRuntimeContext();
        RuntimeContext.init(ctx, (short) 10421);

        var session = new MockDeveloperSession();
        ctx.setCurrentSession(session);
        var hub = session.getDesignHub();
        hub.setIDE(true);
        //hub.typeSystem.init(); //必须初始化

        StopWatch stopWatch = StopWatch.createStarted();
        var result =
                hub.dartLanguageServer.compilePreview("packages/appbox/sys/views/HomePage.dart.js", true).join();
        stopWatch.stop();
        assertNotNull(result);
        System.out.println("耗时: " + stopWatch.getTime()); //约450毫秒
    }

}
