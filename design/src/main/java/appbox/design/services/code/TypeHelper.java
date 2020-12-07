package appbox.design.services.code;

import appbox.design.DesignHub;
import org.eclipse.jdt.core.dom.*;

final class TypeHelper {

    public static ITypeBinding isEntityClass(SimpleType node, DesignHub hub) {
        //TODO:忽略常规类型如String
        var type = node.resolveBinding();
        if (!type.isFromSource())
            return null;
        var pkg = type.getPackage().getJavaElement();
        if (pkg == null)
            return null;
        if (pkg.getPath().lastSegment().equals("entities"))
            return type;
        return null;
    }

    public static boolean isServiceClass(TypeDeclaration node, String appName, String serviceName) {
        //TODO:暂简单判断
        return node.getRoot() == node.getParent();
    }

    public static boolean isServiceMethod(MethodDeclaration node) {
        //TODO:暂简单判断方法是否public，还需要判断返回类型
        boolean foundPublicModifier = false;
        for (var item : node.modifiers()) {
            if (((Modifier) item).isPublic()) {
                foundPublicModifier = true;
                break;
            }
        }
        return foundPublicModifier;
    }

}
