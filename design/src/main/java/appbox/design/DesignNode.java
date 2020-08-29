package appbox.design;

import appbox.runtime.RuntimeContext;
import appbox.serialization.PayloadType;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public abstract class DesignNode
{

    private DesignNode Parent;
    public final DesignNode getParent()
    {
        return Parent;
    }
    public final void setParent(DesignNode value)
    {
        Parent = value;
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

    /**
     用于前端回传时识别是哪个节点
     */
    public String getID()
    {
        return getText();
    }

    private NodeCollection Nodes;
    public final NodeCollection getNodes()
    {
        return Nodes;
    }

    public abstract DesignNodeType getNodeType();
    public int getSortNo()
    {
        return Integer.MAX_VALUE;
    }

    private String Text;
    public String getText()
    {
        return Text;
    }
    public void setText(String value)
    {
        Text = value;
    }

    //region ====Checkout相关属性====
    private int Version;
    public int getVersion()
    {
        return Version;
    }
    public void setVersion(int value)
    {
        Version = value;
    }

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

    public DesignNode()
    {
        Nodes = new NodeCollection(this);
    }

    /**
     目前仅支持签出ModelRootNode及ModelNode
     */
//C# TO JAVA CONVERTER TODO TASK: There is no equivalent in Java to the 'async' keyword:
//ORIGINAL LINE: public virtual async Task<bool> Checkout()
    /*public CompletableFuture<Boolean> Checkout() //TODO:考虑加入参数允许签出所有下属节点
    {
        //判断是否已签出或者能否签出
        if (!getAllowCheckout())
        {
            return false;
        }
        if (getIsCheckoutByMe())
        {
            return true;
        }

        //调用签出服务
        List<CheckoutInfo> infos = new ArrayList<CheckoutInfo>();
        CheckoutInfo       info  = new CheckoutInfo(getNodeType(), getCheckoutInfoTargetID(), getVersion(), getDesignTree().getDesignHub().getSession().getName(), getDesignTree().getDesignHub().getSession().getLeafOrgUnitID());
        infos.add(info);
//C# TO JAVA CONVERTER TODO TASK: There is no equivalent to 'await' in Java:
//        CheckoutResult result = await CheckoutService.CheckoutAsync(infos);

        //CompletableFuture<CheckoutResult> future = CompletableFuture.supplyAsync(() -> CheckoutService.CheckoutAsync(infos));
        if (result.getSuccess())
        {
            //签出成功则将请求的签出信息添加至当前的已签出列表
            getDesignTree().AddCheckoutInfos(infos);
            //如果签出的是单个模型，且具备更新的版本，则更新
//C# TO JAVA CONVERTER TODO TASK: Java has no equivalent to C# pattern variables in 'is' expressions:
//ORIGINAL LINE: if (this is ModelNode modelNode && result.ModelWithNewVersion != null)
            if (this instanceof ModelNode modelNode && result.getModelWithNewVersion() != null)
            {
                modelNode.Model = result.getModelWithNewVersion(); //替换旧模型
//C# TO JAVA CONVERTER TODO TASK: There is no equivalent to 'await' in Java:
                await getDesignTree().getDesignHub().getTypeSystem().UpdateModelDocumentAsync(modelNode); //更新为新模型的RoslynDocument
            }
            //更新当前节点的签出信息
            setCheckoutInfo(infos.get(0));
        }

        return result.getSuccess();
    }*/

    private static DesignNode GetRootNode(DesignNode current)
    {
        return current.getParent() == null ? current : GetRootNode(current.getParent());
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
