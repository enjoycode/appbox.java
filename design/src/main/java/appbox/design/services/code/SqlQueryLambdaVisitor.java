package appbox.design.services.code;

import appbox.logging.Log;
import appbox.utils.StringUtil;
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
    public boolean visit(QualifiedName node) {
        //eg: e.name or e.customer.name
        //TODO:检查EntitySet成员报错 eg: e.orderItems
        var identifier = ServiceCodeGenerator.getIdentifier(node);
        if (lambdaParameters.containsKey(identifier)
            /*&& TypeHelper.isEntityType(node.getQualifier().resolveTypeBinding())*/) {
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
        } else {
            return generator.visit(node);
        }
    }

    @Override
    public boolean visit(ParenthesizedExpression node) {
        //TODO:转换为GroupExpression
        return super.visit(node);
    }

    @Override
    public boolean visit(InfixExpression node) {
        node.getLeftOperand().accept(this);
        node.getRightOperand().accept(this);

        //TODO:判断左边非表达式，需要转换为PrimitiveExpression
        var newLeft  = (ASTNode) generator.astRewrite.get(node, InfixExpression.LEFT_OPERAND_PROPERTY);
        var newRight = (ASTNode) generator.astRewrite.get(node, InfixExpression.RIGHT_OPERAND_PROPERTY);
        if (newLeft.getParent() != null) //newLeft.getStartPosition() != -1
            newLeft = ASTNode.copySubtree(generator.ast, newLeft);
        if (newRight.getParent() != null)
            newRight = ASTNode.copySubtree(generator.ast, newRight);

        var opMethod = getOperator(node.getOperator());
        var newNode  = generator.ast.newMethodInvocation();
        newNode.setName(generator.ast.newSimpleName(opMethod));
        newNode.setExpression((Expression) newLeft);
        newNode.arguments().add(newRight);

        generator.astRewrite.replace(node, newNode, null);
        return false;
    }

    private static String getOperator(InfixExpression.Operator op) {
        if (op == InfixExpression.Operator.EQUALS) {
            return "eq";
        } else if (op == InfixExpression.Operator.NOT_EQUALS) {
            return "nq";
        } else if (op == InfixExpression.Operator.CONDITIONAL_AND) {
            return "and";
        } else if (op == InfixExpression.Operator.CONDITIONAL_OR) {
            return "or";
        } else if (op == InfixExpression.Operator.LESS) {
            return "lt";
        } else if (op == InfixExpression.Operator.LESS_EQUALS) {
            return "le";
        } else if (op == InfixExpression.Operator.GREATER_EQUALS) {
            return "ge";
        } else if (op == InfixExpression.Operator.GREATER) {
            return "gt";
        } else if (op == InfixExpression.Operator.PLUS) {
            return "plus";
        } else if (op == InfixExpression.Operator.MINUS) {
            return "minus";
        } else {
            throw new RuntimeException("未实现: " + op);
        }
    }

}
