package appbox.design;

import appbox.serialization.PayloadType;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public abstract class DesignNode
{

    private DesignNode parent;

    private NodeCollection nodes;

    public abstract DesignNodeType getNodeType();

    private String text;

    public DesignNode()
    {
        nodes = new NodeCollection(this);
    }

    //region ====Checkout相关属性====
    private int version;

    /**
     是否允许签出
     */
    public boolean getAllowCheckout()
    {
        if (getNodeType() == DesignNodeType.ModelRootNode || getNodeType().getValue() >= DesignNodeType.EntityModelNode.getValue() || getNodeType() == DesignNodeType.DataStoreNode)
        {
            return true;
        }
        //TODO:根据证书判断
        return false;
    }

    private CheckoutInfo _checkoutInfo;
    /**
     节点的签出信息
     */
    public CheckoutInfo getCheckoutInfo()
    {
        return _checkoutInfo;
    }
    public void setCheckoutInfo(CheckoutInfo value)
    {
        if (!value.equals(_checkoutInfo))
        {
            _checkoutInfo = value;
            //this.OnPropertyChanged("CheckoutInfo");
            //this.OnPropertyChanged("CheckoutImageVisibility");
        }
    }

    /**
     节点签出信息的标识
     */
    public String getCheckoutInfoTargetID()
    {
        return getText();
    }

    /**
     设计节点是否被当前用户签出
     */
    public final boolean getIsCheckoutByMe()
    {
        //todo getCurrent session
        return false;
        //return getCheckoutInfo() != null && getCheckoutInfo().getDeveloperOuid().equals(RuntimeContext.getCurrent().getCurrentSession().getLeafOrgUnitID());
    }
    //endregion

    /**
     目前仅支持签出ModelRootNode及ModelNode
     */
    public CompletableFuture<Boolean> Checkout() throws ExecutionException, InterruptedException //TODO:考虑加入参数允许签出所有下属节点
    {
        //判断是否已签出或者能否签出
        if (!getAllowCheckout())
        {
            return CompletableFuture.completedFuture(false);
        }
        if (getIsCheckoutByMe())
        {
            return CompletableFuture.completedFuture(true);
        }

        //调用签出服务
        List<CheckoutInfo> infos = new ArrayList<CheckoutInfo>();
        //CheckoutInfo info = new CheckoutInfo(getNodeType(), getCheckoutInfoTargetID(), getVersion(), getDesignTree().getDesignHub().getSession().getName(), getDesignTree().getDesignHub().getSession().getLeafOrgUnitID());
        CheckoutInfo info = new CheckoutInfo();
        //TODO set param
        infos.add(info);

        return CheckoutService.CheckoutAsync(infos).thenApply(r->{
            if(r.getSuccess()){
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
            }else{
                return false;
            }

        });

    }

    private static DesignNode GetRootNode(DesignNode current)
    {
        return current.getParent() == null ? current : GetRootNode(current.getParent());
    }

    public final DesignNode getParent()
    {
        return parent;
    }
    public final void setParent(DesignNode value)
    {
        parent = value;
    }

    public DesignTree getDesignTree()
    {
        DesignNode root = GetRootNode(this);
        if (root instanceof ITopNode)
        {
            return root.getDesignTree();
        }
        return null;
    }

    public final NodeCollection getNodes()
    {
        return nodes;
    }
    public int getSortNo()
    {
        return Integer.MAX_VALUE;
    }
    public String getText()
    {
        return text;
    }
    public void setText(String value)
    {
        text = value;
    }
    public int getVersion()
    {
        return version;
    }
    public void setVersion(int value)
    {
        version = value;
    }
    /**
     用于前端回传时识别是哪个节点
     */
    public String getID()
    {
        return getText();
    }

/*    public final int CompareTo(DesignNode other)
    {
        if (getNodeType() == other.getNodeType())
        {
            return String.Compare(getText(), other.getText(), StringComparison.Ordinal);
        }
        return (new Integer(getNodeType().getValue())).compareTo(other.getNodeType().getValue());
    }*/

    //region ====JsonSerialization====
    // string IJsonSerializable.JsonObjID => this.ID;

    private byte getJsonPayloadType()
    {
        return PayloadType.UnknownType;
    }

    /*public final void WriteToJson(Utf8JsonWriter writer, WritedObjects objrefs)
    {
        writer.WriteString("ID", getID());
        writer.WriteNumber("Type", getNodeType().getValue());
        writer.WriteString("Text", getText());
        if (!(this instanceof ModelNode))
        {
            writer.WritePropertyName("Nodes");
            writer.WriteStartArray();
            for (int i = 0; i < getNodes().getCount(); i++)
            {
                appbox.Serialization.JsonExtensions.Serialize(writer, getNodes().getItem(i), objrefs);
            }
            writer.WriteEndArray();
        }

        if (getCheckoutInfo() != null)
        {
            writer.WritePropertyName("CheckoutBy");
            if (getIsCheckoutByMe())
            {
                writer.WriteStringValue("Me");
            }
            else
            {
                writer.WriteStringValue(getCheckoutInfo().getDeveloperName());
            }
        }

        WriteMembers(writer, objrefs);
    }

    *//**
     用于继承类重写。来写入子类的成员

     @param writer
     @param objrefs
     *//*
    public void WriteMembers(Utf8JsonWriter writer, WritedObjects objrefs)
    {
    }

    public final void ReadFromJson(tangible.RefObject<Utf8JsonReader> reader, ReadedObjects objrefs)
    {
        throw new UnsupportedOperationException();
    }*/
    //endregion

}
