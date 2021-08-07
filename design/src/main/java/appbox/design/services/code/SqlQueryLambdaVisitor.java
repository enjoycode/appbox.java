package appbox.design.services.code;

import appbox.logging.Log;
import appbox.store.DbFunc;
import org.eclipse.jdt.core.dom.*;

import java.util.HashMap;
import java.util.Map;

final class SqlQueryLambdaVisitor extends ServiceCodeGeneratorProxy {

    private final MethodInvocation method;
    private final LambdaExpression lambda;

    private final Map<String, Object> lambdaParameters = new HashMap<>();

    public SqlQueryLambdaVisitor(MethodInvocation method, LambdaExpression lambda, ServiceCodeGenerator generator) {
        super(generator);
        this.method = method;
        this.lambda = lambda;

        for (var lp : lambda.parameters()) {
            if (lp instanceof VariableDeclarationFragment) {
                var vdf = (VariableDeclarationFragment) lp;
                lambdaParameters.put(vdf.getName().getIdentifier(), lp);
            } else {
                Log.warn("未知Lambda参数类型: " + lp.getClass().toString());
                lambdaParameters.put(lp.toString(), lp);
            }
        }
    }

    @Override
    public boolean visit(SimpleName node) {
        if (node.getIdentifier().equals("DbFunc")) {
            var newNode = generator.ast.newName(DbFunc.class.getName());
            generator.astRewrite.replace(node, newNode, null);
            return false;
        }

        return super.visit(node);
    }

    @Override
    public boolean visit(QualifiedName node) {
        //eg: e.name or e.customer.name
        //TODO:检查EntitySet成员报错 eg: e.orderItems
        var identifier = ServiceCodeGenerator.getTopIdentifier(node);
        if (lambdaParameters.containsKey(identifier)) {
            if (node.getQualifier().isQualifiedName()) {
                visit((QualifiedName) node.getQualifier()); //继续向上转换
            }

            var newQualifier = (ASTNode) generator.astRewrite.get(node, QualifiedName.QUALIFIER_PROPERTY);
            if (newQualifier.getParent() != null)
                newQualifier = ASTNode.copySubtree(generator.ast, newQualifier);

            var newNode = generator.makeEntityExpression((Expression) newQualifier,
                    node.getName().getIdentifier());
            generator.astRewrite.replace(node, newNode, null);
            return false;
        }

        return generator.visit(node);
    }

    @Override
    public boolean visit(ParenthesizedExpression node) {
        //TODO:转换为GroupExpression
        return super.visit(node);
    }

    @Override
    public boolean visit(InfixExpression node) {
        var infixRewrite = new InfixRewrite(lambdaParameters, generator);
        var result       = infixRewrite.visit(node);

        generator.astRewrite.replace(node, result.newNode, null);
        return false;
    }

    @Override
    public boolean visit(Assignment node) {
        var left = node.getLeftHandSide();
        if (left instanceof QualifiedName) {
            var qualified  = (QualifiedName) left;
            var identifier = ServiceCodeGenerator.getTopIdentifier(qualified);
            if (lambdaParameters.containsKey(identifier)) {
                if (qualified.getQualifier().isQualifiedName()) //eg: t.City.Name = "Wuxi"
                    throw new RuntimeException("EntityRef path not allowed");

                var exp     = generator.ast.newSimpleName(identifier);
                var newLeft = generator.makeEntityExpression(exp, qualified.getName().getIdentifier());

                node.getRightHandSide().accept(this);
                var newRight = (ASTNode) generator.astRewrite.get(node, Assignment.RIGHT_HAND_SIDE_PROPERTY);
                if (newRight.getParent() != null)
                    newRight = ASTNode.copySubtree(generator.ast, newRight);

                var newNode = generator.ast.newMethodInvocation();
                newNode.setName(generator.ast.newSimpleName("set"));
                newNode.setExpression(newLeft);
                newNode.arguments().add(newRight);

                generator.astRewrite.replace(node, newNode, null);
                return false;
            }
        }

        return super.visit(node);
    }

}
