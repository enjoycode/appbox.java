package appbox.design.common;

import appbox.design.tree.ModelNode;
import appbox.serialization.IJsonSerializable;
import appbox.serialization.IJsonWriter;

/**
 * 模型或其成员的引用者的基类，目前分为两类：
 * 1. 模型对模型的引用
 * 2. 模型内虚拟代码对模型的引用
 */
public abstract class Reference implements Comparable<Reference>, IJsonSerializable {

    public final ModelNode modelNode;

    public Reference(ModelNode modelNode) {
        this.modelNode = modelNode;
    }

    /** 用于友好显示的位置信息 */
    public abstract String location();

    @Override
    public final int compareTo(Reference other) {
        if (modelNode.model().modelType() != other.modelNode.model().modelType()
                || modelNode.model().id() != other.modelNode.model().id()) {
            return Long.compare(modelNode.model().id(), other.modelNode.model().id());
        }

        return compareSameModel(other);
    }

    protected int compareSameModel(Reference other) {
        return 0;
    }

    @Override
    public final void writeToJson(IJsonWriter writer) {
        writer.startObject();
        writer.writeKeyValue("Type", modelNode.model().modelType().toString());
        writer.writeKeyValue("Model", String.format("%s.%s", modelNode.appNode.model.name(), modelNode.model().name()));
        writer.writeKeyValue("Location", location());

        writeMember(writer);
        writer.endObject();
    }

    protected void writeMember(IJsonWriter writer) {}
}
