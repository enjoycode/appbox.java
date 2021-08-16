package appbox.design.common;

import appbox.design.tree.ModelNode;
import appbox.serialization.IJsonWriter;
import org.eclipse.lsp4j.Range;

/** 模型虚拟代码的引用 */
public final class CodeReference extends Reference {

    private final Range range;

    //TODO:加入语句类型,如读写属性/创建实例等

    public CodeReference(ModelNode modelNode, Range range) {
        super(modelNode);

        this.range = range;
    }

    @Override
    public String location() {
        return String.format("[%d:%d] - [%d:%d]",
                range.getStart().getLine() + 1, range.getStart().getCharacter() + 1,
                range.getEnd().getLine() + 1, range.getEnd().getCharacter() + 1);
    }

    @Override
    protected void writeMember(IJsonWriter writer) {
        writer.writeKeyValue("StartLine", range.getStart().getLine() + 1);
        writer.writeKeyValue("StartColumn", range.getStart().getCharacter() + 1);
        writer.writeKeyValue("EndLine", range.getEnd().getLine() + 1);
        writer.writeKeyValue("EndColumn", range.getEnd().getCharacter() + 1);
    }

    @Override
    protected int compareSameModel(Reference other) {
        final var o = (CodeReference) other;

        if (range.getStart().getLine() != o.range.getStart().getLine())
            return Integer.compare(range.getStart().getLine(), o.range.getStart().getLine());
        return Integer.compare(range.getStart().getCharacter(), o.range.getStart().getCharacter());
    }
}
