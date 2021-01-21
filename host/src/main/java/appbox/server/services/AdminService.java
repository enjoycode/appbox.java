package appbox.server.services;

import appbox.data.EntityId;
import appbox.data.JsonResult;
import appbox.data.PermissionNode;
import appbox.model.*;
import appbox.runtime.IService;
import appbox.runtime.InvokeArgs;
import appbox.runtime.RuntimeContext;
import appbox.store.KVTransaction;
import appbox.store.ModelStore;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/** 系统管理员服务，主要用于权限管理 */
public final class AdminService implements IService {

    /** 用于前端组织结构权限管理界面加载整个权限树 */
    private CompletableFuture<List<PermissionNode>> loadPermissionTree() {
        //TODO:***暂简单实现加载全部，待优化为加载特定类型
        return ModelStore.loadAllApplicationAsync().thenCompose(apps -> ModelStore.loadAllFolderAsync()
                .thenCombine(ModelStore.loadAllModelAsync(), (allFolders, allModels) -> {
                    var folders = Arrays.stream(allFolders)
                            .filter(f -> f.targetModelType() == ModelType.Permission)
                            .toArray(ModelFolder[]::new);
                    var permissions = Arrays.stream(allModels)
                            .filter(m -> m.modelType() == ModelType.Permission)
                            .toArray(ModelBase[]::new);

                    var list = new ArrayList<PermissionNode>();

                    for (var app : apps) {
                        var appNode = new PermissionNode(app.name());
                        list.add(appNode);
                        //加载文件夹
                        var folderMap = new HashMap<UUID, PermissionNode>();
                        var rootFolder = Arrays.stream(folders)
                                .filter(f -> f.appId() == app.id())
                                .findFirst();
                        rootFolder.ifPresent(folder -> loopAddFolder(folderMap, appNode, folder));
                        //加载PermissionModels
                        var appPermissions = Arrays.stream(permissions)
                                .filter(m -> m.appId() == app.id())
                                .map(m -> (PermissionModel) m)
                                .toArray(PermissionModel[]::new);
                        for (var p : appPermissions) {
                            var modelNode = new PermissionNode(p);
                            if (p.getFolderId() != null) {
                                var folderNode = folderMap.get(p.getFolderId());
                                Objects.requireNonNullElse(folderNode, appNode).children().add(modelNode);
                            } else {
                                appNode.children().add(modelNode);
                            }
                        }
                    }

                    return list;
                }));
    }

    /** 用于前端实时保存单个PermissionModel的权限变更 */
    private CompletableFuture<Object> savePermission(String id, EntityId[] orgUnits) {
        final var modelId = Long.parseUnsignedLong(id);
        return ModelStore.loadModelAsync(modelId).thenApply(model -> {
            PermissionModel oldModel = (PermissionModel) model;
            if (oldModel == null)
                throw new RuntimeException("Can't find PermissionModel");
            //开始重置
            if (oldModel.hasOrgUnits())
                oldModel.orgUnits().clear();

            if (orgUnits != null) {
                for (var ou : orgUnits) {
                    oldModel.orgUnits().add(ou);
                }
            }
            return oldModel;
        }).thenCompose(model -> KVTransaction.beginAsync()
                .thenCompose(txn -> ModelStore.updateModelAsync(model, txn, null)
                        .thenCompose(r -> txn.commitAsync())))
                .thenApply(r -> {
                    //更新服务端缓存
                    RuntimeContext.current().invalidModelsCache(null, new long[]{modelId}, true);
                    //TODO:***激发模型变更事件通知集群的其他节点
                    return null;
                });
    }

    private static void loopAddFolder(Map<UUID, PermissionNode> map, PermissionNode parent, ModelFolder folder) {
        var parentNode = parent;
        if (folder.getParent() != null) {
            var node = new PermissionNode(folder.name());
            map.put(folder.id(), node);
            parent.children().add(node);
            parentNode = node;
        }

        if (folder.hasChildren()) {
            for (var child : folder.children()) {
                loopAddFolder(map, parentNode, child);
            }
        }
    }

    @Override
    public CompletableFuture<Object> invokeAsync(CharSequence method, InvokeArgs args) {
        //TODO:判断管理员权限

        if (method.equals("LoadPermissionNodes")) { //TODO:暂兼容旧版名称
            return loadPermissionTree().thenApply(JsonResult::new);
        } else if (method.equals("SavePermission")) {
            return savePermission(args.getString(), args.getArrayOfEntityId());
        } else {
            var ex = new NoSuchMethodException("AdminService can't find method: " + method.toString());
            return CompletableFuture.failedFuture(ex);
        }
    }

}
