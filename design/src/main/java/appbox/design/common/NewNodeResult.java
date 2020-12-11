package appbox.design.common;

import appbox.design.tree.DesignNode;
import appbox.serialization.IJsonSerializable;
import appbox.serialization.IJsonWriter;

/** 调用新建节点后返回的结果(JSON) */
public final class NewNodeResult implements IJsonSerializable {
    public int ParentNodeType;
    public String ParentNodeID;
    public DesignNode NewNode;
    /** 用于判断模型根节点是否已签出(非自动签出)，用于前端判断是否需要刷新模型根节点 */
    public String RootNodeID;
    /** 用于前端处理插入点，由后端排好序后返回给前端，省得前端处理排序问题 */
    public int InsertIndex;

    @Override
    public void writeToJson(IJsonWriter writer) {
        writer.startObject();

        writer.writeKeyValue("ParentNodeType", ParentNodeType);
        writer.writeKeyValue("ParentNodeID", ParentNodeID);
        writer.writeKey("NewNode");
        NewNode.writeToJson(writer);
        if (RootNodeID != null && !RootNodeID.isEmpty())
            writer.writeKeyValue("RootNodeID", RootNodeID);
        writer.writeKeyValue("InsertIndex", InsertIndex);

        writer.endObject();
    }
}
