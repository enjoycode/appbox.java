package appbox.design.lang.java;

import appbox.model.ModelType;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.ls.core.internal.JDTUtils;

import java.util.Arrays;

/** 用于查找模型对应的类型 */
public final class ModelSymbolFinder {

    private final JdtLanguageServer languageServer;

    public ModelSymbolFinder(JdtLanguageServer languageServer) {
        this.languageServer = languageServer;
    }

    /** 根据指定的模型类型及标识号获取相应的虚拟类的类型 */
    public IType getModelSymbol(ModelType modelType, String appName, String modelName) {
        final var file = languageServer.findFileFromModelsProject(modelType, appName, modelName);
        final var cu   = JDTUtils.resolveCompilationUnit(file);
        try {
            return cu.getTypes()[0];
        } catch (JavaModelException e) {
            e.printStackTrace();
            return null;
        }
    }

    /** 获取实体模型成员的虚拟类对应的Field成员 */
    public IField getEntityMemberSymbol(String appName, String entityModelName, String memberName) {
        final var entitySymbol = getModelSymbol(ModelType.Entity, appName, entityModelName);
        if (entitySymbol == null) return null;
        try {
            final var member= Arrays.stream(entitySymbol.getFields())
                    .filter(f ->f.getElementName().equals(memberName)).findFirst();
            return member.isEmpty() ? null : member.get();
        } catch (JavaModelException e) {
            e.printStackTrace();
            return null;
        }
    }

}
