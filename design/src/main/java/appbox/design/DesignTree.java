package appbox.design;

import java.util.*;

public final class DesignTree {
    private int _loadingFlag;

    /**
     * 仅用于加载树时临时放入挂起的模型
     */
    private StagedItems staged;
    private DesignHub designHub;
    private NodeCollection nodes;
    private byte storeRootNode;
    private byte appRootNode;

    public DesignTree(DesignHub hub) {
        setDesignHub(hub);
        setNodes(new NodeCollection(null));
    }

    private Map<String, CheckoutInfo> _checkouts;

    //用于签出节点成功后添加签出信息列表
    public void AddCheckoutInfos(List<CheckoutInfo> infos) {
        for (int i = 0; i < infos.size(); i++) {
            String key = CheckoutInfo.MakeKey(infos.get(i).getNodeType(), infos.get(i).getTargetID());
            if (!_checkouts.containsKey(key)) {
                _checkouts.put(key, infos.get(i));
            }
        }
    }
    //region ====get set====
    public StagedItems getStaged() {
        return staged;
    }

    public void setStaged(StagedItems value) {
        staged = value;
    }


    public DesignHub getDesignHub() {
        return designHub;
    }

    public void setDesignHub(DesignHub value) {
        designHub = value;
    }


    public NodeCollection getNodes() {
        return nodes;
    }

    private void setNodes(NodeCollection value) {
        nodes = value;
    }



    public byte getStoreRootNode() {
        return storeRootNode;
    }

    public void setStoreRootNode(byte value) {
        storeRootNode = value;
    }


    public byte getAppRootNode() {
        return appRootNode;
    }

