package appbox.design.services.code;

import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.LambdaExpression;
import org.eclipse.jdt.core.dom.MethodInvocation;

final class SqlQueryWhereInterceptor implements IMethodInterceptor {

    //q.where(t -> t.name == "Rick")

    @Override
    public boolean visit(MethodInvocation node, ServiceCodeGenerator generator) {
        var lambda = (LambdaExpression) node.arguments().get(node.arguments().size() - 1);
        //暂不允许Block的表达式
        if (lambda.getBody() instanceof Block)
            throw new RuntimeException("Block is not supported now");

        var lambdaVisitor = new SqlQueryLambdaVisitor(node, lambda, generator);
        lambda.getBody().accept(lambdaVisitor);

        return false;
    }

}
