package appbox.design.services.code;

import appbox.store.query.SqlQuery;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;

final class SqlQueryCtorInterceptor implements ICtorInterceptor {

    //Convert "new SqlQuery<Employee>()" -> "new appbox.store.query.SqlQuery<>(modelId, Emploee.class)"

    @Override
    public boolean visit(ClassInstanceCreation node, ServiceCodeGenerator generator) {
        final var ast = generator.ast;

        var queryType = ast.newSimpleType(ast.newName(SqlQuery.class.getName()));
        var type = ast.newParameterizedType(queryType);

        var newNode = ast.newClassInstanceCreation();
        newNode.setType(type);

        var entityType = node.resolveTypeBinding().getTypeArguments()[0];
        var entityModelNode = generator.getUsedEntity(entityType);
        //参数1 modelId
        var para1 = ast.newNumberLiteral(entityModelNode.model().id() + "L");
        newNode.arguments().add(para1);
        //参数2 Entity.class
        var entityClassName   = EntityCodeGenerator.makeEntityClassName(entityModelNode);
        var entityRuntimeType = ast.newSimpleType(ast.newName(entityClassName));
        var para2 = ast.newTypeLiteral();
        para2.setType(entityRuntimeType);
        newNode.arguments().add(para2);

        //改写
        generator.astRewrite.replace(node, newNode, null);

        return false;
    }

}
