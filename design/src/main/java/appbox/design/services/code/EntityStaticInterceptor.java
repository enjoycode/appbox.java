package appbox.design.services.code;

import org.eclipse.jdt.core.dom.MethodInvocation;

/** 调用实体静态方法的拦截器 eg: sys.entities.Employee.fetchAsync() */
public final class EntityStaticInterceptor implements IMethodInterceptor {

    @Override
    public boolean visit(MethodInvocation node, ServiceCodeGenerator generator) {
        var entityType      = node.getExpression().resolveTypeBinding();
        var modelNode       = generator.getUsedEntity(entityType);
        var entityClassName = EntityCodeGenerator.makeEntityClassName(modelNode);

        var newExp = generator.ast.newSimpleName(entityClassName);
        generator.astRewrite.replace(node.getExpression(), newExp, null);

        return true; //返回true继续处理参数
    }

}
