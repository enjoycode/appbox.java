package appbox.design.services.code;

import appbox.logging.Log;
import appbox.model.EntityModel;
import appbox.model.entity.EntityMemberModel;
import appbox.store.DbFunc;
import org.eclipse.jdt.core.dom.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

//q.toList(j, (orderItem, product) -> new Object() {
//        String pname = product.name;
//        int quantity = orderItem.quantity;
//        });

//SqlUpdateCommand.output(e -> e.Amount) -> SqlUpdateCommand.output(e -> e.getString(0), (e) -> e.select(e.m("Name")))

final class SqlQueryMapperInterceptor implements IMethodInterceptor {

    private final Map<String, Object>    lambdaParameters = new HashMap<>();
    private final List<String>           selects          = new ArrayList<>();
    private final List<MethodInvocation> targets          = new ArrayList<>();

    @Override
    public boolean visit(MethodInvocation node, ServiceCodeGenerator generator) {
        var lambda = (LambdaExpression) node.arguments().get(node.arguments().size() - 1);
        if (!(lambda.getBody() instanceof ClassInstanceCreation || lambda.getBody() instanceof QualifiedName))
            throw new RuntimeException("Mapper must be a ClassInstanceCreation or QualifiedName");

        final String firstLambdaParaName = lambda.parameters().get(0).toString();
        var          selectLambda        = generator.ast.newLambdaExpression();

        for (var lp : lambda.parameters()) {
            if (lp instanceof VariableDeclarationFragment) {
                var vdf = (VariableDeclarationFragment) lp;
                lambdaParameters.put(vdf.getName().getIdentifier(), lp);

                var sp = generator.ast.newVariableDeclarationFragment();
                sp.setName(generator.ast.newSimpleName(vdf.getName().getIdentifier()));
                selectLambda.parameters().add(sp);
            } else {
                throw new RuntimeException("未实现");
                //Log.warn("未知Lambda参数类型: " + lp.getClass().toString());
                //lambdaParameters.put(lp.toString(), lp);
            }
        }

        var visitor = new ServiceCodeGeneratorProxy(generator) {
            private boolean inDbFunc = false;

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
                //注意需要排除相同成员的引用
                var identifier = ServiceCodeGenerator.getIdentifier(node);
                if (lambdaParameters.containsKey(identifier)) {
                    if (!inDbFunc) {
                        //final String CityName = t.City.Name
                        int pos = findSelectIndex(node.getFullyQualifiedName());
                        if (pos < 0) {
                            selects.add(node.getFullyQualifiedName());
                            pos = selects.size() - 1;
                            //添加选择的表达式 eg: t.m("Name")至targets列表内备用
                            targets.add(makeExpression(node, generator));
                        }
                        //根据类型转换目标 t.name -> t.getString(1)
                        var newNode = makeSqlReaderGet(
                                node.resolveTypeBinding(), firstLambdaParaName, pos, generator.ast);
                        generator.astRewrite.replace(node, newNode, null);
                    } else {
                        //final int Totals = DbFunc.sum(t.Quantity)
                        var newNode = makeExpression(node, generator);
                        generator.astRewrite.replace(node, newNode, null);
                    }
                    return false;
                }
                return generator.visit(node);
            }

            @Override
            public boolean visit(InfixExpression node) {
                if (inDbFunc) {
                    var infixRewrite = new InfixRewrite(lambdaParameters, generator);
                    var result       = infixRewrite.visit(node);
                    generator.astRewrite.replace(node, result.newNode, null);
                }
                return super.visit(node);
            }

            @Override
            public boolean visit(MethodInvocation node) {
                //DbFunc.XXX特殊处理
                if (node.getExpression() instanceof SimpleName
                        && ((SimpleName) node.getExpression()).getIdentifier().equals("DbFunc")) {
                    inDbFunc = true;

                    //暂不考虑相同的
                    selects.add(node.toString());
                    node.getExpression().accept(this);
                    for (var arg : node.arguments()) {
                        ((ASTNode) arg).accept(this);
                    }
                    targets.add((MethodInvocation) generator.astRewrite.createMoveTarget(node));

                    //根据DbFunc返回类型转换
                    var pos = selects.size() - 1;
                    var newNode = makeSqlReaderGet(
                            node.resolveTypeBinding(), firstLambdaParaName, pos, generator.ast);
                    generator.astRewrite.replace(node, newNode, null);

                    inDbFunc = false;
                    return false;
                }
                return super.visit(node);
            }
        };

