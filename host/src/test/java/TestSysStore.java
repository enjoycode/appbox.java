import appbox.core.runtime.RuntimeContext;
import appbox.server.channel.SharedMemoryChannel;
import appbox.server.channel.messages.KVInsertRequire;
import appbox.server.runtime.HostRuntimeContext;
import appbox.store.SysStoreApi;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

//注意：需要主进程启动后再进行测试

public class TestSysStore {

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
    public void testKVInsertCommand() throws ExecutionException, InterruptedException {
        var cmd = new KVInsertRequire();
        cmd.raftGroupId = 0;
        cmd.dataCF = -1;

        var fut = SysStoreApi.execKVInsertAsync(cmd);
        var res = fut.get();
    }
}
