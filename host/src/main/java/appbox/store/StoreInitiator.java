package appbox.store;

import appbox.entities.*;
import appbox.logging.Log;
import appbox.model.*;
import appbox.model.entity.*;
import appbox.runtime.RuntimeContext;
import appbox.server.runtime.HostRuntimeContext;
import appbox.store.utils.ModelCodeUtil;
import appbox.utils.IdUtil;

import static appbox.model.entity.DataFieldModel.DataFieldType;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
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
                //新建模型文件夹
                var entityRootFolder     = new ModelFolder(app.id(), ModelType.Entity);
                var entityOrgUnitsFolder = new ModelFolder(entityRootFolder, "OrgUnits");
                var entityDesignFolder   = new ModelFolder(entityRootFolder, "Design");

                var viewRootFolder     = new ModelFolder(app.id(), ModelType.View);
                var viewOrgUnitsFolder = new ModelFolder(viewRootFolder, "OrgUnits");
                var viewOpsFolder      = new ModelFolder(viewRootFolder, "OPS");
                var viewMetricsFolder  = new ModelFolder(viewRootFolder, "Metrics");
                var viewClusterFolder  = new ModelFolder(viewRootFolder, "Cluster");

                //新建实体模型
                var enterpriseModel = createEnterpriseModel();
                enterpriseModel.setFolderId(entityOrgUnitsFolder.id());
                var employeeModel = createEmployeeModel();
                employeeModel.setFolderId(entityOrgUnitsFolder.id());
                var workgroupModel = createWorkgroupModel();
                workgroupModel.setFolderId(entityOrgUnitsFolder.id());
                var orgunitModel = createOrgUnitModel();
                orgunitModel.setFolderId(entityOrgUnitsFolder.id());

                var stagedModel = createStagedModel();
                stagedModel.setFolderId(entityDesignFolder.id());
                var checkoutModel = createCheckoutModel();
                checkoutModel.setFolderId(entityDesignFolder.id());

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
                        .thenCompose(txn -> ModelStore.upsertFolderAsync(entityRootFolder, txn)
                                .thenCompose(r -> ModelStore.upsertFolderAsync(viewRootFolder, txn))
                                .thenCompose(r -> ModelStore.insertModelAsync(employeeModel, txn))
                                .thenCompose(r -> ModelStore.insertModelAsync(enterpriseModel, txn))
                                .thenCompose(r -> ModelStore.insertModelAsync(workgroupModel, txn))
                                .thenCompose(r -> ModelStore.insertModelAsync(orgunitModel, txn))
                                .thenCompose(r -> ModelStore.insertModelAsync(stagedModel, txn))
                                .thenCompose(r -> ModelStore.insertModelAsync(checkoutModel, txn))
                                .thenCompose(r -> createServiceModel("OrgUnitService", 1, null, txn))
                                .thenCompose(r -> createViewModel("Home", 1, null, txn, null))
                                .thenCompose(r -> createViewModel("PermissionTree", 2, viewOrgUnitsFolder.id(), txn, null))
                                .thenCompose(r -> createViewModel("EnterpriseView", 3, viewOrgUnitsFolder.id(), txn, null))
                                .thenCompose(r -> createViewModel("WorkgroupView", 4, viewOrgUnitsFolder.id(), txn, null))
                                .thenCompose(r -> createViewModel("EmployeeView", 5, viewOrgUnitsFolder.id(), txn, null))
                                .thenCompose(r -> createViewModel("OrgUnits", 6, viewOrgUnitsFolder.id(), txn, null))
                                .thenCompose(r -> insertEntities(txn))
                                .thenCompose(list -> createPermissionModels(txn, list))
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

    private static EntityModel createCheckoutModel() {
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

    private static EntityModel createStagedModel() {
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

    private static EntityModel createOrgUnitModel() {
        var model = new EntityModel(IdUtil.SYS_ORGUNIT_MODEL_ID, "OrgUnit");
        model.bindToSysStore(true, false);

        var name = new DataFieldModel(model, "Name", DataFieldType.String, false);
        name.setLength(100);
        model.addSysMember(name, OrgUnit.NAME_ID);

        var baseId = new DataFieldModel(model, "BaseId", DataFieldType.EntityId, false, true);
        model.addSysMember(baseId, OrgUnit.BASEID_ID);

        var baseType = new DataFieldModel(model, "BaseType", DataFieldType.Long, false, true);
        model.addSysMember(baseType, OrgUnit.BASE_TYPE_ID);

        List<Long> refModelIds = new ArrayList<Long>() {{
            add(IdUtil.SYS_ENTERPRISE_MODEL_ID);
            add(IdUtil.SYS_WORKGROUP_MODEL_ID);
            add(IdUtil.SYS_EMPLOYEE_MODEL_ID);
        }};
        var base = new EntityRefModel(model, "Base", refModelIds,
                new short[]{baseId.memberId()}, baseType.memberId(), true);
        model.addSysMember(base, OrgUnit.BASE_ID);

        var parentId = new DataFieldModel(model, "ParentId", DataFieldType.EntityId, true, true);
        model.addSysMember(parentId, OrgUnit.PARENTID_ID);

        var parent = new EntityRefModel(model, "Parent", IdUtil.SYS_ORGUNIT_MODEL_ID,
                new short[]{parentId.memberId()}, true);
        parent.setAllowNull(true);
        model.addSysMember(parent, OrgUnit.PARENT_ID);

        var childs = new EntitySetModel(model, "Childs", IdUtil.SYS_ORGUNIT_MODEL_ID, parent.memberId());
        model.addSysMember(childs, OrgUnit.CHILDS_ID);

        return model;
    }

    private static EntityModel createWorkgroupModel() {
        var model = new EntityModel(IdUtil.SYS_WORKGROUP_MODEL_ID, "Workgroup");
        model.bindToSysStore(true, false);

        var nameFiled = new DataFieldModel(model, "Name", DataFieldType.String, false);
        nameFiled.setLength(50);
        model.addSysMember(nameFiled, Workgroup.NAME_ID);

        return model;
    }

    private static CompletableFuture<ApplicationModel> createAppAsync() {
        var app = new ApplicationModel("appbox", "sys");

        return ModelStore.createApplicationAsync(app).thenApply(appStoreId -> {
            app.setAppStoreId(appStoreId);
            return app;
        });
    }

    private static EntityModel createEmployeeModel() {
        var model = new EntityModel(IdUtil.SYS_EMPLOYEE_MODEL_ID, "Employee");
        model.bindToSysStore(true, false);

        //Members
        var name = new DataFieldModel(model, "Name", DataFieldType.String, false);
        model.addSysMember(name, Employee.NAME_ID);
        var male = new DataFieldModel(model, "Male", DataFieldType.Bool, false);
        model.addSysMember(male, Employee.MALE_ID);
        var birthday = new DataFieldModel(model, "Birthday", DataFieldType.DateTime, true);
        model.addSysMember(birthday, Employee.BIRTHDAY_ID);
        var account = new DataFieldModel(model, "Account", DataFieldType.String, true);
        model.addSysMember(account, Employee.ACCOUNT_ID);
        var password = new DataFieldModel(model, "Password", DataFieldType.Binary, true);
        model.addSysMember(password, Employee.PASSWORD_ID);

        //TODO:
        //var orgunits = new EntitySetModel(model, "OrgUnits", IdUtil.SYS_ORGUNIT_MODEL_ID, Consts.ORGUNIT_BASE_ID);
        //model.AddSysMember(orgunits, Consts.EMPLOEE_ORGUNITS_ID);

        //Indexes
        var ui_account = new SysIndexModel(model, "UI_Account", true,
                new FieldWithOrder[]{new FieldWithOrder(Employee.ACCOUNT_ID)},
                new short[]{Employee.PASSWORD_ID});
        model.sysStoreOptions().addSysIndex(model, ui_account, Employee.UI_Account.INDEXID);

        return model;
    }

    private static EntityModel createEnterpriseModel() {
        var model = new EntityModel(IdUtil.SYS_ENTERPRISE_MODEL_ID, "Enterprise");
        model.bindToSysStore(true, false);

        //Members
        var name = new DataFieldModel(model, "Name", DataFieldType.String, false, false);
        model.addSysMember(name, Enterprise.NAME_ID);
        var address = new DataFieldModel(model, "Address", DataFieldType.String, true, false);
        model.addSysMember(address, Enterprise.ADDRESS_ID);

        return model;
    }

    private static CompletableFuture<Void> createServiceModel(
            String name, long idIndex, UUID folderId, KVTransaction txn) {
        var modelId = ((long) IdUtil.SYS_APP_ID << IdUtil.MODELID_APPID_OFFSET)
                | ((long) ModelType.Service.value << IdUtil.MODELID_TYPE_OFFSET)
                | (idIndex << IdUtil.MODELID_SEQ_OFFSET);
        var model = new ServiceModel(modelId, name);
        model.setFolderId(folderId);

        //TODO:添加依赖项
        return ModelStore.insertModelAsync(model, txn).thenCompose(r -> {
            try {
                var codeStream = getResourceStream("services", name, "java");
                var utf8Data   = codeStream.readAllBytes();
                codeStream.close();
                var codeData = ModelCodeUtil.encodeServiceCodeData(utf8Data, false);
                return ModelStore.upsertModelCodeAsync(modelId, codeData, txn);
            } catch (IOException e) {
                return CompletableFuture.failedFuture(e);
            }
        }).thenCompose(r -> {
            try {
                var asmStream = getResourceStream("services", name, "bin");
                var asmData   = asmStream.readAllBytes();

                return ModelStore.upsertAssemblyAsync(true, "sys." + name, asmData, txn);
            } catch (Exception ex) {
                return CompletableFuture.failedFuture(ex);
            }
        });
    }

    private static CompletableFuture<Void> createViewModel(
            String name, long idIndex, UUID folderId, KVTransaction txn, String routePath) {
        var modelId = ((long) IdUtil.SYS_APP_ID << IdUtil.MODELID_APPID_OFFSET)
                | ((long) ModelType.View.value << IdUtil.MODELID_TYPE_OFFSET)
                | (idIndex << IdUtil.MODELID_SEQ_OFFSET);
        var model = new ViewModel(modelId, name, ViewModel.TYPE_VUE);
        model.setFolderId(folderId);

        return ModelStore.insertModelAsync(model, txn).thenCompose(r -> {
            if (!(routePath == null || routePath.isEmpty())) {
                model.setFlag(ViewModel.FLAG_ROUTE);
                model.setRoutePath(routePath);
                var viewName = "sys." + model.name();
                return ModelStore.upsertViewRouteAsync(viewName, model.getRoutePath(), txn);
            }
            return CompletableFuture.completedFuture(null);
        }).thenCompose(r -> {
            try {
                var    templateStream = getResourceStream("views", name, "html");
                var    templateCode   = new String(templateStream.readAllBytes(), StandardCharsets.UTF_8);
                var    scriptStream   = getResourceStream("views", name, "js");
                var    scriptCode     = new String(scriptStream.readAllBytes(), StandardCharsets.UTF_8);
                var    styleStream    = getResourceStream("views", name, "css");
                String styleCode      = null;
                if (styleStream != null)
                    styleCode = new String(styleStream.readAllBytes(), StandardCharsets.UTF_8);
                var codeData = ModelCodeUtil.encodeViewCode(templateCode, scriptCode, styleCode);

                return ModelStore.upsertModelCodeAsync(modelId, codeData, txn);
            } catch (Exception ex) {
                return CompletableFuture.failedFuture(ex);
            }
        }).thenCompose(r -> {
            try {
                var asmStream = getResourceStream("views", name, "bin");
                var asmData   = asmStream.readAllBytes();

                return ModelStore.upsertAssemblyAsync(false, "sys." + name, asmData, txn);
            } catch (Exception ex) {
                return CompletableFuture.failedFuture(ex);
            }
        });
    }

    private static InputStream getResourceStream(String folder, String name, String ext) {
        var path = String.format("/%s/%s.%s", folder, name, ext);
        return StoreInitiator.class.getResourceAsStream(path);
    }

    /** insert default entities */
    private static CompletableFuture<List<OrgUnit>> insertEntities(KVTransaction txn) {
        var list = new ArrayList<OrgUnit>(); //用于返回设置权限模型

        //新建默认组织
        var defaultEnterprise = new Enterprise();
        defaultEnterprise.setName("AppBoxFuture");
        //新建默认系统管理员及测试账号
        var admin = new Employee();
        admin.setName("Admin");
        admin.setAccount("Admin");
        admin.setPassword(RuntimeContext.current().passwordHasher().hashPassword("760wb"));
        admin.setMale(true);
        admin.setBirthday(LocalDateTime.of(1977, 1, 27, 8, 8));

        var test = new Employee();
        test.setName("Test");
        test.setAccount("Test");
        test.setPassword(RuntimeContext.current().passwordHasher().hashPassword("la581"));
        test.setMale(false);
        test.setBirthday(LocalDateTime.of(1979, 12, 4, 8, 8));
        //新建默认组织单元
        var itdept = new Workgroup();
        itdept.setName("IT Dept");

        var entou = new OrgUnit();
        entou.setName(defaultEnterprise.getName());
        entou.setBaseType(IdUtil.SYS_ENTERPRISE_MODEL_ID);
        entou.setBaseId(defaultEnterprise.id());

        var itdeptou = new OrgUnit();
        itdeptou.setName(itdept.getName());
        itdeptou.setBaseType(IdUtil.SYS_WORKGROUP_MODEL_ID);
        itdeptou.setBaseId(itdept.id());
        itdeptou.setParent(entou);
        list.add(itdeptou);

        var adminou = new OrgUnit();
        adminou.setName(admin.getName());
        adminou.setBaseId(admin.id());
        adminou.setBaseType(IdUtil.SYS_EMPLOYEE_MODEL_ID);
        adminou.setParent(itdeptou);
        list.add(adminou);

        var testou = new OrgUnit();
        testou.setName(test.getName());
        testou.setBaseId(test.id());
        testou.setBaseType(IdUtil.SYS_EMPLOYEE_MODEL_ID);
        testou.setParent(itdeptou);

        return EntityStore.insertEntityAsync(defaultEnterprise, txn)
                .thenCompose(r -> EntityStore.insertEntityAsync(admin, txn))
                .thenCompose(r -> EntityStore.insertEntityAsync(test, txn))
                .thenCompose(r -> EntityStore.insertEntityAsync(itdept, txn))
                .thenCompose(r -> EntityStore.insertEntityAsync(entou, txn))
                .thenCompose(r -> EntityStore.insertEntityAsync(itdeptou, txn))
                .thenCompose(r -> EntityStore.insertEntityAsync(adminou, txn))
                .thenCompose(r -> EntityStore.insertEntityAsync(testou, txn))
                .thenApply(r -> list);
    }

    /** 创建默认权限模型 */
    private static CompletableFuture<Void> createPermissionModels(KVTransaction txn, List<OrgUnit> ous) {
        var admin = new PermissionModel(IdUtil.SYS_PERMISSION_ADMIN_ID, "Admin");
        admin.setRemark("System administrator");
        admin.orgUnits().add(ous.get(1).id());

        var developer = new PermissionModel(IdUtil.SYS_PERMISSION_DEVELOPER_ID, "Developer");
        developer.setRemark("System developer");
        developer.orgUnits().add(ous.get(0).id());

        return ModelStore.insertModelAsync(admin, txn)
                .thenCompose(r -> ModelStore.insertModelAsync(developer, txn));
    }

}
