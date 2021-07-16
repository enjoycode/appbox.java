package appbox.design.tree;

import appbox.design.common.CheckoutInfo;
import appbox.design.DesignHub;
import appbox.design.services.CheckoutService;
import appbox.design.services.StagedItems;
import appbox.design.services.StagedService;
import appbox.logging.Log;
import appbox.model.*;
import appbox.model.entity.EntityMemberModel;
import appbox.model.entity.EntityRefModel;
import appbox.runtime.RuntimeContext;
import appbox.store.ModelStore;
import appbox.utils.IdUtil;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

public final class DesignTree {
    private final AtomicInteger _loadingFlag = new AtomicInteger(0);

    public final DesignHub      designHub;
    public final NodeCollection nodes;

    private StagedItems         staged;       //仅用于加载树时临时放入挂起的模型
    private DataStoreRootNode   storeRootNode;
    private ApplicationRootNode appRootNode;

    private Map<String, CheckoutInfo> _checkouts;

    public DesignTree(DesignHub hub) {
        designHub = hub;
        nodes     = new NodeCollection(null);
    }

    //region ====Properties====
    public DataStoreRootNode storeRootNode() {
        return storeRootNode;
    }

    public ApplicationRootNode appRootNode() {
        return appRootNode;
    }
    //endregion

    //region ====Load Methods====
    public CompletableFuture<Boolean> loadNodesAsync() {
        if (_loadingFlag.compareAndExchange(0, 1) != 0) {
            return CompletableFuture.failedFuture(new Exception("DesignTree is loading."));
        }

        //先判断是否已经加载过，是则清空准备重新加载
        if (nodes.size() > 0) {
            nodes.clear();
        }

        //开始加载
        storeRootNode = new DataStoreRootNode(this);
        nodes.add(storeRootNode);
        appRootNode = new ApplicationRootNode(this);
        nodes.add(appRootNode);

        //先加载签出信息及StagedModels
        return CheckoutService.loadAllAsync().thenCompose(checkouts -> {
            _checkouts = checkouts;
            return StagedService.loadStagedAsync(true);
        }).thenCompose(stagedItems -> {
            staged = stagedItems;
            return ModelStore.loadDesignTreeAsync();
        }).thenApply(all -> {
            //1.加载Apps
            for (ApplicationModel app : all.apps) {
                appRootNode.nodes.add(new ApplicationNode(this, app));
            }

            //2.加载Folders
            var mergedFolders = all.folders;
            //2.1从staged中添加新建的并更新修改的文件夹
            staged.updateFolders(mergedFolders);
            //2.2加入Folders
            for (var folder : mergedFolders) {
                findModelRootNode(folder.appId(), folder.targetModelType()).addFolder(folder, null);
            }

            //3.加载存储
            var storeModels = new ArrayList<DataStoreModel>();
            //3.1 添加系统默认存储模型
            var defaultStoreModel = new DataStoreModel(DataStoreModel.DataStoreKind.Future, "", "Default");
            storeModels.add(defaultStoreModel);
            //3.2 添加第三方存储
            if (all.stores != null)
                storeModels.addAll(all.stores);
            for (var s : storeModels) {
                storeRootNode.addModel(s, designHub, false);
            }

            //4.加载模型
            var mergedModels = all.models;
            //4.1加载staged中新建的模型，可能包含DataStoreModel
            mergedModels.addAll(Arrays.asList(staged.findNewModels()));
            //4.2加入Models
            staged.removeDeletedModels(mergedModels);  //先移除已删除的
            var allModelNodes = new ArrayList<ModelNode>(); //需要延迟创建虚拟代码的模型(排除Permission)
            for (ModelBase m : mergedModels) {
                var modelNode = findModelRootNode(m.appId(), m.modelType()).addModel(m);
                if (m.modelType() != ModelType.Permission)
                    allModelNodes.add(modelNode);
            }

            //5.在所有节点加载完后创建模型对应的虚拟文件
            designHub.typeSystem.createPermissionsDocuments(); //权限模型单独处理
            for (ModelNode n : allModelNodes) {
                designHub.typeSystem.createModelDocument(n);
            }

            //最后加载完后通知DartLanguageServer开始初始化
            if (designHub.isFlutterIDE()) {
                designHub.dartLanguageServer.start();
            }

            return true;
        }).handle((r, ex) -> {
            _loadingFlag.compareAndExchange(1, 0);
            return ex == null ? r : false;
        });
    }

    /** 仅用于测试 */
    public void loadNodesForTest(ApplicationModel appModel, DataStoreModel dataStore, List<ModelBase> models) {
        nodes.clear();
        _checkouts = new HashMap<>();

        storeRootNode = new DataStoreRootNode(this);
        nodes.add(storeRootNode);
        appRootNode = new ApplicationRootNode(this);
        nodes.add(appRootNode);

        appRootNode.nodes.add(new ApplicationNode(this, appModel));
        storeRootNode.addModel(dataStore, designHub, false);

        var allModelNodes = new ArrayList<ModelNode>();
        for (ModelBase m : models) {
            allModelNodes.add(findModelRootNode(m.appId(), m.modelType()).addModel(m));
        }

        //在所有节点加载完后创建模型对应的虚拟文件
        designHub.typeSystem.createPermissionsDocuments(); //权限模型单独处理
        for (ModelNode n : allModelNodes) {
            designHub.typeSystem.createModelDocument(n);
        }
    }
    //endregion

