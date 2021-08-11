package appbox.design.common;

import appbox.design.tree.ModelNode;
import appbox.serialization.IJsonWriter;

/** 模型虚拟代码的引用 */
public final class CodeReference extends Reference {

    private final int offset;
    private final int length;

    public CodeReference(ModelNode modelNode, int offset, int length) {
        super(modelNode);

        this.offset = offset;
        this.length = length;
    }

    @Override
    public String location() {
        return String.format("[%d - %d]", offset, length);
    }

    @Override
    protected void writeMember(IJsonWriter writer) {
        writer.writeKeyValue("Offset", offset);
        writer.writeKeyValue("Length", length);
    }

    @Override
    protected int compareSameModel(Reference other) {
        return Integer.compare(offset, ((CodeReference) other).offset);
    }
}
