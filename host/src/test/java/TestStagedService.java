import appbox.channel.SharedMemoryChannel;
import appbox.design.MockDeveloperSession;
import appbox.design.services.StagedService;
import appbox.entities.StagedModel;
import appbox.model.ModelType;
import appbox.runtime.RuntimeContext;
import appbox.server.runtime.HostRuntimeContext;
import appbox.store.SysStoreApi;
import appbox.store.query.TableScan;
import appbox.utils.IdUtil;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class TestStagedService {

    private static SharedMemoryChannel channel;

    @BeforeAll
    public static void init() {
        RuntimeContext.init(new HostRuntimeContext(), (short) 10410);
        channel = new SharedMemoryChannel("AppChannel");
        SysStoreApi.init(channel);

        //注入模拟会话
        var session = new MockDeveloperSession();
        RuntimeContext.current().setCurrentSession(session);

        CompletableFuture.runAsync(() -> {
            channel.startReceive();
        });
    }

    @AfterAll
    public static void clean() {
        //TODO: storeReceive
    }

    private static long makeServiceModelId(long idIndex) {
        var modelId = ((long) IdUtil.SYS_APP_ID << IdUtil.MODELID_APPID_OFFSET)
                | ((long) ModelType.Service.value << IdUtil.MODELID_TYPE_OFFSET)
                | (idIndex << IdUtil.MODELID_SEQ_OFFSET);
        return modelId;
    }

    @Test
    public void testLoadAllStaged() throws ExecutionException, InterruptedException {
        var testServiceId = makeServiceModelId(1);
        //var list = StagedService.loadStagedAsync(false).get();
        //var q = new TableScan<>(IdUtil.SYS_STAGED_MODEL_ID, StagedModel.class);
        var developerID = RuntimeContext.current().currentSession().leafOrgUnitId();

        var q = new TableScan<>(IdUtil.SYS_STAGED_MODEL_ID, StagedModel.class);
        q.where(StagedModel.TYPE.eq((byte) 2)
                .and(StagedModel.MODEL.eq(Long.toUnsignedString(testServiceId)))
                .and(StagedModel.DEVELOPER.eq(developerID)));
        var list = q.toListAsync().get();
        System.out.println(list == null);
    }

    @Test
    public void testSaveAndLoadServiceCode() throws ExecutionException, InterruptedException {
        var testServiceId = makeServiceModelId(1);
        StagedService.saveServiceCodeAsync(testServiceId, "AAAA").get();
        var code = StagedService.loadServiceCode(testServiceId).get();
        assertEquals("AAAA", code);
    }

}
