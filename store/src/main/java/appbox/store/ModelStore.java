package appbox.store;

import appbox.channel.messages.*;
import appbox.logging.Log;
import appbox.model.*;
import appbox.utils.IdUtil;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/** 模型存储实现 */
public final class ModelStore {

    // ManagedBlocker示例
    //class BlockingGetUnicorns implements ForkJoinPool.ManagedBlocker {
    //    List<String> unicorns;
    //    public boolean block() {
    //        unicorns = unicornService.getUnicorns();
    //        return true;
    //    }
    //    public boolean isReleasable() { return false; }
    //}
    //CompletableFuture<List<String>> fetchUnicorns  =
    //        CompletableFuture.supplyAsync(() -> {
    //            BlockingGetUnicorns getThem = new BlockingGetUnicorns();
    //            try {
    //                ForkJoinPool.managedBlock(getThem);
    //            } catch (InterruptedException ex) {
    //                throw new AssertionError();
    //            }
    //            return getThem.unicorns;
    //        });


    /**
     * 创建新的应用，成功返回应用对应的存储Id
     */
    public static CompletableFuture<Byte> createApplicationAsync(ApplicationModel app) {
        return SysStoreApi.metaNewAppAsync(app).thenApply(r -> {
            r.checkStoreError();
            return r.appId;
        });
    }

    public static CompletableFuture<Long> genModelIdAsync(int appId, ModelType type, ModelLayer layer) {
        if (layer == ModelLayer.SYS)
            throw new UnsupportedOperationException();

        return SysStoreApi.metaGenModelIdAsync(appId, layer == ModelLayer.DEV)
                .thenApply(res -> {
                    res.checkStoreError();

                    int  seq = res.modelId;
                    long nid = ((long) appId) << IdUtil.MODELID_APPID_OFFSET;
                    nid |= ((long) type.value) << IdUtil.MODELID_TYPE_OFFSET;
                    nid |= ((long) seq) << IdUtil.MODELID_SEQ_OFFSET;
                    nid |= layer.value;
                    return nid;
                });
    }

    public static CompletableFuture<Void> insertModelAsync(ModelBase model, KVTransaction txn) {
        var req = new KVInsertModelRequire(txn.id(), model);
        return SysStoreApi.execKVInsertAsync(req)
                .thenAccept(StoreResponse::checkStoreError)
                .whenComplete((r, ex) -> txn.rollbackOnException(ex));
    }

    public static CompletableFuture<Void> updateModelAsync(ModelBase model, KVTransaction txn,
                                                           Function<Integer, ApplicationModel> getApp) {
        //TODO:考虑先处理变更项但不提议变更命令，再保存AcceptChanges后的模型数据，最后事务提议变更命令
        model.increaseVersion(); //先增加模型版本号
        var req = new KVUpdateModelRequest(txn.id(), model);
        return SysStoreApi.execKVUpdateAsync(req)
                .thenAccept(StoreResponse::checkStoreError)
                .whenComplete((r, ex) -> txn.rollbackOnException(ex))
                .thenCompose(r -> updateEntityModelAsync(model, txn));
    }

    private static CompletableFuture<Void> updateEntityModelAsync(ModelBase model, KVTransaction txn) {
        if (model.modelType() != ModelType.Entity)
            return CompletableFuture.completedFuture(null);

        var entiyModel = (EntityModel) model;
        //判断是否系统存储且结构已发生变更(Schema Changed)
        if (entiyModel.sysStoreOptions() != null && entiyModel.sysStoreOptions().hasSchemaChanged()) {
            Log.debug(String.format("Entity[%s] schema changed, %d -> %d", entiyModel.name()
                    , entiyModel.sysStoreOptions().oldSchemaVersion()
                    , entiyModel.sysStoreOptions().schemaVersion()));
            throw new RuntimeException("未实现");
        }

        return CompletableFuture.completedFuture(null);
    }

    public static CompletableFuture<Void> deleteModelAsync(ModelBase model, KVTransaction txn,
                                                           Function<Integer, ApplicationModel> getApp) {
        if (model.modelType() == ModelType.Entity && ((EntityModel) model).sysStoreOptions() != null) {
            throw new RuntimeException("未实现");
        }

        //非系统存储的实体模型直接删除原数据
        var req = new KVDeleteModelRequest(txn.id(), model.id());
        return SysStoreApi.execKVDeleteAsync(req)
                .thenAccept(StoreResponse::checkStoreError)
                .whenComplete((r, ex) -> txn.rollbackOnException(ex));
    }

    public static CompletableFuture<Void> upsertFolderAsync(ModelFolder folder, KVTransaction txn) {
        var req = new KVInsertFolderRequest(folder, txn.id());
        return SysStoreApi.execKVInsertAsync(req)
                .thenAccept(StoreResponse::checkStoreError)
                .whenComplete((r, ex) -> txn.rollbackOnException(ex));
    }

    //region ====模型代码及Assembly相关操作====

    /**
     * 保存模型相关的代码，目前主要用于服务模型及视图模型
     * @param codeData 已经压缩编码过
     */
    public static CompletableFuture<Void> upsertModelCodeAsync(long modelId, byte[] codeData, KVTransaction txn) {
        var req = new KVInsertModelCodeRequire(txn.id(), modelId, codeData);
        return SysStoreApi.execKVInsertAsync(req)
                .thenAccept(StoreResponse::checkStoreError)
                .whenComplete((r, ex) -> txn.rollbackOnException(ex));
    }

