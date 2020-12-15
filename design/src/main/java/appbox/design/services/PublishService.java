package appbox.design.services;

import appbox.compression.BrotliUtil;
import appbox.data.PersistentState;
import appbox.design.DesignHub;
import appbox.design.common.PublishPackage;
import appbox.design.jdt.JavaBuilderWrapper;
import appbox.design.services.code.LanguageServer;
import appbox.design.services.code.ServiceCodeGenerator;
import appbox.design.services.code.TypeSystem;
import appbox.logging.Log;
import appbox.model.*;
import appbox.runtime.RuntimeContext;
import appbox.serialization.BytesOutputStream;
import appbox.store.DbTransaction;
import appbox.store.KVTransaction;
import appbox.store.ModelStore;
import appbox.store.SqlStore;
import org.eclipse.core.internal.resources.BuildConfiguration;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.ls.core.internal.JDTUtils;
import org.eclipse.jface.text.Document;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public final class PublishService {

    private PublishService() {}

    public static void validateModels(DesignHub hub, PublishPackage pkg) {
        //TODO:
    }

    public static void compileModels(DesignHub hub, PublishPackage pkg) throws Exception {
        for (var item : hub.pendingChanges) {
            if (item instanceof ServiceModel && ((ServiceModel) item).persistentState() != PersistentState.Deleted) {
                var serviceModel = (ServiceModel) item;
                var asmData      = compileService(hub, serviceModel, null);
                var appName      = hub.designTree.findApplicationNode(serviceModel.appId()).model.name();
                var fullName     = String.format("%s.%s", appName, serviceModel.name());
                //重命名的已不再需要加入待删除列表，保存模型时已处理
                pkg.serviceAssemblies.put(fullName, asmData);
            }
        }
    }

    /**
     * 发布或调试时编译服务模型
     * @return 返回的是已经压缩过的
     */
    public static byte[] compileService(DesignHub hub, ServiceModel model, String debugFolder) throws Exception {
        //获取对应的虚拟文件
        var designNode = hub.designTree.findModelNode(ModelType.Service, model.id());
        var appName    = designNode.appNode.model.name();
        var vfile      = hub.typeSystem.findFileForServiceModel(appName, model.name());
        var cu         = JDTUtils.resolveCompilationUnit(vfile);

        var astParser = ASTParser.newParser(AST.JLS15);
        astParser.setSource(cu);
        astParser.setResolveBindings(true);
        //astParser.setStatementsRecovery(true);
        var astNode = astParser.createAST(null);

        //检测虚拟代码错误
        var problems = ((CompilationUnit) astNode).getProblems();
        if (problems != null && problems.length > 0) {
            //TODO:友好提示及忽略警告
            throw new RuntimeException("Has problems.");
        }

        //开始转换编译服务模型的运行时代码
        var astRewrite           = ASTRewrite.create(astNode.getAST());
        var serviceCodeGenerator = new ServiceCodeGenerator(hub, appName, model, astRewrite);
        astNode.accept(serviceCodeGenerator);
        serviceCodeGenerator.finish();

        var edits  = astRewrite.rewriteAST();
        var newdoc = new Document(cu.getSource());
        edits.apply(newdoc);

        var runtimeCode       = newdoc.get();
        var runtimeCodeStream = new ByteArrayInputStream(runtimeCode.getBytes(StandardCharsets.UTF_8));

        //生成运行时临时Project并进行编译
        var libs = new IClasspathEntry[]{
                JavaCore.newLibraryEntry(TypeSystem.libAppBoxCorePath, null, null),
                JavaCore.newLibraryEntry(TypeSystem.libAppBoxStorePath, null, null)
        };
        var runtimeProject =
                hub.typeSystem.languageServer.createProject(
                        "runtime_" + Long.toUnsignedString(model.id()), libs);
        var runtimeFile = runtimeProject.getFile(vfile.getName());
        runtimeFile.create(runtimeCodeStream, true, null);

        var config  = new BuildConfiguration(runtimeProject);
        var builder = new JavaBuilderWrapper(config);
        builder.build();

        //获取并压缩编译好的.class
        var    classFolder = runtimeProject.getFolder(LanguageServer.BUILD_OUTPUT);
        var    classFiles  = classFolder.members();
        var    outStream   = new BytesOutputStream(2048);
        byte[] classData   = null;

        outStream.writeVariant(classFiles.length); //.class文件数
        for (var classFile : classFiles) {
            var className = classFile.getName().replace(".class", "");
            outStream.writeString(className);
            outStream.writeByteArray(Files.readAllBytes(classFile.getLocation().toFile().toPath()));
            classFile.delete(true, null);
        }

        classData = BrotliUtil.compress(outStream.getBuffer(), 0, outStream.size());

        //删除用于编译的临时Project及运行时服务代码
        runtimeFile.delete(true, null);
        runtimeProject.delete(true, null);

        return classData;
    }

    /**
     * 1. 保存模型(包括编译好的服务Assembly)，并生成EntityModel的SchemaChangeJob;
     * 2. 通知集群各节点更新缓存;
     * 3. 删除当前会话的CheckoutInfo;
     * 4. 刷新DesignTree相应的节点，并删除挂起
     * 5. 保存递交日志
     */
    public static CompletableFuture<Void> publishAsync(DesignHub hub, PublishPackage pkg, String commitMsg) {
        //先根据依赖关系排序
        pkg.sortAllModels();

        //注意目前实现无法保证第三方数据库与内置模型存储的一致性,第三方数据库发生异常只能手动清理
        //TODO:或考虑记录第三方数据库所有事务命令,并同系统事务同步保存,再尝试递交至第三方数据库
        var otherStoreTxns = new HashMap<Long, DbTransaction>();
        return KVTransaction.beginAsync().thenCompose(txn -> {
            //1.保存所有模型并同步相关表结构
            var task = saveModelsAsync(hub, pkg, txn, otherStoreTxns); //TODO:异常回滚
            //2.签入所有
            task = task.thenCompose(r -> CheckoutService.checkInAsync(txn));
            //3.刷新所有CheckoutByMe的节点项,注意必须先刷新后清除缓存，否则删除的节点在移除后会自动保存
            hub.designTree.checkinAllNodes();
            //4.清除所有修改
            task = task.thenCompose(r -> StagedService.deleteStagedAsync());

            return task.thenCompose(r -> {
                CompletableFuture<Void> commitTask = CompletableFuture.completedFuture(null);
                //5.先尝试递交第三方数据库的DDL事务
                for (var sqlTxn : otherStoreTxns.values()) {
                    commitTask = commitTask.thenCompose(r2 -> sqlTxn.commitAsync());
                }
                //6.再递交系统数据库事务
                return commitTask.thenCompose(r2 -> txn.commitAsync())
                        .thenAccept(r2 -> invalidModelsCache(hub, pkg)); //7.最后通知各节点更新模型缓存
            });

        });
    }

    private static CompletableFuture<Void> saveModelsAsync(
            DesignHub hub, PublishPackage pkg, KVTransaction txn, Map<Long, DbTransaction> otherStoreTxns) {
        //TODO:保存文件夹

        //保存模型，注意:
        //1.映射至系统存储的实体模型的变更与删除暂由ModelStore处理，映射至SqlStore的DDL暂在这里处理
        //2.删除的模型同时删除相关代码及编译好的组件，包括视图模型的相关路由
        CompletableFuture<Void> task = CompletableFuture.completedFuture(null);
        for (var model : pkg.models) {
            switch (model.persistentState()) {
                case Detached:
                    task = task.thenCompose(r -> insertModelAsync(hub, model, txn, otherStoreTxns));
                    break;
                case Unchnaged: //TODO:临时
                case Modified:
                    task = task.thenCompose(r -> updateModelAsync(hub, model, txn, otherStoreTxns));
                    break;
                case Deleted:
                    task = task.thenCompose(r -> deleteModelAsync(hub, model, txn, otherStoreTxns));
                    break;
                default:
                    throw new RuntimeException("Can't save model with state: " + model.persistentState().name());
            }
        }

        //保存模型相关的代码
        for (var entry : pkg.sourceCodes.entrySet()) {
            task = task.thenCompose(r -> ModelStore.upsertModelCodeAsync(entry.getKey(), entry.getValue(), txn));
        }

        //保存服务模型编译好的组件
        for (var entry : pkg.serviceAssemblies.entrySet()) {
            task = task.thenCompose(
                    r -> ModelStore.upsertAssemblyAsync(true, entry.getKey(), entry.getValue(), txn));
        }

        //TODO:保存视图模型编译好的运行时组件

        return task;
    }

    private static CompletableFuture<Void> insertModelAsync(DesignHub hub, ModelBase model
            , KVTransaction txn, Map<Long, DbTransaction> otherStoreTxns) {
        return ModelStore.insertModelAsync(model, txn).thenCompose(r -> {
            if (model.modelType() == ModelType.Entity) {
                return createTableAsync(hub, (EntityModel) model, otherStoreTxns);
            } else if (model.modelType() == ModelType.View) { //TODO:暂在这里保存视图模型的路由
                return upsertViewRouteAsync(model, txn);
            }
            return CompletableFuture.completedFuture(null);
        });
    }

    private static CompletableFuture<Void> updateModelAsync(DesignHub hub, ModelBase model
            , KVTransaction txn, Map<Long, DbTransaction> otherStoreTxns) {
        return ModelStore.updateModelAsync(model, txn, appid -> hub.designTree.findApplicationNode(appid).model)
                .thenCompose(r -> {
                    if (model.modelType() == ModelType.Entity) {
                        return alterTableAsync(hub, (EntityModel) model, otherStoreTxns);
                    } else if (model.modelType() == ModelType.Service) {
                        //TODO:服务模型重命名删除旧的Assembly
                    } else if (model.modelType() == ModelType.View) {
                        throw new RuntimeException("未实现");
                    }
                    return CompletableFuture.completedFuture(null);
                });
    }

    private static CompletableFuture<Void> deleteModelAsync(DesignHub hub, ModelBase model
            , KVTransaction txn, Map<Long, DbTransaction> otherStoreTxns) {
        final Function<Integer, ApplicationModel> findApp = appid -> hub.designTree.findApplicationNode(appid).model;
        return ModelStore.deleteModelAsync(model, txn, findApp).thenCompose(r -> {
            if (model.modelType() == ModelType.Entity) {
                return dropTableAsync(hub, (EntityModel) model, otherStoreTxns);
            } else if (model.modelType() == ModelType.Service) {
                var app     = hub.designTree.findApplicationNode(model.appId());
                var asmName = String.format("%s.%s", app.model.name(), model.originalName()); //注意是旧名称
                return ModelStore.deleteModelCodeAsync(model.id(), txn)
                        .thenCompose(r2 -> ModelStore.deleteAssemblyAsync(true, asmName, txn));
            } else if (model.modelType() == ModelType.View) {
                throw new RuntimeException("未实现");
            }
            return CompletableFuture.completedFuture(null);
        });
    }

    /** 保存视图模型的路由 */
    private static CompletableFuture<Void> upsertViewRouteAsync(ModelBase model, KVTransaction txn) {
        Log.warn("保存视图路由未实现");
        return CompletableFuture.completedFuture(null);
    }

    //region ====第三方数据库的数据表操作====
    private static CompletableFuture<DbTransaction> makeOtherStoreTxn(long storeId, Map<Long, DbTransaction> txns) {
        var txn = txns.get(storeId);
        if (txn != null)
            return CompletableFuture.completedFuture(txn);

        var sqlStore = SqlStore.get(storeId);
        return sqlStore.beginTransaction().thenApply(r -> {
            txns.put(storeId, r);
            return r;
        });
    }

    /** 新建第三方数据库的数据表 */
    private static CompletableFuture<Void> createTableAsync(DesignHub hub, EntityModel em
            , Map<Long, DbTransaction> otherStoreTxns) {
        if (em.sqlStoreOptions() != null) {
            final var sqlStoreId = em.sqlStoreOptions().storeModelId();
            var       sqlStore   = SqlStore.get(sqlStoreId);
            return makeOtherStoreTxn(sqlStoreId, otherStoreTxns)
                    .thenCompose(sqlTxn -> sqlStore.createTableAsync(em, sqlTxn, hub));
        } //TODO:Cql
        return CompletableFuture.completedFuture(null);
    }

    /** 更改第三方数据库的数据表 */
    private static CompletableFuture<Void> alterTableAsync(DesignHub hub, EntityModel em
            , Map<Long, DbTransaction> otherStoreTxns) {
        if (em.sqlStoreOptions() != null) {
            final var sqlStoreId = em.sqlStoreOptions().storeModelId();
            var       sqlStore   = SqlStore.get(sqlStoreId);
            return makeOtherStoreTxn(sqlStoreId, otherStoreTxns)
                    .thenCompose(sqlTxn -> sqlStore.alterTableAsync(em, sqlTxn, hub));
        } //TODO:Cql
        return CompletableFuture.completedFuture(null);
    }

    /** 删除第三方数据库的数据表 */
    private static CompletableFuture<Void> dropTableAsync(DesignHub hub, EntityModel em
            , Map<Long, DbTransaction> otherStoreTxns) {
        if (em.sqlStoreOptions() != null) {
            final var sqlStoreId = em.sqlStoreOptions().storeModelId();
            var       sqlStore   = SqlStore.get(sqlStoreId);
            return makeOtherStoreTxn(sqlStoreId, otherStoreTxns)
                    .thenCompose(sqlTxn -> sqlStore.dropTableAsync(em, sqlTxn, hub));
        } //TODO:Cql
        return CompletableFuture.completedFuture(null);
    }
    //endregion

    /** 通知集群各节点模型缓存失效 */
    private static void invalidModelsCache(DesignHub hub, PublishPackage pkg) {
        if (pkg.models.size() == 0)
            return;

        var others = pkg.models.stream()
                .filter(t -> t.modelType() != ModelType.Service)
                .mapToLong(ModelBase::id).toArray();
        var services = pkg.models.stream()
                .filter(t -> t.modelType() == ModelType.Service)
                .map(t -> {
                    var appName = hub.designTree.findApplicationNode(t.appId()).model.name();
                    var name    = t.isNameChanged() ? t.originalName() : t.name();
                    return String.format("%s.%s", appName, name);
                }).toArray(String[]::new);

        RuntimeContext.current().invalidModelsCache(services, others, true);
    }

}
