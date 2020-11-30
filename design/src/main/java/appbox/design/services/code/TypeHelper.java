package appbox.design.services.code;

import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.TypeDeclaration;

final class TypeHelper {

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
