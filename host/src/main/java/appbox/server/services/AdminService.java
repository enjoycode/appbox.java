package appbox.server.services;

import appbox.data.JsonResult;
import appbox.data.PermissionNode;
import appbox.model.*;
import appbox.runtime.IService;
import appbox.runtime.InvokeArgs;
import appbox.store.ModelStore;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/** 系统管理员服务，主要用于权限管理 */
public final class AdminService implements IService {

    /** 用于前端组织结构权限管理界面加载整个权限树 */
    private CompletableFuture<Object> loadPermissionTree() {
        //TODO:***暂简单实现加载全部，待优化为加载特定类型
        return ModelStore.loadAllApplicationAsync().thenApply(apps -> ModelStore.loadAllFolderAsync()
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
                        var folderMap  = new HashMap<UUID, PermissionNode>();
                        var rootFolder = Arrays.stream(folders)
                                .filter(f -> f.appId() == app.id())
                                .findFirst();
                        rootFolder.ifPresent(folder -> loopAddFolder(folderMap, appNode, folder));
                        //加载PermissionModels
                        var appPermissions = Arrays.stream(permissions)
                                .filter(m -> m.appId() == app.id())
                                .map(m -> (PermissionModel)m)
                                .toArray(PermissionModel[]::new);
                        for(var p : appPermissions) {
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
        if (method.equals("LoadPermissionNodes")) { //TODO:暂兼容旧版名称
            return loadPermissionTree().thenApply(JsonResult::new);
        } else {
            var ex = new NoSuchMethodException("AdminService can't find method: " + method.toString());
            return CompletableFuture.failedFuture(ex);
        }
    }

}
