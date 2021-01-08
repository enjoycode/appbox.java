package appbox.design.services.code;

import appbox.logging.Log;
import appbox.store.DbFunc;
import org.eclipse.jdt.core.dom.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class SqlQueryLambdaVisitor extends ServiceCodeGeneratorProxy {

    private final MethodInvocation method;
    private final LambdaExpression lambda;

    private final Map<String, Object>    lambdaParameters = new HashMap<>();
    private final List<MethodInvocation> infixStack       = new ArrayList<>();

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
        var identifier = ServiceCodeGenerator.getIdentifier(node);
        if (lambdaParameters.containsKey(identifier)) {
            if (node.getQualifier().isQualifiedName()) {
                visit((QualifiedName) node.getQualifier()); //继续向上转换
            }

            var newQualifier = (ASTNode) generator.astRewrite.get(node, QualifiedName.QUALIFIER_PROPERTY);
            if (newQualifier.getParent() != null)
                newQualifier = ASTNode.copySubtree(generator.ast, newQualifier);

            var newNode = generator.makeEntityExpression((Expression) newQualifier,
                    node.getName().getIdentifier());
            //if (node.getParent() instanceof InfixExpression) {
            //    var parent = infixStack.get(infixStack.size() - 1);
            //    if (node.getLocationInParent().getId().equals("leftOperand"))
            //        generator.astRewrite.replace(parent.getExpression(), newNode, null);
            //    else
            //        generator.astRewrite.replace((ASTNode) parent.arguments().get(0), newNode, null);
            //} else {
                generator.astRewrite.replace(node, newNode, null);
            //}

            return false;
        }

        //if (TypeHelper.isEntityType(node.getQualifier().resolveTypeBinding())) {
        //    var newNode = generator.makeEntityGetMember(node);
        //    if (node.getParent() instanceof InfixExpression) {
        //        var parent = infixStack.get(infixStack.size() - 1);
        //        if (node.getLocationInParent().getId().equals("leftOperand"))
        //            generator.astRewrite.replace(parent.getExpression(), newNode, null);
        //        else
        //            generator.astRewrite.replace((ASTNode) parent.arguments().get(0), newNode, null);
        //    } else {
        //        generator.astRewrite.replace(node, newNode, null);
        //    }
        //    node.getQualifier().accept(this);
        //    return false;
        //}

        return generator.visit(node);
    }

    @Override
    public boolean visit(ParenthesizedExpression node) {
        //TODO:转换为GroupExpression
        return super.visit(node);
    }

    @Override
    public boolean visit(InfixExpression node) {
        return transformInfixExpression(node, this, generator, infixStack);
    }

    //@Override
    //public boolean visit(MethodInvocation node) {
    //    if (node.getExpression() instanceof SimpleName
    //            && ((SimpleName) node.getExpression()).getIdentifier().equals("DbFunc")) {
    //        return transformDbFunc(node, this, generator);
    //    }
    //
    //    return super.visit(node);
    //}

    @Override
    public boolean visit(Assignment node) {
        var left = node.getLeftHandSide();
        if (left instanceof QualifiedName) {
            var qualified  = (QualifiedName) left;
            var identifier = ServiceCodeGenerator.getIdentifier(qualified);
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

    //protected static boolean transformDbFunc(MethodInvocation node
    //        , ASTVisitor visitor, ServiceCodeGenerator generator) {
    //
    //    var newNode = generator.ast.newMethodInvocation();
    //    newNode.setName(generator.ast.newSimpleName(node.getName().getIdentifier()));
    //    newNode.setExpression(generator.ast.newName(DbFunc.class.getName()));
    //
    //    for (var arg : node.arguments()) {
    //        var argNode = (ASTNode) arg;
    //        argNode.accept(visitor);
    //    }
    //
    //    var listRewrite = generator.astRewrite.getListRewrite(node, MethodInvocation.ARGUMENTS_PROPERTY);
    //    for (var arg : listRewrite.getRewrittenList()) {
    //        var argNode = (ASTNode) arg;
    //        if (argNode.getParent() != null) {
    //            //argNode = ASTNode.copySubtree(generator.ast, argNode);
    //            argNode = generator.astRewrite.createMoveTarget(argNode);
    //            generator.astRewrite.replace(argNode, null, null);
    //        }
    //
    //        newNode.arguments().add(argNode);
    //    }
    //
    //    generator.astRewrite.replace(node, newNode, null);
    //    return false;
    //}

    // t.Name == "Rick" to t.m("Name").eq("Rick")
    protected static boolean transformInfixExpression(InfixExpression node
            , SqlQueryLambdaVisitor visitor, ServiceCodeGenerator generator, List<MethodInvocation> infixStack) {

        if (!visitor.needTransformInfix(node))
            return true;

        //node.getLeftOperand().accept(visitor);
        //node.getRightOperand().accept(visitor);

        //TODO:处理运算符优先级
        //TODO:判断左边非表达式，需要转换为PrimitiveExpression
        ASTNode newLeft;
        //if (node.getLeftOperand() instanceof InfixExpression
        //        && visitor.needTransformInfix((InfixExpression) node.getLeftOperand()))
        //    newLeft = generator.astRewrite.createStringPlaceholder("l.eq(l)", ASTNode.METHOD_INVOCATION);
        //    //newLeft = generator.ast.newMethodInvocation();
        //else
            newLeft = generator.astRewrite.createCopyTarget(node.getLeftOperand());
        ASTNode newRight;
        //if (node.getRightOperand() instanceof InfixExpression
        //        && visitor.needTransformInfix((InfixExpression) node.getRightOperand()))
        //    newRight = generator.astRewrite.createStringPlaceholder("r.eq(r)", ASTNode.METHOD_INVOCATION);
        //    //newRight = generator.ast.newMethodInvocation();
        //else
            newRight = generator.astRewrite.createCopyTarget(node.getRightOperand());

        var opMethod = getOperator(node.getOperator());
        var newNode  = generator.ast.newMethodInvocation();
        newNode.setName(generator.ast.newSimpleName(opMethod));
        newNode.setExpression((Expression) newLeft);
        newNode.arguments().add(newRight);

        generator.astRewrite.replace(node, newNode, null);
        //if (infixStack.size() > 0) {
        //    var parent = infixStack.get(infixStack.size() - 1);
        //    if (node.getLocationInParent().getId().equals("leftOperand"))
        //        generator.astRewrite.replace(parent.getExpression(), newNode, null);
        //    else
        //        generator.astRewrite.replace((ASTNode) parent.arguments().get(0), newNode, null);
        //} else {
        //    generator.astRewrite.replace(node, newNode, null);
        //}

        //infixStack.add(newNode);
        node.getLeftOperand().accept(visitor);
        node.getRightOperand().accept(visitor);
        //infixStack.remove(infixStack.size() - 1);
        return false;
    }

    /** 用于判断是否需要转换 */
    private boolean needTransformInfix(InfixExpression node) {
        return isExpression(node.getLeftOperand()) || isExpression(node.getRightOperand());
    }

    private boolean isExpression(ASTNode node) {
        if (node instanceof InfixExpression)
            return needTransformInfix((InfixExpression) node);

        if (isDbFunc(node))
            return true;

        if (node instanceof QualifiedName) {
            var identifier = ServiceCodeGenerator.getIdentifier((QualifiedName) node);
            if (lambdaParameters.containsKey(identifier))
                return true;
        }

        return false;
    }

    private static boolean isDbFunc(ASTNode exp) {
        if (exp instanceof MethodInvocation) {
            var methodInvocation = (MethodInvocation) exp;
            return methodInvocation.getExpression() instanceof SimpleName
                    && ((SimpleName) methodInvocation.getExpression()).getIdentifier().equals("DbFunc");
        }

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
