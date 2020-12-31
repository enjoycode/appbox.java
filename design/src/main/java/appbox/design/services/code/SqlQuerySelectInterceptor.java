package appbox.design.services.code;

import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.LambdaExpression;
import org.eclipse.jdt.core.dom.MethodInvocation;

public final class SqlQuerySelectInterceptor implements IMethodInterceptor {

    //q.toSubQuery(e -> e.Age) -> q.toSubQuery(e -> e.m("Age"))

    @Override
    public boolean visit(MethodInvocation node, ServiceCodeGenerator generator) {
        var lambda = (LambdaExpression) node.arguments().get(node.arguments().size() - 1);
        if (lambda.getBody() instanceof ClassInstanceCreation || lambda.getBody() instanceof Block) {
            throw new RuntimeException("Not supported now");
        }

        var lambdaVisitor = new SqlQueryLambdaVisitor(node, lambda, generator);
        lambda.getBody().accept(lambdaVisitor);

        return false;
    }

}
