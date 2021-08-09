package appbox.design.services.code;

import org.eclipse.jdt.core.dom.MethodInvocation;

/** 调用虚拟静态方法的拦截器 eg: sys.entities.Employee.fetchAsync() */
public final class InvokeStaticInterceptor implements IMethodInterceptor {

    @Override
    public boolean visit(MethodInvocation node, ServiceCodeGenerator generator) {
        var entityType = node.getExpression().resolveTypeBinding();
        if (TypeHelper.isEntityType(entityType)) {
            var modelNode       = generator.getUsedEntity(entityType);
            var entityClassName = EntityCodeGenerator.makeEntityClassName(modelNode);

            var newExp = generator.ast.newSimpleName(entityClassName);
            generator.astRewrite.replace(node.getExpression(), newExp, null);
        } else {
            var runtimeType = TypeHelper.getRuntimeType(entityType);
            var newExp      = generator.ast.newName(runtimeType);
            generator.astRewrite.replace(node.getExpression(), newExp, null);
        }

        return true; //返回true继续处理参数
    }

}
