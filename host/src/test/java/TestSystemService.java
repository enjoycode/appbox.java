import appbox.channel.IHostMessageChannel;
import appbox.runtime.RuntimeContext;
import appbox.server.runtime.HostRuntimeContext;
import appbox.server.services.SystemService;
import appbox.store.SysStoreApi;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.CompletableFuture;

public class TestSystemService {

    private static IHostMessageChannel channel;

    @BeforeAll
    public static void init() {
        RuntimeContext.init(new HostRuntimeContext("AppChannel"), (short) 1/*TODO: fix peerId*/);
        channel = ((HostRuntimeContext) RuntimeContext.current()).channel;
        SysStoreApi.init(channel);
        CompletableFuture.runAsync(() -> {
            channel.startReceive();
        });
    }

    @AfterAll
    public static void clean() {
        //TODO: storeReceive
    }

    @Test
    public void testLogin() throws Exception {
        var reqJson = "{\"u\":\"Admin\",\"p\":\"1\",\"e\":false}";
        var s       = new SystemService();
        var res     = s.login(1L, reqJson).get();
        assertNotNull(res);
    }

}
