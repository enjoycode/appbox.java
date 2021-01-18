package appbox.design.services.code;

import appbox.store.query.SqlQuery;
import appbox.store.query.TableScan;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;

final class SqlQueryCtorInterceptor implements ICtorInterceptor {

    //Convert "new SqlQuery<Employee>()" -> "new appbox.store.query.SqlQuery<>(modelId, SYS_Emploee.class)"
    //Convert "new SqlUpdateCommand<Employee>()" -> "new appbox.store.query.SqlUpdateCommand(modelId)"

    @Override
    public boolean visit(ClassInstanceCreation node, ServiceCodeGenerator generator) {
        final var ast         = generator.ast;
        var       runtimeType = TypeHelper.getRuntimeType(node.resolveTypeBinding());
        var isSqlQuery = runtimeType.equals(SqlQuery.class.getName())
                || runtimeType.equals(TableScan.class.getName());
        var entityType      = node.resolveTypeBinding().getTypeArguments()[0];
        var entityModelNode = generator.getUsedEntity(entityType);

        var newNode   = ast.newClassInstanceCreation();
        var queryType = ast.newSimpleType(ast.newName(runtimeType));
        if (isSqlQuery) {
            newNode.setType(ast.newParameterizedType(queryType));
        } else {
            newNode.setType(queryType);
        }

        //参数1 modelId
        var para1 = ast.newNumberLiteral(entityModelNode.model().id() + "L");
        newNode.arguments().add(para1);
        //参数2 Entity.class
        if (isSqlQuery) {
            var entityClassName   = EntityCodeGenerator.makeEntityClassName(entityModelNode);
            var entityRuntimeType = ast.newSimpleType(ast.newName(entityClassName));
            var para2             = ast.newTypeLiteral();
            para2.setType(entityRuntimeType);
            newNode.arguments().add(para2);
        }

        //改写
        generator.astRewrite.replace(node, newNode, null);

        return false;
    }

}
