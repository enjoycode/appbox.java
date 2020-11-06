package appbox.store;

import appbox.entities.Enterprise;
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
                var emploeeModel    = createEmploeeModel();
                //将新建的模型加入运行时上下文
                var ctx = (HostRuntimeContext) RuntimeContext.current();
                ctx.injectApplication(app);
                ctx.injectModel(enterpriseModel);
                ctx.injectModel(emploeeModel);

                //开始事务保存
                return KVTransaction.beginAsync()
                        .thenCompose(txn -> ModelStore.insertModelAsync(emploeeModel, txn)
                                .thenCompose(r -> ModelStore.insertModelAsync(enterpriseModel, txn))
                                .thenCompose(r -> createServiceModel("TestService", 1, null, txn))
                                .thenCompose(r -> insertEntities(txn))
                                .thenApply(r -> txn.commitAsync())
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

    private static CompletableFuture<ApplicationModel> createAppAsync() {
        var app = new ApplicationModel("appbox", "sys");

        return ModelStore.createApplicationAsync(app).thenApply(appStoreId -> {
            app.setAppStoreId(appStoreId);
            return app;
        });
    }

    private static EntityModel createEmploeeModel() throws Exception {
        var nameId     = (short) (1 << IdUtil.MEMBERID_SEQ_OFFSET);
        var maleId     = (short) (2 << IdUtil.MEMBERID_SEQ_OFFSET);
        var birthdayId = (short) (3 << IdUtil.MEMBERID_SEQ_OFFSET);
        var accountId  = (short) (4 << IdUtil.MEMBERID_SEQ_OFFSET);
        var passwordId = (short) (5 << IdUtil.MEMBERID_SEQ_OFFSET);

        var model = new EntityModel(IdUtil.SYS_EMPLOEE_MODEL_ID, "Emploee", true, false);

        //Members
        var name = new DataFieldModel(model, "Name", DataFieldType.String, false, false);
        model.addSysMember(name, nameId);
        var male = new DataFieldModel(model, "Male", DataFieldType.Bool, false, false);
        model.addSysMember(male, maleId);
        var birthday = new DataFieldModel(model, "Birthday", DataFieldType.DateTime, false, false);
        model.addSysMember(birthday, birthdayId);
        var account = new DataFieldModel(model, "Account", DataFieldType.String, true, false);
        model.addSysMember(account, accountId);
        var password = new DataFieldModel(model, "Password", DataFieldType.Binary, true, false);
        model.addSysMember(password, passwordId);

        //TODO:
        //var orgunits = new EntitySetModel(model, "OrgUnits", IdUtil.SYS_ORGUNIT_MODEL_ID, Consts.ORGUNIT_BASE_ID);
        //model.AddSysMember(orgunits, Consts.EMPLOEE_ORGUNITS_ID);

        //Indexes
        var ui_account = new SysIndexModel(model, "UI_Account", true,
                new FieldWithOrder[]{new FieldWithOrder(accountId)},
                new short[]{passwordId});
        model.sysStoreOptions().addSysIndex(model, ui_account, (byte) ((1 << IdUtil.INDEXID_UNIQUE_OFFSET) | (1 << 2)));

        return model;
    }

    private static EntityModel createEnterpriseModel() throws Exception {
        var model = new EntityModel(IdUtil.SYS_ENTERPRISE_MODEL_ID, "Enterprise", true, false);

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
