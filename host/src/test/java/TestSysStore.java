import appbox.channel.messages.*;
import appbox.runtime.RuntimeContext;
import appbox.channel.SharedMemoryChannel;
import appbox.server.runtime.HostRuntimeContext;
import appbox.store.KVTxnId;
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
        final KVTxnId txnId = new KVTxnId();

        var fut = SysStoreApi.beginTxnAsync() //启动事务
                .thenCompose(res -> {
                    txnId.copyFrom(res.txnId);
                    var cmd = new KVInsertDataRequire(txnId);
                    cmd.raftGroupId = 0;
                    cmd.dataCF      = -1;
                    cmd.key         = new byte[]{65, 66, 67, 68}; //ABCD
                    cmd.data        = new byte[]{65, 66, 67, 68};
                    return SysStoreApi.execKVInsertAsync(cmd); //执行Insert命令
                })
                .thenCompose(res -> SysStoreApi.commitTxnAsync(txnId)); //递交事务

        var res = fut.get();
        assertEquals(0, res.errorCode);
    }

    @Test
    public void testKVDeleteCommand() throws ExecutionException, InterruptedException {
        var cmd = new KVDeleteRequire();
        cmd.raftGroupId = 0;
        cmd.dataCF      = -1;
        cmd.key         = new byte[]{65, 66, 67, 68}; //ABCD

        var fut = SysStoreApi.beginTxnAsync()
                .thenCompose(res -> {
                    cmd.txnId.copyFrom(res.txnId);
                    return SysStoreApi.execKVDeleteAsync(cmd);
                }).thenCompose(res -> SysStoreApi.commitTxnAsync(cmd.txnId));

        var res = fut.get();
        assertEquals(0, res.errorCode);
    }

    @Test
    public void testTxnBeginAndEnd() throws ExecutionException, InterruptedException {
        var fut1 = SysStoreApi.beginTxnAsync();
        var res1 = fut1.get();
        assertEquals(0, res1.errorCode);

        var fut2 = SysStoreApi.rollbackTxnAsync(res1.txnId);
        var res2 = fut2.get();
        assertEquals(0, res2.errorCode);
    }

    @Test
    public void testKVGetModel() throws Exception {
        long modelId = 0x9E9AA8F702000004L;
        var  req     = new KVGetModelRequest(modelId, KVReadDataType.Model);

        var fut = SysStoreApi.execKVGetAsync(req);
        var res = fut.get();
        assertEquals(0, res.errorCode);
    }

    @Test
    public void testKVScanModels() throws Exception {
        var req = new KVScanModelsRequest(true);
        var fut = SysStoreApi.execKVScanAsync(req);
        var res = fut.get();
        assertEquals(0, res.errorCode);
    }

}
