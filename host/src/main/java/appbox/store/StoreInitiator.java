package appbox.store;

import appbox.entities.*;
import appbox.logging.Log;
import appbox.model.ApplicationModel;
import appbox.model.EntityModel;
import appbox.model.ModelType;
import appbox.model.ServiceModel;
import appbox.model.entity.DataFieldModel;
import appbox.model.entity.FieldWithOrder;
import appbox.model.entity.SysIndexModel;
import appbox.runtime.RuntimeContext;
import appbox.server.runtime.HostRuntimeContext;
import appbox.store.utils.ModelCodeUtil;
import appbox.utils.IdUtil;

import static appbox.model.entity.DataFieldModel.DataFieldType;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * 系统存储初始化，仅用于启动集群第一节点时
 */
public final class StoreInitiator {

    public static CompletableFuture<Boolean> initAsync() {
        //TODO:考虑判断是否已初始化
        Log.debug("Start init system store...");

        return createAppAsync().thenCompose(app -> {
            try {
                //新建EntityModels
                var enterpriseModel = createEnterpriseModel();
                var employeeModel    = createEmployeeModel();
                
                var workgroupModel = createWorkgroupModel();
                var orgunitModel = createOrgUnitModel();
                var stagedModel = createStagedModel();
                var checkoutModel = createCheckoutModel();

                //新建默认组织

                //新建默认系统管理员及测试账号

                //新建默认组织单元

                //将新建的模型加入运行时上下文
                var ctx = (HostRuntimeContext) RuntimeContext.current();
                ctx.injectApplication(app);
                ctx.injectModel(enterpriseModel);
                ctx.injectModel(employeeModel);
                ctx.injectModel(workgroupModel);
                ctx.injectModel(orgunitModel);
                ctx.injectModel(stagedModel);
                ctx.injectModel(checkoutModel);



                //开始事务保存
                return KVTransaction.beginAsync()
                        .thenCompose(txn -> ModelStore.insertModelAsync(employeeModel, txn)
                                .thenCompose(r -> ModelStore.insertModelAsync(enterpriseModel, txn))
                                .thenCompose(r -> ModelStore.insertModelAsync(workgroupModel, txn))
                                .thenCompose(r -> ModelStore.insertModelAsync(orgunitModel, txn))
                                .thenCompose(r -> ModelStore.insertModelAsync(stagedModel, txn))
                                .thenCompose(r -> ModelStore.insertModelAsync(checkoutModel, txn))
                                .thenCompose(r -> createServiceModel("TestService", 1, null, txn))
                                .thenCompose(r -> insertEntities(txn))
                                .thenCompose(r -> txn.commitAsync())
                                .thenApply(r -> true));
            } catch (Exception e) {
                Log.error(e.getMessage());
                return CompletableFuture.completedFuture(false);
            }
        }).exceptionally(ex -> {
            Log.error(ex.getMessage());
            return false;
        });
    }

    private static EntityModel createCheckoutModel() throws Exception{
        var model = new EntityModel(IdUtil.SYS_CHECKOUT_MODEL_ID, "Checkout");
        model.bindToSysStore(true, false);
        var nodeTypeFiled = new DataFieldModel(model, "NodeType", DataFieldType.Byte, false);
        model.addSysMember(nodeTypeFiled, Checkout.NODE_TYPE_ID);

        var targetIdFiled = new DataFieldModel(model, "TargetId", DataFieldType.String, false);
        targetIdFiled.setLength(100);
        model.addSysMember(targetIdFiled, Checkout.TARGET_ID);

        var developerIdFiled = new DataFieldModel(model, "DeveloperId", DataFieldType.Guid, false);
        model.addSysMember(developerIdFiled, Checkout.DEVELOPER_ID);

        var developerNameFiled = new DataFieldModel(model, "DeveloperName", DataFieldType.String, false);
        developerNameFiled.setLength(100);
        model.addSysMember(developerNameFiled, Checkout.DEVELOPER_NAME_ID);

        var versionFiled = new DataFieldModel(model, "Version", DataFieldType.Int, false);
        model.addSysMember(versionFiled, Checkout.VERSION_ID);
        return model;
    }

    private static EntityModel createStagedModel() throws Exception{
        var model = new EntityModel(IdUtil.SYS_STAGED_MODEL_ID, "StagedModel");
        model.bindToSysStore(false, false); //非MVCC

        var typeFiled = new DataFieldModel(model, "Type", DataFieldType.Byte, false, false);
        model.addSysMember(typeFiled, StagedModel.TYPE_ID);

        var modelFiled = new DataFieldModel(model, "ModelId", DataFieldType.String, false, false);
        modelFiled.setLength(100);
        model.addSysMember(modelFiled, StagedModel.MODEL_ID);

        var developerFiled = new DataFieldModel(model, "DeveloperId", DataFieldType.Guid, false, false);
        model.addSysMember(developerFiled, StagedModel.DEVELOPER_ID);

        var dataFiled = new DataFieldModel(model, "Data", DataFieldType.Binary, false, false);
        model.addSysMember(dataFiled, StagedModel.DATA_ID);
        return model;
    }

    private static EntityModel createOrgUnitModel() throws Exception{
        var model = new EntityModel(IdUtil.SYS_ORGUNIT_MODEL_ID, "OrgUnit");
        model.bindToSysStore(true, false);
        var nameFiled = new DataFieldModel(model, "Name", DataFieldType.String, false, false);
        nameFiled.setLength(100);
        model.addSysMember(nameFiled, Orgunit.NAME_ID);

        var baseFiled = new DataFieldModel(model, "BaseId", DataFieldType.Guid, false, false);
        model.addSysMember(baseFiled, Orgunit.BASE_ID);

        var baseTypeFiled = new DataFieldModel(model, "BaseType", DataFieldType.Byte, false, false);
        model.addSysMember(baseTypeFiled, Orgunit.BASE_TYPE_ID);

        //var Base = new EntityRefModel(model, "Base",
        //        new List<ulong>() { Consts.SYS_ENTERPRISE_MODEL_ID, Consts.SYS_WORKGROUP_MODEL_ID, Consts.SYS_EMPLOEE_MODEL_ID },
        //        new ushort[] { baseId.MemberId }, baseType.MemberId);
        //model.AddSysMember(Base, Consts.ORGUNIT_BASE_ID);
        return model;
    }

