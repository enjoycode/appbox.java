import appbox.channel.SharedMemoryChannel;
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

    private static SharedMemoryChannel channel;

    @BeforeAll
    public static void init() {
        RuntimeContext.init(new HostRuntimeContext(), (short) 1/*TODO: fix peerId*/);
        channel = new SharedMemoryChannel("AppChannel");
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
        var s = new SystemService();
        var res = s.login(1L, reqJson).get();
        assertNotNull(res);
    }

}