    //region ====Find Methods====

    /** 用于前端传回的参数查找对应的设计节点 */
    public DesignNode findNode(DesignNodeType type, String id) {
        switch (type) {
            case EntityModelNode:
                return findModelNode(ModelType.Entity, Long.parseUnsignedLong(id));
            case ServiceModelNode:
                return findModelNode(ModelType.Service, Long.parseUnsignedLong(id));
            case ViewModelNode:
                return findModelNode(ModelType.View, Long.parseUnsignedLong(id));
            case FolderNode:
                return findFolderNode(id);
            case DataStoreNode:
                return storeRootNode.nodes.find(n -> n.id().equals(id));
            case ModelRootNode:
                final var sr = id.split("-");
                return findModelRootNode(Integer.parseUnsignedInt(sr[0])
                        , ModelType.fromValue((byte) Integer.parseInt(sr[1])));
            default:
                Log.warn("findNode: " + type.name() + "未实现");
                throw new RuntimeException("未实现");
        }
    }

    public ApplicationNode findApplicationNodeByName(String name) {
        for (DesignNode node : appRootNode.nodes.list) {
            if (((ApplicationNode) node).model.name().equals(name)) {
                return (ApplicationNode) node;
            }
        }
        return null;
    }

    public ApplicationNode findApplicationNode(int appId) {
        for (DesignNode node : appRootNode.nodes.list) {
            if (((ApplicationNode) node).model.id() == appId) {
                return (ApplicationNode) node;
            }
        }
        return null;
    }

    public DataStoreNode findDataStoreNodeByName(String name) {
        return (DataStoreNode) storeRootNode.nodes.find(n -> n.text().equals(name));
    }

    public ModelRootNode findModelRootNode(int appId, ModelType modelType) {
        for (DesignNode node : appRootNode.nodes.list) {
            var appNode = (ApplicationNode) node;
            if (appNode.model.id() == appId) {
                return appNode.findModelRootNode(modelType);
            }
        }
        return null;
    }

    private FolderNode findFolderNode(String id) {
        var folderId = UUID.fromString(id);
        for (int i = 0; i < appRootNode.nodes.size(); i++) {
            if (appRootNode.nodes.get(i) instanceof ApplicationNode) {
                var folderNode = ((ApplicationNode) appRootNode.nodes.get(i)).findFolderNode(folderId);
                if (folderNode != null)
                    return folderNode;
            }
        }
        return null;
    }

    /** 根据模型标识获取相应的节点 */
    public final ModelNode findModelNode(long modelId) {
        return findModelNode(IdUtil.getModelTypeFromModelId(modelId), modelId);
    }

    /** 根据模型类型及标识号获取相应的节点 */
    public final ModelNode findModelNode(ModelType modelType, long modelId) {
        var appId         = IdUtil.getAppIdFromModelId(modelId);
        var modelRootNode = findModelRootNode(appId, modelType);
        if (modelRootNode == null) {
            return null;
        }
        return modelRootNode.findModelNode(modelId);
    }

    /**
     * 根据前端模型对应的文件名查找相应的模型节点
     * @param fileName eg: sys.Views.HomePage.dart
     * @return null for not found
     */
    public final ModelNode findModelNodeByFileName(String fileName) {
        var firstDot  = fileName.indexOf('.');
        var lastDot   = fileName.lastIndexOf('.');
        var appName   = fileName.substring(0, firstDot);
        var app       = findApplicationNodeByName(appName);
        var secondDot = fileName.indexOf('.', firstDot + 1);
        var typeName  = fileName.substring(firstDot + 1, secondDot);
        var modelName = fileName.substring(secondDot + 1, lastDot);

        if (typeName.equals("Services")) {
            return findModelNodeByName(app.model.id(), ModelType.Service, modelName);
        } else if (typeName.equals("Views")) {
            return findModelNodeByName(app.model.id(), ModelType.View, modelName);
        } else {
            throw new RuntimeException("未实现");
        }
    }

    /** 查找所有引用指定模型标识的EntityRef Member集合 */
    public List<EntityRefModel> findEntityRefModels(long targetEntityModelID) {
        List<EntityRefModel> result = new ArrayList<>();
        List<ModelNode>      ls     = findNodesByType(ModelType.Entity);

        for (ModelNode l : ls) {
            EntityModel model = (EntityModel) l.model();
            //注意：不能排除自身引用，主要指树状结构的实体
            for (int j = 0; j < model.getMembers().size(); j++) {
                if (model.getMembers().get(j).type() == EntityMemberModel.EntityMemberType.EntityRef) {
                    EntityRefModel refMember = (EntityRefModel) model.getMembers().get(j);
                    //注意不排除聚合引用
                    for (int k = 0; k < refMember.getRefModelIds().size(); k++) {
                        if (refMember.getRefModelIds().get(k) == targetEntityModelID)
                            result.add(refMember);
                    }
                }
            }
        }
        return result;
    }

