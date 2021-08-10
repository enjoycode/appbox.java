package appbox.design.lang.java.code;

import appbox.store.DbFunc;
import org.eclipse.jdt.core.dom.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** 用于SqlQueryLambdaVisitor改写InfixExpression,注意:运算符优先级AST会处理 */
final class InfixRewrite { //TODO:处理行差
    static class Result {
        public final ASTNode newNode;
        public final boolean rewrited;

        public Result(ASTNode newNode, boolean rewrited) {
            this.newNode  = newNode;
            this.rewrited = rewrited;
        }
    }

    private final Map<String, Object>  lambdaParameters;
    private final AST                  ast;
    private final ServiceCodeGenerator generator;

    public InfixRewrite(Map<String, Object> lambdaParameters, ServiceCodeGenerator generator) {
        this.lambdaParameters = lambdaParameters;
        this.ast              = generator.ast;
        this.generator        = generator;
    }

    private Result visit(ASTNode node) {
        switch (node.getNodeType()) {
            case ASTNode.SIMPLE_NAME:
                return visit((SimpleName) node);
            case ASTNode.QUALIFIED_NAME:
                return visit((QualifiedName) node);
            case ASTNode.INFIX_EXPRESSION:
                return visit((InfixExpression) node);
            case ASTNode.METHOD_INVOCATION:
                return visit((MethodInvocation) node);
            case ASTNode.STRING_LITERAL:
            case ASTNode.NULL_LITERAL:
            case ASTNode.NUMBER_LITERAL:
            case ASTNode.BOOLEAN_LITERAL:
            case ASTNode.CHARACTER_LITERAL:
                return new Result(ASTNode.copySubtree(ast, node), false);
            default:
                throw new RuntimeException();
        }
    }

    private Result visit(SimpleName node) {
        boolean isDbFunc    = node.getIdentifier().equals("DbFunc");
        boolean needRewrite = lambdaParameters.containsKey(node.getIdentifier()) || isDbFunc;
        var newNode = isDbFunc ? ast.newName(DbFunc.class.getName()) :
                ast.newSimpleName(node.getIdentifier());
        return new Result(newNode, needRewrite);
    }

    private Result visit(QualifiedName node) {
        var newQualifier = visit(node.getQualifier());

        if (newQualifier.rewrited) {
            var newNode = generator.makeEntityExpression((Expression) newQualifier.newNode,
                    node.getName().getIdentifier());
            return new Result(newNode, true);
        } else {
            if (TypeHelper.isEntityType(node.getQualifier().resolveTypeBinding())) {
                var newNode = ast.newMethodInvocation();
                newNode.setName(ast.newSimpleName("get" + node.getName().getIdentifier()));
                newNode.setExpression((Expression) newQualifier.newNode);
                return new Result(newNode, false);
            } else {
                var newNode = ast.newQualifiedName((Name) newQualifier.newNode,
                        ast.newSimpleName(node.getName().getIdentifier()));
                return new Result(newNode, false);
            }
        }
    }

    private Result visit(MethodInvocation node) {
        //如果有拦截器还是需要先处理, eg: DbFunc.in(t.Name, sq.toSubQuery(s -> s.Name))
        var methodInterceptor = TypeHelper.getMethodInterceptor(node.resolveMethodBinding());
        if (methodInterceptor != null) {
            var res = generator.methodInterceptors.get(methodInterceptor).visit(node, generator);
            node.getExpression().accept(generator);
            return new Result(generator.astRewrite.createCopyTarget(node), false);
        }

        var expression = visit(node.getExpression());
        var arguments  = new ArrayList<Result>(node.arguments().size());
        for (var arg : node.arguments()) {
            arguments.add(visit((ASTNode) arg));
        }
        var newNode = ast.newMethodInvocation();
        newNode.setExpression((Expression) expression.newNode);
        newNode.setName(ast.newSimpleName(node.getName().getIdentifier()));
        for (var arg : arguments) {
            newNode.arguments().add(arg.newNode);
        }
        return new Result(newNode, expression.rewrited);
    }

    public Result visit(InfixExpression node) {
        boolean rewrited = false;
        var     left     = visit(node.getLeftOperand());
        var     right    = visit(node.getRightOperand());
        rewrited = left.rewrited || right.rewrited;
        List<Result> extended = null;
        if (node.hasExtendedOperands()) {
            extended = new ArrayList<>(node.extendedOperands().size());
            for (var ext : node.extendedOperands()) {
                var r = visit((ASTNode) ext);
                extended.add(r);
                rewrited |= r.rewrited;
            }
        }

        if (rewrited) {
            //TODO:判断左边非表达式，需要转换为PrimitiveExpression
            var newNode = ast.newMethodInvocation();
            newNode.setName(ast.newSimpleName(getOperator(node.getOperator())));
            newNode.setExpression((Expression) left.newNode);
            newNode.arguments().add(right.newNode);

            if (extended != null) {
                for (var ext : extended) {
                    var temp = ast.newMethodInvocation();
                    temp.setName(ast.newSimpleName(getOperator(node.getOperator())));
                    temp.setExpression(newNode);
                    temp.arguments().add(ext.newNode);
                    newNode = temp;
                }
            }

            return new Result(newNode, true);
        } else {
            var newNode = ast.newInfixExpression();
            newNode.setLeftOperand((Expression) left.newNode);
            newNode.setRightOperand((Expression) right.newNode);
            newNode.setOperator(InfixExpression.Operator.toOperator(node.getOperator().toString()));
            if (extended != null) {
                for (var ext : extended) {
                    newNode.extendedOperands().add(ext.newNode);
                }
            }

            return new Result(newNode, false);
        }
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
        } else if (op == InfixExpression.Operator.TIMES) {
            return "times";
        } else if (op == InfixExpression.Operator.DIVIDE) {
            return "div";
        } else {
            throw new RuntimeException("未实现: " + op);
        }
    }

}
