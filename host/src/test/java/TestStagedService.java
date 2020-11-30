import appbox.channel.SharedMemoryChannel;
import appbox.design.services.StagedService;
import appbox.logging.Log;
import appbox.model.ApplicationModel;
import appbox.model.ModelBase;
import appbox.model.ModelType;
import appbox.model.ServiceModel;
import appbox.runtime.MockRuntimeContext;
import appbox.runtime.RuntimeContext;
import appbox.server.runtime.HostRuntimeContext;
import appbox.store.SysStoreApi;
import appbox.utils.IdUtil;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class TestStagedService {

    private static SharedMemoryChannel channel;

    private static final UUID devId =new UUID(-6038453433195871438l,-7082168417221633763l);

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

    private static long makeServiceModelId(long idIndex) {
        var modelId = ((long) IdUtil.SYS_APP_ID << IdUtil.MODELID_APPID_OFFSET)
                | ((long) ModelType.Service.value << IdUtil.MODELID_TYPE_OFFSET)
                | (idIndex << IdUtil.MODELID_SEQ_OFFSET);
        return modelId;
    }

    @Test
    public void loadCodeDataAsync() throws Exception{
        var ctx = new MockRuntimeContext();
        RuntimeContext.init(ctx, (short) 10421);

        var appModel = new ApplicationModel("appbox", "sys");
        var models = new ArrayList<ModelBase>();
        ////生成测试服务模型
        //var testServiceModel = new ServiceModel(makeServiceModelId(10), "TestService");
        //models.add(testServiceModel);
        ////注入测试模型
        //ctx.injectApplicationModel(appModel);
        //ctx.injectModels(models);

        var session = new MockDeveloperSession();
        ctx.setCurrentSession(session);
        var res=StagedService.loadCodeDataAsync(IdUtil.SYS_CHECKOUT_MODEL_ID).get();
        Log.debug("size:"+res.length);
    }
    @Test
    public void saveModelAsync(){

    }
    @Test
    public void saveFolderAsync(){

    }
    @Test
    public void saveServiceCodeAsync(){

    }
    @Test
    public void loadServiceCode(){

    }
    @Test
    public void saveReportCodeAsync(){

    }
    @Test
    public void saveViewCodeAsync(){

    }
    @Test
    public void saveViewRuntimeCodeAsync(){

    }
    @Test
    public void loadViewRuntimeCode(){

    }
    @Test
    public void loadStagedAsync(){

    }
    @Test
    public void deleteStagedAsync(){

    }
    @Test
    public void deleteModelAsync(){

    }




}