    private static EntityModel createWorkgroupModel() throws Exception{
        var model = new EntityModel(IdUtil.SYS_WORKGROUP_MODEL_ID, "Workgroup");
        model.bindToSysStore(true, false);

        var nameFiled = new DataFieldModel(model, "Name", DataFieldType.String, false, false);
        nameFiled.setLength(50);
        model.addSysMember(nameFiled, Workgroup.NAME_ID);

        //indexes
        var ui_nodeType_targetId = new SysIndexModel(model, "UI_NodeType_TargetId", false,
                new FieldWithOrder[]
                        {
                                new FieldWithOrder(Workgroup.CHECKOUT_NODETYPE_ID),
                                new FieldWithOrder(Workgroup.CHECKOUT_TARGETID_ID)
                        },new short[]{Workgroup.CHECKOUT_NODETYPE_ID, Workgroup.CHECKOUT_TARGETID_ID});
        model.sysStoreOptions().addSysIndex(model, ui_nodeType_targetId, (byte) ((1 << IdUtil.INDEXID_UNIQUE_OFFSET) | (1 << 2)));
        return model;
    }

    private static CompletableFuture<ApplicationModel> createAppAsync() {
        var app = new ApplicationModel("appbox", "sys");

        return ModelStore.createApplicationAsync(app).thenApply(appStoreId -> {
            app.setAppStoreId(appStoreId);
            return app;
        });
    }

    private static EntityModel createEmployeeModel() throws Exception {
        var model = new EntityModel(IdUtil.SYS_EMPLOYEE_MODEL_ID, "Emploee");
        model.bindToSysStore(true, false);

        //Members
        var name = new DataFieldModel(model, "Name", DataFieldType.String, false, false);
        model.addSysMember(name, Employee.NAME_ID);
        var male = new DataFieldModel(model, "Male", DataFieldType.Bool, false, false);
        model.addSysMember(male, Employee.MALE_ID);
        var birthday = new DataFieldModel(model, "Birthday", DataFieldType.DateTime, false, false);
        model.addSysMember(birthday, Employee.BIRTHDAY_ID);
        var account = new DataFieldModel(model, "Account", DataFieldType.String, true, false);
        model.addSysMember(account, Employee.ACCOUNT_ID);
        var password = new DataFieldModel(model, "Password", DataFieldType.Binary, true, false);
        model.addSysMember(password, Employee.PASSWORD_ID);

        //TODO:
        //var orgunits = new EntitySetModel(model, "OrgUnits", IdUtil.SYS_ORGUNIT_MODEL_ID, Consts.ORGUNIT_BASE_ID);
        //model.AddSysMember(orgunits, Consts.EMPLOEE_ORGUNITS_ID);

        //Indexes
        var ui_account = new SysIndexModel(model, "UI_Account", true,
                new FieldWithOrder[]{new FieldWithOrder(Employee.ACCOUNT_ID)},
                new short[]{Employee.PASSWORD_ID});
        model.sysStoreOptions().addSysIndex(model, ui_account, (byte) ((1 << IdUtil.INDEXID_UNIQUE_OFFSET) | (1 << 2)));

        return model;
    }

    private static EntityModel createEnterpriseModel() throws Exception {
        var model = new EntityModel(IdUtil.SYS_ENTERPRISE_MODEL_ID, "Enterprise");
        model.bindToSysStore(true, false);

        //Members
        var name = new DataFieldModel(model, "Name", DataFieldType.String, false, false);
        model.addSysMember(name, Enterprise.NAME_ID);
        var address = new DataFieldModel(model, "Address", DataFieldType.String, true, false);
        model.addSysMember(address, Enterprise.ADDRESS_ID);

        return model;
    }

    private static CompletableFuture<Boolean> createServiceModel(String name, long idIndex, UUID folderId, KVTransaction txn) {
        var modelId = ((long) IdUtil.SYS_APP_ID << IdUtil.MODELID_APPID_OFFSET)
                | ((long) ModelType.Service.value << IdUtil.MODELID_TYPE_OFFSET)
                | (idIndex << IdUtil.MODELID_SEQ_OFFSET);
        var model = new ServiceModel(modelId, name);
        model.setFolderId(folderId);

        //TODO:添加依赖项
        return ModelStore.insertModelAsync(model, txn).thenCompose(r -> {
            try {
                var codeStream = StoreInitiator.class.getResourceAsStream(String.format("/services/%s.java", name));
                var utf8Data   = codeStream.readAllBytes();
                codeStream.close();
                var codeData = ModelCodeUtil.encodeServiceCodeData(utf8Data, false);
                return ModelStore.upsertModelCodeAsync(modelId, codeData, txn);
            } catch (IOException e) {
                return CompletableFuture.failedFuture(e);
            }
        }).thenCompose(r -> {
            //TODO:继续处理编译好的类库
            return CompletableFuture.completedFuture(true);
        });
    }

    /** insert default entities */
    private static CompletableFuture<Void> insertEntities(KVTransaction txn) {
        var defaultEnterprise = new Enterprise();
        defaultEnterprise.setName("AppBoxFuture");
        return EntityStore.insertEntityAsync(defaultEnterprise, txn);
    }

}
