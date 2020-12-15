package appbox.design.services.code;

import org.eclipse.jdt.core.dom.*;

final class TypeHelper {

    public static ITypeBinding isEntityClass(SimpleType node) {
        //TODO:忽略常规类型如String
        var type = node.resolveBinding();
        return isEntityType(type) ? type : null;
    }

    public static boolean isEntityType(ITypeBinding type) {
        if (type == null || !type.isFromSource())
            return false;

        var pkg = type.getPackage().getJavaElement();
        if (pkg == null)
            return false;
        if (pkg.getPath().lastSegment().equals("entities"))
            return true;
        return false;
    }

    public static boolean isDataStoreType(ITypeBinding type) {
        if (type == null || !type.isFromSource())
            return false;

        var pkg = type.getPackage();
        return pkg != null && pkg.isUnnamed() && type.getName().equals("DataStore");
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

    /** 判断是否具有构造拦截器，没有返回null，有则返回拦截器名称 */
    public static String getCtorInterceptor(ITypeBinding type) {
        var annotations = type.getAnnotations();
        if (annotations == null || annotations.length == 0)
            return null;
        for (var item : annotations) {
            if (item.getName().equals("CtorInterceptor")) {
                return (String)item.getDeclaredMemberValuePairs()[0].getValue();
            }
        }
        return null;
    }

    /** 判断是否具有方法调用拦截器，没有返回null，有则返回拦截器名称 */
    public static String getMethodInterceptor(IMethodBinding method) {
        var annotations = method.getAnnotations();
        if (annotations == null || annotations.length == 0)
            return null;
        for (var item : annotations) {
            if(item.getName().equals("MethodInterceptor")) {
                return (String) item.getDeclaredMemberValuePairs()[0].getValue();
            }
        }
        return null;
    }

}