        //如果是继承的扩展类型先select all
        if (lambda.getBody() instanceof ClassInstanceCreation) {
            var creationType     = ((ClassInstanceCreation) lambda.getBody()).getType();
            var creationTypeName = ((SimpleType) creationType).getName();
            var typeName = creationTypeName.isSimpleName() ?
                    ((SimpleName) creationTypeName).getIdentifier() :
                    ((QualifiedName) creationTypeName).getName().getIdentifier();
            if (!typeName.equals("Object")) {
                //TODO:验证类型必须与查询类型一致
                var entityType      = creationType.resolveBinding();
                var entityModelNode = generator.getUsedEntity(entityType);
                var entityModel     = (EntityModel) entityModelNode.model();
                for (var member : entityModel.getMembers()) {
                    if (member.type() != EntityMemberModel.EntityMemberType.DataField)
                        continue;
                    selects.add(firstLambdaParaName + "." + member.name());
                    var owner = generator.ast.newSimpleName(firstLambdaParaName);
                    targets.add(generator.makeEntityExpression(owner, member.name()));
                }
            }
        }

        //开始处理Lambda表达式
        lambda.accept(visitor);
        //最后添加选择表达式
        var selectMethod = generator.ast.newMethodInvocation();
        selectMethod.setName(generator.ast.newSimpleName("select"));
        selectMethod.setExpression(generator.ast.newSimpleName(firstLambdaParaName));
        for (var exp : targets) {
            selectMethod.arguments().add(exp);
        }
        selectLambda.setBody(selectMethod);
        var listRewrite = generator.astRewrite.getListRewrite(node, MethodInvocation.ARGUMENTS_PROPERTY);
        listRewrite.insertLast(selectLambda, null);

        //因为实例重用，清除旧数据
        lambdaParameters.clear();
        selects.clear();
        targets.clear();
        return false;
    }

    private int findSelectIndex(String fullName) {
        for (int i = 0; i < selects.size(); i++) {
            if (selects.get(i).equals(fullName))
                return i;
        }
        return -1;
    }

    /** t.City.Name -> q.m("City").m("Name") */
    private static MethodInvocation makeExpression(QualifiedName node, ServiceCodeGenerator generator) {
        if (node.getQualifier().isSimpleName()) {
            var ownerName = ((SimpleName) node.getQualifier()).getIdentifier();
            var owner     = generator.ast.newSimpleName(ownerName);
            return generator.makeEntityExpression(owner, node.getName().getIdentifier());
        }
        var owner = makeExpression((QualifiedName) node.getQualifier(), generator);
        return generator.makeEntityExpression(owner, node.getName().getIdentifier());
    }

    private static MethodInvocation makeSqlReaderGet(
            ITypeBinding type, String firstLambdaParaName, int pos, AST ast) {
        var getMethod = ast.newMethodInvocation();
        getMethod.setExpression(ast.newSimpleName(firstLambdaParaName));
        getMethod.arguments().add(ast.newNumberLiteral(Integer.toString(pos)));

        //TODO:*** others
        var typeName = type.getName();
        if (type.isPrimitive()) {
            switch (typeName) {
                case "boolean":
                    getMethod.setName(ast.newSimpleName("getBool")); break;
                case "int":
                    getMethod.setName(ast.newSimpleName("getInt")); break;
                case "long":
                    getMethod.setName(ast.newSimpleName("getLong")); break;
                case "float":
                    getMethod.setName(ast.newSimpleName("getFloat")); break;
                case "double":
                    getMethod.setName(ast.newSimpleName("getDouble")); break;
                default:
                    throw new RuntimeException("未实现:" + typeName);
            }
        } else if (type.isClass()) {
            switch (typeName) {
                case "String":
                    getMethod.setName(ast.newSimpleName("getString")); break;
                case "Integer":
                    getMethod.setName(ast.newSimpleName("getInt")); break;
                case "Long":
                    getMethod.setName(ast.newSimpleName("getLong")); break;
                default:
                    throw new RuntimeException("未实现:" + typeName);
            }
        } else {
            throw new RuntimeException("未实现:" + typeName);
        }

        return getMethod;
    }

}