    /** 查找指定类型的所有节点 */
    public List<ModelNode> findNodesByType(ModelType modelType) {
        var list = new ArrayList<ModelNode>();
        for (int i = 0; i < appRootNode.nodes.size(); i++) {
            var appNode       = (ApplicationNode) appRootNode.nodes.get(i);
            var modelRootNode = appNode.findModelRootNode(modelType);
            list.addAll(modelRootNode.getAllModelNodes());
        }
        return list;
    }
    //endregion

    //region ====Find for Create====

    /** 用于新建时检查相同名称的模型是否已存在 */
    public ModelNode findModelNodeByName(int appId, ModelType type, String name) {
        //TODO:***** 考虑在这里加载存储有没有相同名称的存在,或发布时检测，如改为全局Workspace没有此问题
        // dev1 -> load tree -> checkout -> add model -> publish
        // dev2 -> load tree                                 -> checkout -> add model with same name will pass
        var modelRootNode = findModelRootNode(appId, type);
        return modelRootNode.findModelNodeByName(name);
    }

    /** 根据当前选择的节点查询新建模型的上级节点 */
    public static DesignNode findNewModelParentNode(DesignNode node, ModelType newModelType) {
        if (node == null)
            return null;

        if (node.nodeType() == DesignNodeType.FolderNode) {
            return node;
        } else if (node.nodeType() == DesignNodeType.ModelRootNode) {
            var modelRootNode = (ModelRootNode) node;
            if (modelRootNode.targetType == newModelType) {
                return node;
            }
        } else if (node.nodeType() == DesignNodeType.ApplicationNode) {
            return ((ApplicationNode) node).findModelRootNode(newModelType);
        }

        return findNewModelParentNode(node.getParent(), newModelType);
    }

    /**
     * 根据当前选择的节点查找新建文件夹节点的上级节点
     * @return ModelRootNode or FolderNode or null
     */
    public static DesignNode findNewFolderParentNode(DesignNode selected) {
        if (selected instanceof ModelRootNode)
            return selected;
        if (selected.nodeType() == DesignNodeType.FolderNode)
            return selected;
        if (selected instanceof ModelNode)
            return selected.getParent();
        return null;
    }

    /** 向上递归查找指定节点所属的应用节点 */
    public static ApplicationNode findAppNodeFromNode(DesignNode node) {
        if (node == null)
            return null;
        if (node.nodeType() == DesignNodeType.ApplicationNode) {
            return (ApplicationNode) node;
        }
        return findAppNodeFromNode(node.getParent());
    }

    //endregion

    //region ====Checkout Methods====
    //用于签出节点成功后添加签出信息列表
    public void addCheckoutInfos(List<CheckoutInfo> infos) {
        for (CheckoutInfo info : infos) {
            String key = CheckoutInfo.makeKey(info.nodeType, info.targetID);
            if (!_checkouts.containsKey(key)) {
                _checkouts.put(key, info);
            }
        }
    }

    /**
     * 给设计节点添加签出信息，如果已签出的模型节点则用本地存储替换原模型
     */
    protected void bindCheckoutInfo(DesignNode node, boolean isNewNode) {
        //if (node.NodeType == DesignNodeType.FolderNode || !node.AllowCheckout)
        //    throw new ArgumentException("不允许绑定签出信息: " + node.NodeType.ToString());

        //先判断是否新增的
        if (isNewNode) {
            node.setCheckoutInfo(new CheckoutInfo(node.nodeType(),
                    node.checkoutInfoTargetID(), node.version(),
                    designHub.session.name(), designHub.session.leafOrgUnitId()));
            return;
        }

        //非新增的比对服务端的签出列表
        var key      = CheckoutInfo.makeKey(node.nodeType(), node.checkoutInfoTargetID());
        var checkout = _checkouts.get(key);
        if (checkout != null) {
            node.setCheckoutInfo(checkout);
            if (node.isCheckoutByMe() && node instanceof ModelNode) { //如果是被当前用户签出的模型
                var modelNode = (ModelNode) node;
                //从staged加载修改中的模型进行替换
                var stagedModel = staged.findModel(modelNode.model().id());
                if (stagedModel != null)
                    modelNode.setModel(stagedModel);
            }
        }
    }

    /** 部署完后更新所有模型节点的状态，并移除待删除的节点 */
    public void checkinAllNodes() {
        //循环更新模型节点
        for (int i = 0; i < appRootNode.nodes.size(); i++) {
            ((ApplicationNode) appRootNode.nodes.get(i)).checkinAllNodes();
        }

        //刷新签出信息表，移除被自己签出的信息
        var list = new ArrayList<String>();
        for (var entry : _checkouts.entrySet()) {
            if (entry.getValue().developerOuid == RuntimeContext.current().currentSession().leafOrgUnitId())
                list.add(entry.getKey());
        }
        for (var key : list) {
            _checkouts.remove(key);
        }
    }
    //endregion

}
