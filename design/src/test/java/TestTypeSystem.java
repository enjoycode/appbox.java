import appbox.design.MockDeveloperSession;
import appbox.model.ApplicationModel;
import appbox.model.EntityModel;
import appbox.model.ModelBase;
import appbox.model.ServiceModel;
import appbox.runtime.MockRuntimeContext;
import appbox.runtime.RuntimeContext;
import appbox.utils.IdUtil;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

public class TestTypeSystem {

    @Test
    public void testEntityModelDummyCode() {
        var ctx = new MockRuntimeContext();
        RuntimeContext.init(ctx, (short) 10421);

        var appModel = new ApplicationModel("appbox", "sys");
        var models   = new ArrayList<ModelBase>();
        //生成测试实体模型
        var entityModel = new EntityModel(IdUtil.SYS_EMPLOYEE_MODEL_ID, "Employee");
        entityModel.bindToSysStore(true, false);
        models.add(entityModel);
        //注入测试模型
        ctx.injectApplicationModel(appModel);
        ctx.injectModels(models);

        var session = new MockDeveloperSession();
        ctx.setCurrentSession(session);
        var hub = session.getDesignHub();
        hub.typeSystem.init(); //必须初始化
        //hub.typeSystem.languageServer.loadFileDelegate = TestServiceCodeGenerator::loadTestServiceCode;
        hub.designTree.loadNodesForTest(appModel, models);
    }


}
