import appbox.design.MockDeveloperSession;
import appbox.design.services.PublishService;
import appbox.entities.Employee;
import appbox.model.*;
import appbox.model.entity.DataFieldModel;
import appbox.runtime.MockRuntimeContext;
import appbox.runtime.RuntimeContext;
import appbox.utils.IdUtil;
import org.eclipse.core.runtime.IPath;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

public class TestServiceCodeGenerator {

    private static long makeServiceModelId(long idIndex) {
        var modelId = ((long) IdUtil.SYS_APP_ID << IdUtil.MODELID_APPID_OFFSET)
                | ((long) ModelType.Service.value << IdUtil.MODELID_TYPE_OFFSET)
                | (idIndex << IdUtil.MODELID_SEQ_OFFSET);
        return modelId;
    }

    private static InputStream loadTestServiceCode(IPath path) {
        return TestServiceCodeGenerator.class.getResourceAsStream("/test_services/" + path.lastSegment());
    }

    @Test
    public void testGenServiceCode() throws Exception {
        var ctx = new MockRuntimeContext();
        RuntimeContext.init(ctx, (short) 10421);

        var appModel = new ApplicationModel("appbox", "sys");
        var models   = new ArrayList<ModelBase>();
        //生成测试实体模型
        var entityModel = new EntityModel(IdUtil.SYS_EMPLOYEE_MODEL_ID, "Employee");
        entityModel.bindToSysStore(true, false);
        var nameField = new DataFieldModel(entityModel, "Name", DataFieldModel.DataFieldType.String, false);
        entityModel.addSysMember(nameField, Employee.NAME_ID);
        var maleField = new DataFieldModel(entityModel, "Male", DataFieldModel.DataFieldType.Bool, false);
        entityModel.addSysMember(maleField, Employee.MALE_ID);

        models.add(entityModel);
        //生成测试服务模型
        var testServiceModel = new ServiceModel(makeServiceModelId(10), "TestService");
        models.add(testServiceModel);
        //注入测试模型
        ctx.injectApplicationModel(appModel);
        ctx.injectModels(models);

        var session = new MockDeveloperSession();
        ctx.setCurrentSession(session);
        var hub = session.getDesignHub();
        hub.typeSystem.init(); //必须初始化
        hub.typeSystem.languageServer.loadFileDelegate = TestServiceCodeGenerator::loadTestServiceCode;
        hub.designTree.loadNodesForTest(appModel, models);

        //测试转译服务代码
        var codeData = PublishService.compileService(hub, testServiceModel, null);
        assertNotNull(codeData);
        //写入测试文件
        var outPath = Path.of("/", "tmp", "appbox", "TestService.data");
        Files.deleteIfExists(outPath);
        Files.createFile(outPath);
        Files.write(outPath, codeData);
    }

}
