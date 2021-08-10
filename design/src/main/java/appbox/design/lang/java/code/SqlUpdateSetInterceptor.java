package appbox.design.lang.java.code;

import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.LambdaExpression;
import org.eclipse.jdt.core.dom.MethodInvocation;

final class SqlUpdateSetInterceptor implements IMethodInterceptor {

    //cmd.update(e -> e.Name = "Rick")   -> cmd.update(e -> e.m("Name").set("Rick"));

    @Override
    public boolean visit(MethodInvocation node, ServiceCodeGenerator generator) {
        var lambda = (LambdaExpression) node.arguments().get(node.arguments().size() - 1);
        //暂不允许Block的表达式
        if (lambda.getBody() instanceof Block)
            throw new RuntimeException("Block is not supported now");
        //暂只允许Assignment
        if (!(lambda.getBody() instanceof Assignment))
            throw new RuntimeException("Only assignment allowed");

        var lambdaVisitor = new SqlQueryLambdaVisitor(node, lambda, generator);
        lambda.getBody().accept(lambdaVisitor);

        return false;
    }

}
