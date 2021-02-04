import appbox.channel.IHostMessageChannel;
import appbox.channel.messages.*;
import appbox.entities.Employee;
import appbox.entities.Enterprise;
import appbox.entities.OrgUnit;
import appbox.runtime.RuntimeContext;
import appbox.server.runtime.HostRuntimeContext;
import appbox.store.EntityStore;
import appbox.store.KVTransaction;
import appbox.store.KVTxnId;
import appbox.store.SysStoreApi;
import appbox.store.query.IndexGet;
import appbox.store.query.TableScan;
import appbox.utils.IdUtil;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

//注意：需要主进程启动后再进行测试

public class TestSysStore {

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

    private static KVInsertDataRequire makeInsertDataCommand(KVTxnId txnId) {
        var cmd = new KVInsertDataRequire(txnId);
        cmd.key  = new byte[]{65, 66, 67, 68}; //ABCD
        cmd.data = new byte[]{65, 66, 67, 68};
        return cmd;
    }

    @Test
    public void testKVInsertCommand() throws ExecutionException, InterruptedException {
        final KVTxnId txnId = new KVTxnId();

        var fut = SysStoreApi.beginTxnAsync() //启动事务
                .thenCompose(res -> {
                    txnId.copyFrom(res.txnId);
                    return SysStoreApi.execKVInsertAsync(makeInsertDataCommand(txnId)); //执行Insert命令
                })
                .thenCompose(res -> SysStoreApi.commitTxnAsync(txnId)); //递交事务

        var res = fut.get();
        assertEquals(0, res.errorCode);
    }

    @Test
    public void testUpdateEntity() {
        var q = new TableScan<>(Employee.MODELID, Employee.class);
        q.where(Employee.NAME.eq("Admin"));
        var list = q.toListAsync().join();
        var emp  = list.get(0);
        emp.setBirthday(LocalDateTime.of(1977, 1, 27, 8, 8));
        EntityStore.saveAsync(emp).join();
    }

    @Test
    public void testKVDeleteCommand() throws ExecutionException, InterruptedException {
        var txn = SysStoreApi.beginTxnAsync().get();
        var cmd = new KVDeleteDataRequest(txn.txnId);
        cmd.key = new byte[]{65, 66, 67, 68}; //ABCD

        var fut = SysStoreApi.execKVDeleteAsync(cmd)
                .thenCompose(res -> SysStoreApi.commitTxnAsync(txn.txnId));

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
        var  req     = new KVGetModelRequest(modelId);

        var fut = SysStoreApi.execKVGetAsync(req, new KVGetModelResponse());
        var res = fut.get();
        assertEquals(0, res.errorCode);
    }

    @Test
    public void testKVScanModels() throws Exception {
        var req = new KVScanAppsRequest();
        var fut = SysStoreApi.execKVScanAsync(req, new KVScanAppsResponse());
        var res = fut.get();
        assertEquals(0, res.errorCode);
    }

    @Test
    public void testTableScan() throws Exception {
        var q = new TableScan<>(IdUtil.SYS_ENTERPRISE_MODEL_ID, Enterprise.class);
        q.where(Enterprise.NAME.eq("AppBoxFuture"));
        var list = q.toListAsync().get();
        assertNotNull(list);
    }

    /** 测试异常时自动回滚事务 */
    @Test
    public void testAutoRollbackTxnOnException() throws Exception {
        var txn = KVTransaction.beginAsync().get();
        var obj = new Enterprise();
        obj.setName("Future Studio");

        var batch = EntityStore.insertEntityAsync(obj, txn)
                .thenCompose(r -> EntityStore.insertEntityAsync(obj, txn)) //重复插入相同主键引发异常
                .thenCompose(r -> txn.commitAsync());

        try {
            batch.get();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /** 测试带二级索引的实体 */
    @Test
    public void testEntityWithIndex() throws Exception {
        var emp = new Employee();
        emp.setName("Rick");
        emp.setMale(true);
        emp.setAccount("cccc");
        emp.setPassword(new byte[]{1, 2, 3, 4});

        //insert
        EntityStore.insertEntityAsync(emp).get();
        //delete
        EntityStore.deleteEntityAsync(emp).get();
    }

    /** 测试通过惟一索引查找 */
    @Test
    public void testIndexGet() throws Exception {
        var q = new IndexGet<>(Employee.UI_Account.class);
        q.where(Employee.ACCOUNT, "cccc");
        var row = q.toIndexRowAsync().get();
        assertNotNull(row);
    }

    @Test
    public void testToTreePath() throws Exception {
        var q = new TableScan<>(IdUtil.SYS_ORGUNIT_MODEL_ID, OrgUnit.class);
        q.where(OrgUnit.NAME.eq("Admin"));
        var list  = q.toListAsync().get();
        var admin = list.get(0);
        assertNotNull(admin);

        var treePath = EntityStore.loadTreePathAsync(OrgUnit.class,
                admin.id(), OrgUnit::getParentId, OrgUnit::getName).get();
        assertNotNull(treePath);
    }

}
