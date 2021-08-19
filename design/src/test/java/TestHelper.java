import appbox.design.DesignHub;
import appbox.design.MockDeveloperSession;
import appbox.entities.Employee;
import appbox.model.*;
import appbox.model.entity.DataFieldModel;
import appbox.model.entity.EntityRefModel;
import appbox.model.entity.EntitySetModel;
import appbox.model.entity.FieldWithOrder;
import appbox.runtime.MockRuntimeContext;
import appbox.runtime.RuntimeContext;
import appbox.utils.IdUtil;
import org.eclipse.core.runtime.IPath;

import java.io.InputStream;
import java.util.List;
import java.util.function.Function;

public final class TestHelper {

    /** 从资源中加载待测试的服务代码 */
    public static InputStream loadTestServiceCode(IPath path) {
        return TestHelper.class.getResourceAsStream("/test_services/" + path.lastSegment());
    }

    public static DesignHub makeDesignHub(Function<IPath, InputStream> loadFileDelegate, boolean needInit) {
        var ctx = new MockRuntimeContext();
        RuntimeContext.init(ctx, (short) 10421);

        var session = new MockDeveloperSession();
        session.loadFileDelegate = loadFileDelegate;
        ctx.setCurrentSession(session);
        var hub = session.getDesignHub();
        if (needInit)
            hub.typeSystem.init(); //必须初始化

        return hub;
    }

    public static void injectAndLoadTree(DataStoreModel dataStoreModel, List<ModelBase> models) {
        final var appModel = makeApplicationModel();
        final var ctx      = (MockRuntimeContext) RuntimeContext.current();
        //注入测试模型
        ctx.injectApplicationModel(appModel);
        ctx.injectModels(models);

        final var session = (MockDeveloperSession) RuntimeContext.current().currentSession();
        session.getDesignHub().designTree.loadNodesForTest(appModel, dataStoreModel, models);
    }

    public static ApplicationModel makeApplicationModel() {
        return new ApplicationModel("appbox", "sys");
    }

    public static DataStoreModel makeSqlDataStoreModel() {
        return new DataStoreModel(DataStoreModel.DataStoreKind.Sql, "PostgreSql", "DemoDB");
    }

    public static EntityModel makeEmployeeModel(DataStoreModel dataStoreModel) {
        var entityModel = new EntityModel(IdUtil.SYS_EMPLOYEE_MODEL_ID, "Employee");
        //entityModel.bindToSysStore(true, false);
        entityModel.bindToSqlStore(dataStoreModel.id());
        //memebers
        var nameField = new DataFieldModel(entityModel, "Name", DataFieldModel.DataFieldType.String, false);
        entityModel.addSysMember(nameField, Employee.NAME_ID);
        var maleField = new DataFieldModel(entityModel, "Male", DataFieldModel.DataFieldType.Bool, false);
        entityModel.addSysMember(maleField, Employee.MALE_ID);
        var ageField = new DataFieldModel(entityModel, "Age", DataFieldModel.DataFieldType.Int, false);
        entityModel.addSysMember(ageField, Employee.BIRTHDAY_ID);
        var managerFK = new DataFieldModel(entityModel, "ManagerName", DataFieldModel.DataFieldType.String, true, true);
        entityModel.addSysMember(managerFK, Employee.ACCOUNT_ID);
        var manager = new EntityRefModel(entityModel, "Manager", IdUtil.SYS_EMPLOYEE_MODEL_ID,
                new short[]{managerFK.memberId()}, true);
        manager.setAllowNull(true);
        entityModel.addSysMember(manager, Employee.PASSWORD_ID);
        var underling = new EntitySetModel(entityModel, "Underling", IdUtil.SYS_EMPLOYEE_MODEL_ID, manager.memberId());
        entityModel.addSysMember(underling, (short) (7 << IdUtil.MEMBERID_SEQ_OFFSET));
        //pk
        entityModel.sqlStoreOptions().setPrimaryKeys(new FieldWithOrder[]{
                new FieldWithOrder(nameField.memberId())
        });

        return entityModel;
    }

    public static EntityModel makeEntityModel(long idIndex, String name) {
        return new EntityModel(makeModelId(ModelType.Entity, idIndex), name);
    }

    public static ServiceModel makeServiceModel(long idIndex, String name) {
        return new ServiceModel(makeModelId(ModelType.Service, idIndex), name);
    }

    public static PermissionModel makeAdminPermissionModel() {
        final var adminPermissionModel = new PermissionModel(IdUtil.SYS_PERMISSION_ADMIN_ID, "Admin");
        adminPermissionModel.setRemark("系统管理员");
        return adminPermissionModel;
    }

    private static long makeModelId(ModelType type, long idIndex) {
        return ((long) IdUtil.SYS_APP_ID << IdUtil.MODELID_APPID_OFFSET)
                | ((long) type.value << IdUtil.MODELID_TYPE_OFFSET)
                | (idIndex << IdUtil.MODELID_SEQ_OFFSET);
    }

}
