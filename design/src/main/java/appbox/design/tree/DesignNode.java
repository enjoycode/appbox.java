package appbox.design.tree;

import appbox.design.common.CheckoutInfo;
import appbox.design.services.CheckoutService;
import appbox.serialization.IJsonSerializable;
import com.alibaba.fastjson.JSONWriter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public abstract class DesignNode implements Comparable<DesignNode>, IJsonSerializable {

    private   DesignNode   parent;
    protected String       text; //TODO: remove it，改为抽象
    private   int          version;
    private   CheckoutInfo _checkoutInfo;

    protected final NodeCollection nodes = new NodeCollection(this);

    public abstract DesignNodeType nodeType();

    //region ====Properties====

    /**
     * 用于前端回传时识别是哪个节点
     */
    public String id() {
        return getText();
    }

    public String getText() {
        return text;
    }

    public final DesignNode getParent() {
        return parent;
    }

    public final void setParent(DesignNode value) {
        parent = value;
    }

    public DesignTree getDesignTree() {
        DesignNode root = GetRootNode(this);
        if (root instanceof ITopNode) {
            return root.getDesignTree();
        }
        return null;
    }
    //endregion

    //region ====Checkout相关属性====

    /**
     * 是否允许签出
     */
    public boolean getAllowCheckout() {
        if (nodeType() == DesignNodeType.ModelRootNode
                || nodeType().value >= DesignNodeType.EntityModelNode.value
                || nodeType() == DesignNodeType.DataStoreNode) {
            return true;
        }
        //TODO:根据证书判断
        return false;
    }

    /**
     * 节点的签出信息
     */
    public CheckoutInfo getCheckoutInfo() {
        return _checkoutInfo;
    }

    public void setCheckoutInfo(CheckoutInfo value) {
        if (!value.equals(_checkoutInfo)) {
            _checkoutInfo = value;
            //this.OnPropertyChanged("CheckoutInfo");
            //this.OnPropertyChanged("CheckoutImageVisibility");
        }
    }

    /**
     * 节点签出信息的标识
     */
    public String getCheckoutInfoTargetID() {
        return getText();
    }

    /**
     * 设计节点是否被当前用户签出
     */
    public final boolean getIsCheckoutByMe() {
        //todo getCurrent session
        return false;
        //return getCheckoutInfo() != null && getCheckoutInfo().getDeveloperOuid().equals(RuntimeContext.getCurrent().getCurrentSession().getLeafOrgUnitID());
    }
    //endregion

    /**
     * 目前仅支持签出ModelRootNode及ModelNode
     */
    public CompletableFuture<Boolean> Checkout() throws ExecutionException, InterruptedException //TODO:考虑加入参数允许签出所有下属节点
    {
        //判断是否已签出或者能否签出
        if (!getAllowCheckout()) {
            return CompletableFuture.completedFuture(false);
        }
        if (getIsCheckoutByMe()) {
            return CompletableFuture.completedFuture(true);
        }

        //调用签出服务
        List<CheckoutInfo> infos = new ArrayList<CheckoutInfo>();
        //CheckoutInfo info = new CheckoutInfo(getNodeType(), getCheckoutInfoTargetID(), getVersion(), getDesignTree().getDesignHub().getSession().getName(), getDesignTree().getDesignHub().getSession().getLeafOrgUnitID());
        CheckoutInfo info = new CheckoutInfo();
        //TODO set param
        infos.add(info);

        return CheckoutService.CheckoutAsync(infos).thenApply(r -> {
            if (r.getSuccess()) {
                //签出成功则将请求的签出信息添加至当前的已签出列表
                getDesignTree().AddCheckoutInfos(infos);
                //如果签出的是单个模型，且具备更新的版本，则更新
//C# TO JAVA CONVERTER TODO TASK: Java has no equivalent to C# pattern variables in 'is' expressions:
//ORIGINAL LINE: if (this is ModelNode modelNode && result.ModelWithNewVersion != null)
//            if (this instanceof ModelNode modelNode && result.getModelWithNewVersion() != null)
//            {
//                modelNode.Model = result.getModelWithNewVersion(); //替换旧模型
////C# TO JAVA CONVERTER TODO TASK: There is no equivalent to 'await' in Java:
//                await getDesignTree().getDesignHub().getTypeSystem().UpdateModelDocumentAsync(modelNode); //更新为新模型的RoslynDocument
//            }
                //更新当前节点的签出信息
                setCheckoutInfo(infos.get(0));
                return true;
            } else {
                return false;
            }

        });

    }

    private static DesignNode GetRootNode(DesignNode current) {
        return current.getParent() == null ? current : GetRootNode(current.getParent());
    }

    //region ====Comparable====
    @Override
    public final int compareTo(DesignNode designNode) {
        if (nodeType() == designNode.nodeType()) {
            return String.CASE_INSENSITIVE_ORDER.compare(getText(), designNode.getText());
        }
        return Byte.compare(nodeType().value, designNode.nodeType().value);
    }
    //endregion

    //region ====IJsonSerializable====
    @Override
    public final void writeToJson(JSONWriter writer) {
        writer.startObject();

        writer.writeKey("ID");
        writer.writeValue(id());

        writer.writeKey("Type");
        writer.writeValue(nodeType().value);

        writer.writeKey("Text");
        writer.writeValue(getText());

        if (!(this instanceof ModelNode)) {
            writer.writeKey("Nodes");
            nodes.writeToJson(writer);
        }

        //TODO:签出信息

        //写入子类成员
        writeJsonMembers(writer);

        writer.endObject();
    }

    protected void writeJsonMembers(JSONWriter writer) {
    }
    //endregion

}
