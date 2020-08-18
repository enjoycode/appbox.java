import appbox.core.runtime.RuntimeContext;
import appbox.server.channel.SharedMemoryChannel;
import appbox.server.channel.messages.KVBeginTxnRequire;
import appbox.server.channel.messages.KVEndTxnRequire;
import appbox.server.channel.messages.KVInsertRequire;
import appbox.server.runtime.HostRuntimeContext;
import appbox.store.SysStoreApi;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

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
        cmd.dataCF      = -1;
        cmd.key         = new byte[]{65, 66, 67, 68}; //ABCD
        cmd.data        = new byte[]{65, 66, 67, 68};

        var fut = SysStoreApi.beginTxnAsync() //启动事务
                .thenCompose(res -> {
                    cmd.txnId.copyFrom(res.txnId);
                    return SysStoreApi.execKVInsertAsync(cmd); //执行Insert命令
                }).thenCompose(res -> {
                    var commitTxn = new KVEndTxnRequire();
                    commitTxn.txnId.copyFrom(cmd.txnId);
                    commitTxn.action = 0;
                    return SysStoreApi.endTxnAsync(commitTxn); //递交事务
                });

        var res = fut.get();
        assertEquals(0, res.errorCode);
    }

    @Test
    public void testTxnBeginAndEnd() throws ExecutionException, InterruptedException {
        var fut1 = SysStoreApi.beginTxnAsync();
        var res1 = fut1.get();
        assertEquals(0, res1.errorCode);

        var req2 = new KVEndTxnRequire();
        req2.txnId.copyFrom(res1.txnId);
        req2.action = 1;
        var fut2 = SysStoreApi.endTxnAsync(req2);
        var res2 = fut2.get();
        assertEquals(0, res2.errorCode);
    }

}
