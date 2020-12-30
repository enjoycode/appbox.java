package appbox.design.services.code;

import org.eclipse.jdt.core.dom.*;

public final class TypeHelper {

    public static ITypeBinding getEntityType(SimpleType node) {
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
        return pkg.getPath().lastSegment().equals("entities");
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

    private static Object getAnnotationValue(IAnnotationBinding[] annotations, String annotation) {
        if (annotations == null || annotations.length == 0)
            return null;
        for (var item : annotations) {
            if (item.getName().equals(annotation)) {
                return item.getDeclaredMemberValuePairs()[0].getValue();
            }
        }
        return null;
    }

    /** 获取运行时类型，没有返回null */
    public static String getRuntimeType(ITypeBinding type) {
        return (String) getAnnotationValue(type.getAnnotations(), "RuntimeType");
    }

    /** 判断是否具有构造拦截器，没有返回null，有则返回拦截器名称 */
    public static String getCtorInterceptor(ITypeBinding type) {
        return (String) getAnnotationValue(type.getAnnotations(), "CtorInterceptor");
    }

    /** 判断是否具有方法调用拦截器，没有返回null，有则返回拦截器名称 */
    public static String getMethodInterceptor(IMethodBinding method) {
        return (String) getAnnotationValue(method.getAnnotations(), "MethodInterceptor");
    }

}