    public static CompletableFuture<Void> deleteModelCodeAsync(long modelId, KVTransaction txn) {
        var req = new KVDeleteModelCodeRequest(txn.id(), modelId);
        return SysStoreApi.execKVDeleteAsync(req)
                .thenAccept(StoreResponse::checkStoreError)
                .whenComplete((r, ex) -> txn.rollbackOnException(ex));
    }

    /**
     * 保存编译好的服务组件或视图运行时代码
     * @param asmName eg: sys.HelloService or sys.CustomerView
     */
    public static CompletableFuture<Void> upsertAssemblyAsync(
            boolean isService, String asmName, byte[] asmData, KVTransaction txn) {
        var req = new KVInsertAssemblyRequest(txn.id(), asmName, asmData, isService);
        return SysStoreApi.execKVInsertAsync(req)
                .thenAccept(StoreResponse::checkStoreError)
                .whenComplete((r, ex) -> txn.rollbackOnException(ex));
    }

    public static CompletableFuture<Void> deleteAssemblyAsync(boolean isService, String asmName, KVTransaction txn) {
        var req = new KVDeleteAssemblyRequest(txn.id(), asmName, isService);
        return SysStoreApi.execKVDeleteAsync(req)
                .thenAccept(StoreResponse::checkStoreError)
                .whenComplete((r, ex) -> txn.rollbackOnException(ex));
    }

    /** 仅用于加载服务模型的代码 */
    public static CompletableFuture<ServiceCode> loadServiceCodeAsync(long modelId) {
        var req = new KVGetModelCodeRequest(modelId);
        return SysStoreApi.execKVGetAsync(req, new KVGetModelCodeResponse())
                .thenApply(r -> (ServiceCode) r.sourceCode);
    }

    /**
     * 加载编译好的压缩的服务组件的字节码
     * @param asmName eg:sys.HelloService
     */
    public static CompletableFuture<byte[]> loadServiceAssemblyAsync(String asmName) {
        var req = new KVGetAssemblyRequest(true, asmName);
        return SysStoreApi.execKVGetAsync(req, new KVGetAssemblyResponse())
                .thenApply(KVGetAssemblyResponse::getAssemblyData);
    }

    public static CompletableFuture<ViewCode> loadViewCodeAsync(long modelId) {
        var req = new KVGetModelCodeRequest(modelId);
        return SysStoreApi.execKVGetAsync(req, new KVGetModelCodeResponse())
                .thenApply(r -> (ViewCode) r.sourceCode);
    }

    //endregion

    //region ====视图模型路由相关====

    /**
     * 保存视图模型的路由信息
     * @param viewName eg: sys.CustomerList
     * @param path     无自定义路由为空，有上级路由;分隔，eg: userInfo;address
     */
    public static CompletableFuture<Void> upsertViewRouteAsync(String viewName, String path, KVTransaction txn) {
        var req = new KVInsertViewRouteRequest(viewName, path, txn.id());
        return SysStoreApi.execKVInsertAsync(req)
                .thenAccept(StoreResponse::checkStoreError)
                .whenComplete((r, ex) -> txn.rollbackOnException(ex));
    }

    public static CompletableFuture<Void> deleteViewRouteAsync(String viewName, KVTransaction txn) {
        var req = new KVDeleteViewRouteRequest(txn.id(), viewName);
        return SysStoreApi.execKVDeleteAsync(req)
                .thenAccept(StoreResponse::checkStoreError)
                .whenComplete((r, ex) -> txn.rollbackOnException(ex));
    }

    //endregion

    //region ====Read Methods====

    /** 用于设计时加载所有ApplicationModel */
    public static CompletableFuture<ApplicationModel[]> loadAllApplicationAsync() {
        var req = new KVScanAppsRequest();
        return SysStoreApi.execKVScanAsync(req, new KVScanAppsResponse())
                .thenApply(r -> r.apps);
    }

    /** 用于运行时加载单个应用模型 */
    public static CompletableFuture<ApplicationModel> loadApplicationAsync(int appId) {
        var req = new KVGetApplicationRequest(appId);
        return SysStoreApi.execKVGetAsync(req, new KVGetApplicationResponse())
                .thenApply(KVGetApplicationResponse::getApplicationModel);
    }

    /** 用于设计时加载所有ModelFolder */
    public static CompletableFuture<ModelFolder[]> loadAllFolderAsync() {
        var req = new KVScanFoldersRequest();
        return SysStoreApi.execKVScanAsync(req, new KVScanFoldersResponse())
                .thenApply(r -> r.folders);
    }

    /** 用于设计时加载所有Model */
    public static CompletableFuture<ModelBase[]> loadAllModelAsync() {
        var req = new KVScanModelsRequest();
        return SysStoreApi.execKVScanAsync(req, new KVScanModelsResponse())
                .thenApply(r -> r.models);
    }

    /** 用于运行时加载单个模型 */
    public static CompletableFuture<ModelBase> loadModelAsync(long modelId) {
        var req = new KVGetModelRequest(modelId);
        return SysStoreApi.execKVGetAsync(req, new KVGetModelResponse())
                .thenApply(KVGetModelResponse::getModel);
    }
    //endregion

}