    public void setAppRootNode(byte value) {
        appRootNode = value;
    }
    //endregion
    //region ====LoadMethod====
//C# TO JAVA CONVERTER TODO TASK: There is no equivalent in Java to the 'async' keyword:
//ORIGINAL LINE: internal async Task LoadNodesAsync()
    /*public Task LoadNodesAsync() {
        tangible.RefObject<Integer> tempRef__loadingFlag = new tangible.RefObject<Integer>(_loadingFlag);
        if (Interlocked.CompareExchange(tempRef__loadingFlag, 1, 0) != 0) {
            _loadingFlag = tempRef__loadingFlag.argValue;
            throw new RuntimeException("DesignTree are loading.");
        } else {
            _loadingFlag = tempRef__loadingFlag.argValue;
        }

        //先判断是否已经加载过，是则清空准备重新加载
        if (getNodes().getCount() > 0) {
            getDesignHub().ResetTypeSystem();
            getNodes().Clear();
        }

        //开始加载
        setStoreRootNode(new byte(this));
        getNodes().Add(getStoreRootNode());
        setAppRootNode(new byte(this));
        getNodes().Add(getAppRootNode());

        //先加载签出信息及StagedModels
//C# TO JAVA CONVERTER TODO TASK: There is no equivalent to 'await' in Java:
        _checkouts = await CheckoutService.LoadAllAsync();
//C# TO JAVA CONVERTER TODO TASK: There is no equivalent to 'await' in Java:
        setStaged(await StagedService.LoadStagedAsync(true));

//C# TO JAVA CONVERTER TODO TASK: There is no equivalent to implicit typing in Java:
//C# TO JAVA CONVERTER TODO TASK: There is no equivalent to 'await' in Java:
        var amodels = await Store.ModelStore.LoadAllApplicationAsync();
        ArrayList<ApplicationModel> applicationModels = new ArrayList<ApplicationModel>(amodels);
        Collections.sort(applicationModels, (a, b) -> a.Name.CompareTo(b.Name));

//C# TO JAVA CONVERTER TODO TASK: There is no equivalent to implicit typing in Java:
//C# TO JAVA CONVERTER TODO TASK: There is no equivalent to 'await' in Java:
        var mfolders = await Store.ModelStore.LoadAllFolderAsync();
        ArrayList<ModelFolder> folders = new ArrayList<ModelFolder>(mfolders);
        //从staged中添加新建的并更新修改的文件夹
        getStaged().UpdateFolders(folders);

//C# TO JAVA CONVERTER TODO TASK: There is no equivalent to implicit typing in Java:
//C# TO JAVA CONVERTER TODO TASK: There is no equivalent to 'await' in Java:
        var mmodels = await Store.ModelStore.LoadAllModelAsync();
        ArrayList<ModelBase> models = new ArrayList<ModelBase>(mmodels);
//C# TO JAVA CONVERTER TODO TASK: There is no preprocessor in Java:
//#if !FUTURE
        //加载默认存储模型
//C# TO JAVA CONVERTER TODO TASK: There is no equivalent to implicit typing in Java:
        var defaultStoreType = Store.SqlStore.getDefault().getClass();
        DataStoreModel defaultStoreModel = new DataStoreModel(DataStoreKind.Sql, String.format("%1$s;%2$s", defaultStoreType.Assembly.GetName().Name, defaultStoreType.Name), "Default");
        defaultStoreModel.setNameRules(DataStoreNameRules.AppPrefixForTable);
        //defaultStoreModel.Settings = ""; //TODO:fix settings
        defaultStoreModel.AcceptChanges();
        models.add(defaultStoreModel);
//#endif
        //加载staged中新建的模型，可能包含DataStoreModel
        models.addAll(Arrays.asList(getStaged().FindNewModels()));

        //加入AppModels节点
        for (ApplicationModel app : applicationModels) {
            getAppRootNode().getNodes().Add(new byte(this, app));
        }
        //加入Folders
        for (ModelFolder f : folders) {
            FindModelRootNode(f.getAppId(), f.getTargetModelType()).AddFolder(f);
        }

        //加入Models
        getStaged().RemoveDeletedModels(models); //先移除已删除的
        ArrayList<ModelNode> allModelNodes = new ArrayList<ModelNode>(models.size());
        for (ModelBase m : models) {
            if (m.getModelType() == ModelType.DataStore) {
                DataStoreModel dsModel = (DataStoreModel) m;
                appbox.Design.byte dsNode = getStoreRootNode().AddModel(dsModel, getDesignHub());
                getDesignHub().getTypeSystem().CreateStoreDocument(dsNode);
            } else {
                allModelNodes.add(FindModelRootNode(m.getAppId(), m.getModelType()).AddModel(m));
            }
        }
        //在所有节点加载完后创建模型对应的RoslynDocument
        for (ModelNode n : allModelNodes) {
//C# TO JAVA CONVERTER TODO TASK: There is no equivalent to 'await' in Java:
            await getDesignHub ().getTypeSystem().CreateModelDocumentAsync(n);
        }

        tangible.RefObject<Integer> tempRef__loadingFlag2 = new tangible.RefObject<Integer>(_loadingFlag);
        Interlocked.Exchange(tempRef__loadingFlag2, 0);
        _loadingFlag = tempRef__loadingFlag2.argValue;
        //清空Staged
        setStaged(null);

//C# TO JAVA CONVERTER TODO TASK: There is no preprocessor in Java:
//#if DEBUG
        ThreadPool.QueueUserWorkItem(s ->
        {
            getDesignHub().getTypeSystem().DumpProjectErrors(getDesignHub().getTypeSystem().ModelProjectId);
            //DesignHub.TypeSystem.DumpProjectErrors(DesignHub.TypeSystem.SyncSysServiceProjectId);
            getDesignHub().getTypeSystem().DumpProjectErrors(getDesignHub().getTypeSystem().ServiceBaseProjectId);
        });
//#endif
    }

//C# TO JAVA CONVERTER TODO TASK: There is no preprocessor in Java:
//#if DEBUG

    //仅用于单元测试
//C# TO JAVA CONVERTER TODO TASK: There is no equivalent in Java to the 'async' keyword:
//ORIGINAL LINE: internal async Task LoadForTest(List<ApplicationModel> apps, List<ModelBase> models)
    public Task LoadForTest(ArrayList<ApplicationModel> apps, ArrayList<ModelBase> models) {
        tangible.RefObject<Integer> tempRef__loadingFlag = new tangible.RefObject<Integer>(_loadingFlag);
        if (Interlocked.CompareExchange(tempRef__loadingFlag, 1, 0) != 0) {
            _loadingFlag = tempRef__loadingFlag.argValue;
            throw new RuntimeException("DesignTree has loaded or loading.");
        } else {
            _loadingFlag = tempRef__loadingFlag.argValue;
        }

        setStoreRootNode(new byte(this));
        getNodes().Add(getStoreRootNode());
        setAppRootNode(new byte(this));
        getNodes().Add(getAppRootNode());

        _checkouts = new HashMap<String, CheckoutInfo>();

        //加入AppModels节点
        for (int i = 0; i < apps.size(); i++) {
            byte appNode = new byte(this, apps.get(i));
            getAppRootNode().getNodes().Add(appNode);
        }
        //加入Models
        ModelNode[] allModelNodes = new ModelNode[models.size()];
        for (int i = 0; i < models.size(); i++) {
            allModelNodes[i] = FindModelRootNode(models.get(i).getAppId(), models.get(i).getModelType()).AddModel(models.get(i));
        }
        //创建RoslynDocument
        for (int i = 0; i < models.size(); i++) {
//C# TO JAVA CONVERTER TODO TASK: There is no equivalent to 'await' in Java:
            await getDesignHub ().getTypeSystem().CreateModelDocumentAsync(allModelNodes[i]);
        }

        tangible.RefObject<Integer> tempRef__loadingFlag2 = new tangible.RefObject<Integer>(_loadingFlag);
        Interlocked.Exchange(tempRef__loadingFlag2, 0);
        _loadingFlag = tempRef__loadingFlag2.argValue;

        getDesignHub().getTypeSystem().DumpProjectErrors(getDesignHub().getTypeSystem().ModelProjectId);
        //DesignHub.TypeSystem.DumpProjectErrors(DesignHub.TypeSystem.SyncSysServiceProjectId);
        getDesignHub().getTypeSystem().DumpProjectErrors(getDesignHub().getTypeSystem().ServiceBaseProjectId);
    }
//#endif
    //endregion

    //C# TO JAVA CONVERTER TODO TASK: There is no preprocessor in Java:
    ///#region ====Find Methods====
    public byte FindbyteByName(String appName) {
        return (byte) getAppRootNode().getNodes().Find(n = appName.equals( > ((byte) n).getModel().getName()))
        ;
    }

    //C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: internal byte Findbyte(uint appId)
    public byte Findbyte(int appId) {
        return (byte) getAppRootNode().getNodes().Find(n -> ((byte) n).getModel().getId() == appId);
    }

    public byte FindbyteByName(String name) {
        DesignNode tempVar = getStoreRootNode().getNodes().Find(t = name.equals( > t.Text));
        return (byte) ((tempVar instanceof byte) ? tempVar : null);
    }

    //C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: internal byte Findbyte(ulong id)
    public byte Findbyte(long id) {
        DesignNode tempVar = getStoreRootNode().getNodes().Find(t -> ((byte) t).getModel().getId() == id);
        return (byte) ((tempVar instanceof byte) ? tempVar : null);
    }

    //用于前端传回的参数查找对应的设计节点
    public DesignNode FindNode(DesignNodeType type, String id) {
        switch (type) {
            case EntityModelNode:
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: return FindModelNode(ModelType.Entity, ulong.Parse(id));
                return FindModelNode(ModelType.Entity, Long.parseLong(id));
            case EnumModelNode:
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: return FindModelNode(ModelType.Enum, ulong.Parse(id));
                return FindModelNode(ModelType.Enum, Long.parseLong(id));
            case ServiceModelNode:
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: return FindModelNode(ModelType.Service, ulong.Parse(id));
                return FindModelNode(ModelType.Service, Long.parseLong(id));
            case ReportModelNode:
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: return FindModelNode(ModelType.Report, ulong.Parse(id));
                return FindModelNode(ModelType.Report, Long.parseLong(id));
            case ViewModelNode:
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: return FindModelNode(ModelType.View, ulong.Parse(id));
                return FindModelNode(ModelType.View, Long.parseLong(id));
            case ApplicationRoot:
                return getAppRootNode();
            case byte:
                return getAppRootNode().getNodes().Find(n = id.equals( > n.ID));
            case ModelRootNode: {
//C# TO JAVA CONVERTER TODO TASK: There is no equivalent to implicit typing in Java:
                var sr = id.split("[-]", -1);
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: return FindModelRootNode(uint.Parse(sr[0]), (ModelType)int.Parse(sr[1]));
                return FindModelRootNode(Integer.parseInt(sr[0]), ModelType.forValue((byte) Integer.parseInt(sr[1])));
            }
            case FolderNode:
                return FindFolderNode(id);
            case byte:
                return getStoreRootNode().getNodes().Find(t = id.equals( > t.ID));
            default:
                throw ExceptionHelper.NotImplemented(); //todo: fix others
        }
    }

    private FolderNode FindFolderNode(String id) {
        UUID folderId = UUID.fromString(id); //注意：id为Guid形式
        for (int i = 0; i < getAppRootNode().getNodes().getCount(); i++) {
//C# TO JAVA CONVERTER TODO TASK: Java has no equivalent to C# pattern variables in 'is' expressions:
//ORIGINAL LINE: if (AppRootNode.Nodes[i] is byte appNode)
            if (getAppRootNode().getNodes().getItem(i) instanceof byte appNode)
            {
//C# TO JAVA CONVERTER TODO TASK: There is no equivalent to implicit typing in Java:
                var folderNode = appNode.FindFolderNode(folderId);
                if (folderNode != null) {
                    return folderNode;
                }
            }
        }

        return null;
    }

    //C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: internal ModelRootNode FindModelRootNode(uint appID, ModelType modelType)
    public ModelRootNode FindModelRootNode(int appID, ModelType modelType) {
        for (int i = 0; i < getAppRootNode().getNodes().getCount(); i++) {
            byte appNode = (byte) getAppRootNode().getNodes().getItem(i);
            if (appNode.getModel().getId() == appID) {
                return appNode.FindModelRootNode(modelType);
            }
        }
        return null;
    }

    //根据模型类型及标识号获取相应的节点
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: internal ModelNode FindModelNode(ModelType modelType, ulong modelId)
    public ModelNode FindModelNode(ModelType modelType, long modelId) {
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: var appId = IdUtil.GetAppIdFromModelId(modelId);
        int appId = IdUtil.GetAppIdFromModelId(modelId);
        appbox.Design.ModelRootNode modelRootNode = FindModelRootNode(appId, modelType);
        if (modelRootNode == null) {
            return null;
        }

        return modelRootNode.FindModelNode(modelId);
    }

    public ModelNode[] FindNodesByType(ModelType modelType) {
        ArrayList<ModelNode> list = new ArrayList<ModelNode>();
        for (int i = 0; i < getAppRootNode().getNodes().getCount(); i++) {
            byte appNode = (byte) getAppRootNode().getNodes().getItem(i);
            appbox.Design.ModelRootNode modelRootNode = appNode.FindModelRootNode(modelType);
            list.addAll(Arrays.asList(modelRootNode.GetAllModelNodes()));
        }
        return list.toArray(new ModelNode[0]);
    }

    //查找所有引用指定模型标识的EntityRef Member集合
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: public List<EntityRefModel> FindEntityRefModels(ulong targetEntityModelID)
    public ArrayList<EntityRefModel> FindEntityRefModels(long targetEntityModelID) {
        ArrayList<EntityRefModel> rs = new ArrayList<EntityRefModel>();

        ModelNode[] ls = FindNodesByType(ModelType.Entity);
        for (int i = 0; i < ls.length; i++) {
            EntityModel model = (EntityModel) ls[i].getModel();
            //注意：不能排除自身引用，主要指树状结构的实体
            for (int j = 0; j < model.getMembers().size(); j++) {
                if (model.getMembers().get(j).getType() == EntityMemberType.EntityRef) {
                    EntityRefModel refMember = (EntityRefModel) model.getMembers().get(j);
                    //注意不排除聚合引用
                    for (int k = 0; k < refMember.getRefModelIds().size(); k++) {
                        if (refMember.getRefModelIds().get(k).equals(targetEntityModelID)) {
                            rs.add(refMember);
                        }
                    }
                }
            }

        }
        return rs;
    }

    ///// <summary>
    ///// 用于获取所有的AppID
    ///// </summary>
    //internal string[] GetAllAppIDs()
    //{
    //    var res = new string[AppRootNode.Nodes.Count];
    //    for (int i = 0; i < AppRootNode.Nodes.Count; i++)
    //    {
    //        res[i] = ((byte)AppRootNode.Nodes[i]).Model.ID;
    //    }
    //    return res;
    //}

//C# TO JAVA CONVERTER TODO TASK: There is no preprocessor in Java:
    ///#endregion

//C# TO JAVA CONVERTER TODO TASK: There is no preprocessor in Java:
    ///#region ====Find for Create====

    //用于新建时检查相同名称的模型是否已存在
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: internal ModelNode FindModelNodeByName(uint appId, ModelType type, string name)
    public ModelNode FindModelNodeByName(int appId, ModelType type, String name) {
        //TODO:***** 考虑在这里加载存储有没有相同名称的存在,或发布时检测，如改为全局Workspace没有此问题
        // dev1 -> load tree -> checkout -> add model -> publish
        // dev2 -> load tree                                 -> checkout -> add model with same name will pass
        appbox.Design.ModelRootNode modelRootNode = FindModelRootNode(appId, type);
        return modelRootNode == null ? null : modelRootNode.FindModelNodeByName(name);
    }

    //根据当前选择的节点查询新建模型的上级节点
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: public DesignNode FindNewModelParentNode(DesignNode selected, out uint appID, ModelType newModelType)
    public DesignNode FindNewModelParentNode(DesignNode selected, tangible.RefObject<Integer> appID, ModelType newModelType) {
        appID.argValue = 0;
        if (selected == null) {
            return null;
        }

        DesignNode target = null;
        tangible.RefObject<DesignNode> tempRef_target = new tangible.RefObject<DesignNode>(target);
        FindNewModelParentNodeInternal(selected, tempRef_target, appID, newModelType);
        target = tempRef_target.argValue;
        return target;
    }

    //C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: private static void FindNewModelParentNodeInternal(DesignNode node, ref DesignNode target, ref uint appID, ModelType newModelType)
    private static void FindNewModelParentNodeInternal(DesignNode node, tangible.RefObject<DesignNode> target, tangible.RefObject<Integer> appID, ModelType newModelType) {
        if (node == null) {
            return;
        }

        switch (node.getNodeType()) {
            case FolderNode:
                if (target.argValue == null) {
                    target.argValue = node;
                }
                break;
            case ModelRootNode:
                ModelRootNode modelRootNode = (ModelRootNode) node;
                if (newModelType == modelRootNode.getTargetType()) {
                    if (target.argValue == null) {
                        target.argValue = node;
                    }
                    appID.argValue = modelRootNode.getAppID();
                    return;
                }
                break;
            case byte:
                target.argValue = ((byte) node).FindModelRootNode(newModelType);
                appID.argValue = ((byte) node).getModel().getId();
                return;
        }

        DesignNode tempVar = node.getParent();
        FindNewModelParentNodeInternal((DesignNode) ((tempVar instanceof DesignNode) ? tempVar : null), target, appID, newModelType);
    }

    //根据当前选择的节点查找新建文件夹节点的上级节点
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: public DesignNode FindNewFolderParentNode(DesignNode selected, out uint appID, out ModelType modelType)
    public DesignNode FindNewFolderParentNode(DesignNode selected, tangible.RefObject<Integer> appID, tangible.RefObject<ModelType> modelType) {
        appID.argValue = 0;
        modelType.argValue = ModelType.Application;

        if (selected == null) {
            return null;
        }

//C# TO JAVA CONVERTER TODO TASK: Java has no equivalent to C# pattern variables in 'is' expressions:
//ORIGINAL LINE: if (selected is ModelRootNode rootNode)
        if (selected instanceof ModelRootNode rootNode)
        {
            appID.argValue = rootNode.AppID;
            modelType.argValue = rootNode.TargetType;
            return selected;
        }
        if (selected.getNodeType() == DesignNodeType.FolderNode) {
            ModelFolder folder = ((FolderNode) selected).getFolder();
            appID.argValue = folder.getAppId();
            modelType.argValue = folder.getTargetModelType();
            return selected;
        }

        return null;
    }

    //从上至下查找指定设计节点下的最后一个文件夹的索引号
    public int FindLastFolderIndex(DesignNode node) {
        if (node.getNodes().getCount() == 0 || ((DesignNode) node.getNodes().getItem(0)).getNodeType() != DesignNodeType.FolderNode) {
            return -1;
        }

        int r = -1;
        for (int i = 0; i < node.getNodes().getCount(); i++) {
            if (((DesignNode) node.getNodes().getItem(i)).getNodeType() == DesignNodeType.FolderNode) {
                r = i;
            } else {
                return r;
            }
        }

        return r;
    }
//C# TO JAVA CONVERTER TODO TASK: There is no preprocessor in Java:
    ///#endregion

    //C# TO JAVA CONVERTER TODO TASK: There is no preprocessor in Java:
    ///#region ====Checkout Info manager====
    private HashMap<String, CheckoutInfo> _checkouts;

    //用于签出节点成功后添加签出信息列表
    public void AddCheckoutInfos(List<CheckoutInfo> infos) {
        for (int i = 0; i < infos.size(); i++) {
            String key = CheckoutInfo.MakeKey(infos.get(i).getNodeType(), infos.get(i).getTargetID());
            if (!_checkouts.containsKey(key)) {
                _checkouts.put(key, infos.get(i));
            }
        }
    }

    //给设计节点添加签出信息，如果已签出的模型节点则用本地存储替换原模型
    public void BindCheckoutInfo(DesignNode node, boolean isNewNode) {
        //if (node.NodeType == DesignNodeType.FolderNode || !node.AllowCheckout)
        //    throw new ArgumentException("不允许绑定签出信息: " + node.NodeType.ToString());

        //先判断是否新增的
        if (isNewNode) {
            node.setCheckoutInfo(new CheckoutInfo(node.getNodeType(), node.getCheckoutInfoTargetID(), node.getVersion(), getDesignHub().getSession().getName(), getDesignHub().getSession().getLeafOrgUnitID()));
            return;
        }

        //非新增的比对服务端的签出列表
        String key = CheckoutInfo.MakeKey(node.getNodeType(), node.getCheckoutInfoTargetID());
        CheckoutInfo checkout;
        tangible.RefObject<CheckoutInfo> tempRef_checkout = new tangible.RefObject<CheckoutInfo>(checkout);
        if (_checkouts.TryGetValue(key, tempRef_checkout)) {
            checkout = tempRef_checkout.argValue;
            node.setCheckoutInfo(checkout);
            if (node.getIsCheckoutByMe()) //如果是被当前用户签出的模型
            {
//C# TO JAVA CONVERTER TODO TASK: Java has no equivalent to C# pattern variables in 'is' expressions:
//ORIGINAL LINE: if (node is ModelNode modelNode)
                if (node instanceof ModelNode modelNode)
                {
                    //从本地缓存加载
                    appbox.Models.ModelBase stagedModel = getStaged().FindModel(modelNode.Model.Id);
                    if (stagedModel != null) {
                        modelNode.Model = stagedModel;
                    }
                }
            }
        } else {
            checkout = tempRef_checkout.argValue;
        }
    }

    //部署完后更新所有模型节点的状态，并移除待删除的节点
    public void CheckinAllNodes() {
        //循环更新模型节点
        for (int i = 0; i < getAppRootNode().getNodes().getCount(); i++) {
            ((byte) getAppRootNode().getNodes().getItem(i)).CheckinAllNodes();
        }

        //刷新签出信息表，移除被自己签出的信息
        ArrayList<String> list = new ArrayList<String>();
//C# TO JAVA CONVERTER TODO TASK: There is no equivalent to implicit typing in Java:
        for (var key : _checkouts.keySet()) {
            if (_checkouts.get(key).getDeveloperOuid().equals(RuntimeContext.getCurrent().getCurrentSession().getLeafOrgUnitID())) {
                list.add(key);
            }
        }
        for (int i = 0; i < list.size(); i++) {
            _checkouts.remove(list.get(i));
        }
    }*/
//C# TO JAVA CONVERTER TODO TASK: There is no preprocessor in Java:
    ///#endregion
}
