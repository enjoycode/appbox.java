import appbox.design.services.CodeGenService;
import appbox.design.services.PublishService;
import appbox.model.*;
import org.eclipse.core.runtime.IPath;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class TestServiceCodeGenerator {

    private static InputStream loadTestServiceCode(IPath path) {
        return TestServiceCodeGenerator.class.getResourceAsStream("/test_services/" + path.lastSegment());
    }

    @Test
    public void testGenServiceCode() throws Exception {
        final var hub = TestHelper.makeDesignHub(TestServiceCodeGenerator::loadTestServiceCode, true);

        //准备测试模型
        final var dataStoreModel       = TestHelper.makeSqlDataStoreModel();
        final var entityModel          = TestHelper.makeEmployeeModel(dataStoreModel);
        final var testServiceModel     = TestHelper.makeServiceModel(10, "TestService");
        final var adminPermissionModel = TestHelper.makeAdminPermissionModel();

        final List<ModelBase> models = List.of(entityModel, testServiceModel, adminPermissionModel);
        TestHelper.injectAndLoadTree(dataStoreModel, models);

        //测试实体代码生成
        var entityCode = CodeGenService.genEntityDummyCode(entityModel, "sys", hub.designTree);
        //测试服务代理生成
        var serviceNode = hub.designTree.findModelNode(testServiceModel.id());
        var proxyCode   = CodeGenService.genServiceProxyCode(hub, serviceNode);
        //测试前端声明生成
        var declareCode = CodeGenService.genServiceDeclareCode(hub, serviceNode);

        //测试转译服务代码
        var codeData = PublishService.compileService(hub, testServiceModel, false);
        assertNotNull(codeData);
        //写入测试文件
        var outPath = Path.of("/", "tmp", "appbox", "TestService.data");
        Files.deleteIfExists(outPath);
        Files.createFile(outPath);
        Files.write(outPath, codeData);
    }

}
