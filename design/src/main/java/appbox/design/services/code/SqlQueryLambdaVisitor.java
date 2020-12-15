package appbox.design.services.code;

import appbox.logging.Log;
import appbox.utils.StringUtil;
import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.internal.corext.dom.GenericVisitor;

import java.util.HashMap;
import java.util.Map;

final class SqlQueryLambdaVisitor extends GenericVisitor {

    private final MethodInvocation     method;
    private final LambdaExpression     lambda;
    private final ServiceCodeGenerator generator;
    private       ASTNode              current = null;

    private final Map<String, Object> lambdaParameters = new HashMap<>();

    public SqlQueryLambdaVisitor(MethodInvocation method, LambdaExpression lambda, ServiceCodeGenerator generator) {
        this.method    = method;
        this.lambda    = lambda;
        this.generator = generator;

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
        var identifier = getIdentifier(node);
        if (lambdaParameters.containsKey(identifier)
            /*&& TypeHelper.isEntityType(node.getQualifier().resolveTypeBinding())*/) {
            if (node.getQualifier().isQualifiedName()) {
                visit((QualifiedName) node.getQualifier()); //继续向上转换
            }
            var newNode = generator.ast.newMethodInvocation();
            newNode.setName(generator.ast.newSimpleName("m"));
            newNode.setExpression((Expression) ASTNode.copySubtree(generator.ast, node.getQualifier()));
            var memberName = StringUtil.firstUpperCase(node.getName().getIdentifier()); //TODO:暂强制转换
            var member     = generator.ast.newStringLiteral();
            member.setLiteralValue(memberName);
            newNode.arguments().add(member);
            generator.astRewrite.replace(node, newNode, null);
            current = newNode;
            return false;
        } else {
            return generator.visit(node);
        }
    }

    @Override
    public boolean visit(InfixExpression node) {
        node.getLeftOperand().accept(this);
        //TODO:根据current==null判断左边非表达式，需要转换为PrimitiveExpression
        var left = current == null ? node.getLeftOperand() : current;
        current = null;
        node.getRightOperand().accept(this);
        var right = current == null ? node.getRightOperand() : current;
        current = null;
        //根据类型进行转换 e.name == "Rick" -> e.m("Name").eq("Rick")
        var opMethod = getOperator(node.getOperator());
        var newNode  = generator.ast.newMethodInvocation();
        newNode.setName(generator.ast.newSimpleName(opMethod));
        newNode.setExpression((Expression) ASTNode.copySubtree(generator.ast, left));
        newNode.arguments().add(ASTNode.copySubtree(generator.ast, right));
        generator.astRewrite.replace(node, newNode, null);

        current = newNode;
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
        } else {
            throw new RuntimeException("未实现: " + op);
        }
    }

    private static String getIdentifier(Name node) {
        if (node.isSimpleName()) {
            return ((SimpleName) node).getIdentifier();
        } else {
            return getIdentifier(((QualifiedName) node).getQualifier());
        }
    }
}
