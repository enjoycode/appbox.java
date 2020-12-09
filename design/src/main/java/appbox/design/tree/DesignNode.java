package appbox.design.tree;

import appbox.design.common.CheckoutInfo;
import appbox.design.services.CheckoutService;
import appbox.runtime.RuntimeContext;
import appbox.serialization.IJsonSerializable;
import appbox.serialization.IJsonWriter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public abstract class DesignNode implements Comparable<DesignNode>, IJsonSerializable {

    private   DesignNode   parent;
    protected String       text; //TODO: remove it，改为抽象
    private   CheckoutInfo _checkoutInfo;

    public final NodeCollection nodes = new NodeCollection(this);

    public abstract DesignNodeType nodeType();

    //region ====Properties====

    /**
     * 用于前端回传时识别是哪个节点
     */
    public String id() {
        return text();
    }

    public String text() {
        return text;
    }

    public int version() { throw new UnsupportedOperationException(); }

    public final DesignNode getParent() {
        return parent;
    }

    public final void setParent(DesignNode value) {
        parent = value;
    }

    public DesignTree designTree() {
        DesignNode root = getRootNode(this);
        if (root instanceof ITopNode) {
            return root.designTree();
        }
        return null;
    }
    //endregion

    //region ====Checkout相关属性====

    /**
     * 是否允许签出
     */
    public boolean allowCheckout() {
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
        _checkoutInfo = value;
    }

    /**
     * 节点签出信息的标识
     */
    public String checkoutInfoTargetID() {
        return text();
    }

    /**
     * 设计节点是否被当前用户签出
     */
    public final boolean isCheckoutByMe() {
        return _checkoutInfo != null
                && _checkoutInfo.developerOuid.equals(
                RuntimeContext.current().currentSession().leafOrgUnitId());
    }
    //endregion

    /**
     * 目前仅支持签出ModelRootNode及ModelNode
     * TODO:考虑加入参数允许签出所有下属节点
     */
    public CompletableFuture<Boolean> checkout() {
        //判断是否已签出或者能否签出
        if (!allowCheckout()) {
            return CompletableFuture.completedFuture(false);
        }
        if (isCheckoutByMe()) {
            return CompletableFuture.completedFuture(true);
        }

        //调用签出服务
        List<CheckoutInfo> infos = new ArrayList<>();
        CheckoutInfo info = new CheckoutInfo(nodeType(),
                checkoutInfoTargetID(), version(),
                designTree().designHub.session.name(),
                designTree().designHub.session.leafOrgUnitId());
        infos.add(info);

        return CheckoutService.checkoutAsync(infos).thenApply(r -> {
            if (r.success) {
                //签出成功则将请求的签出信息添加至当前的已签出列表
                designTree().addCheckoutInfos(infos);
                //如果签出的是单个模型，且具备更新的版本，则更新
                if (this instanceof ModelNode && r.modelWithNewVersion != null) {
                    var modelNode = (ModelNode) this;
                    modelNode.setModel(r.modelWithNewVersion); //替换旧模型
                    designTree().designHub.typeSystem.updateModelDocument(modelNode); //更新为新模型的虚拟代码
                }
                //更新当前节点的签出信息
                setCheckoutInfo(infos.get(0));
            }
            return r.success;
        });
    }

    private static DesignNode getRootNode(DesignNode current) {
        return current.getParent() == null ? current : getRootNode(current.getParent());
    }

    //region ====Comparable====
    @Override
    public final int compareTo(DesignNode designNode) {
        if (nodeType() == designNode.nodeType()) {
            return String.CASE_INSENSITIVE_ORDER.compare(text(), designNode.text());
        }
        return Byte.compare(nodeType().value, designNode.nodeType().value);
    }
    //endregion

    //region ====IJsonSerializable====
    @Override
    public final void writeToJson(IJsonWriter writer) {
        writer.startObject();

        writer.writeKeyValue("ID", id());
        writer.writeKeyValue("Type", nodeType().value);
        writer.writeKeyValue("Text", text());

        if (!(this instanceof ModelNode)) {
            writer.writeKey("Nodes");
            nodes.writeToJson(writer);
        }

        //签出信息
        if (_checkoutInfo != null) {
            writer.writeKey("CheckoutBy");
            writer.writeValue(isCheckoutByMe() ? "Me" : _checkoutInfo.developerName);
        }

        //写入子类成员
        writeJsonMembers(writer);

        writer.endObject();
    }

    protected void writeJsonMembers(IJsonWriter writer) {
    }
    //endregion

}
