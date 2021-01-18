package appbox.design.services.code;

import com.ea.async.Async;
import org.eclipse.jdt.core.dom.MethodInvocation;

final class AsyncAwaitInterceptor implements IMethodInterceptor {

    @Override
    public boolean visit(MethodInvocation node, ServiceCodeGenerator generator) {
        //TODO:Debug服务转换为调试方法
        generator.hasAwaitInvocation = true;

        var newExp = generator.ast.newName(Async.class.getName());
        if (node.getExpression() != null) {
            generator.astRewrite.replace(node.getExpression(), newExp, null);
        } else {
            generator.astRewrite.set(node, MethodInvocation.EXPRESSION_PROPERTY, newExp, null);
        }

        return true; //注意返回true继续处理参数
    }

}
