package appbox.design.services.code;

import org.eclipse.jdt.core.dom.MethodInvocation;

/** 调用虚拟静态方法的拦截器 eg: appbox.store.KVTransaction.beginAsync() */
public final class InvokeStaticInterceptor implements IMethodInterceptor {

    @Override
    public boolean visit(MethodInvocation node, ServiceCodeGenerator generator) {
        final var ownerType = node.getExpression().resolveTypeBinding();
        final var runtimeType = TypeHelper.getRuntimeType(ownerType);
        final var newExp      = generator.ast.newName(runtimeType);
        generator.astRewrite.replace(node.getExpression(), newExp, null);

        return true; //返回true继续处理参数
    }

}
