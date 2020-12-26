import appbox.design.MockDeveloperSession;
import appbox.design.services.CodeGenService;
import appbox.design.services.PublishService;
import appbox.entities.Employee;
import appbox.model.*;
import appbox.model.entity.DataFieldModel;
import appbox.model.entity.EntityRefModel;
import appbox.model.entity.FieldWithOrder;
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
        return ((long) IdUtil.SYS_APP_ID << IdUtil.MODELID_APPID_OFFSET)
                | ((long) ModelType.Service.value << IdUtil.MODELID_TYPE_OFFSET)
                | (idIndex << IdUtil.MODELID_SEQ_OFFSET);
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
        //生成测试DataStore
        var dataStoreModel = new DataStoreModel(DataStoreModel.DataStoreKind.Sql, "PostgreSql", "DemoDB");
        models.add(dataStoreModel);

        //生成测试实体模型
        var entityModel = new EntityModel(IdUtil.SYS_EMPLOYEE_MODEL_ID, "Employee");
        //entityModel.bindToSysStore(true, false);
        entityModel.bindToSqlStore(dataStoreModel.id());
        //memebers
        var nameField = new DataFieldModel(entityModel, "Name", DataFieldModel.DataFieldType.String, false);
        entityModel.addSysMember(nameField, Employee.NAME_ID);
        var maleField = new DataFieldModel(entityModel, "Male", DataFieldModel.DataFieldType.Bool, false);
        entityModel.addSysMember(maleField, Employee.MALE_ID);
        var managerFK = new DataFieldModel(entityModel, "ManagerName", DataFieldModel.DataFieldType.String, true, true);
        entityModel.addSysMember(managerFK, Employee.ACCOUNT_ID);
        var manager = new EntityRefModel(entityModel, "Manager", IdUtil.SYS_EMPLOYEE_MODEL_ID,
                new short[]{managerFK.memberId()}, true);
        manager.setAllowNull(true);
        entityModel.addSysMember(manager, Employee.PASSWORD_ID);
        //pk
        entityModel.sqlStoreOptions().setPrimaryKeys(new FieldWithOrder[] {
                new FieldWithOrder(nameField.memberId())
        });

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

        //测试实体虚拟代码生成
        var entityCode = CodeGenService.genEntityDummyCode(entityModel, "sys", hub.designTree);

        //测试服务代理生成
        var serviceNode = hub.designTree.findModelNode(testServiceModel.id());
        var proxyCode   = CodeGenService.genServiceProxyCode(hub, serviceNode);
        //测试前端声明生成
        var declareCode = CodeGenService.genServiceDeclareCode(hub, serviceNode);

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
