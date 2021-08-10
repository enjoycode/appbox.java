package appbox.design.lang.java;

import appbox.model.ModelType;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.ls.core.internal.JDTUtils;

/** 用于查找模型对应的类型 */
public final class ModelSymbolFinder {

    private final JdtLanguageServer languageServer;

    public ModelSymbolFinder(JdtLanguageServer languageServer) {
        this.languageServer = languageServer;
    }

    /** 根据指定的模型类型及标识号获取相应的虚拟类的类型 */
    public IJavaElement getModelSymbol(ModelType modelType, String appName, String modelName) {
        final var file = languageServer.findFileFromModelsProject(modelType, appName, modelName);
        return JDTUtils.resolveCompilationUnit(file);
    }

}
