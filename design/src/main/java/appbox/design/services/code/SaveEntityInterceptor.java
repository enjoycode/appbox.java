package appbox.design.services.code;

import appbox.model.EntityModel;
import appbox.store.EntityStore;
import appbox.store.SqlStore;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.MethodInvocation;

/** 转换DbEntity.insertAsync()等 */
public final class SaveEntityInterceptor implements IMethodInterceptor {

    @Override
    public boolean visit(MethodInvocation node, ServiceCodeGenerator generator) {
        //TODO:暂只支持SqlStore及SysStore
        var     entityType      = node.getExpression().resolveTypeBinding();
        var     entityModelNode = generator.getUsedEntity(entityType);
        var     entityModel     = (EntityModel) entityModelNode.model();
        boolean isSqlStore      = entityModel.sqlStoreOptions() != null;

        //原表达式作为参数1
        node.getExpression().accept(generator);
        var arg1 = (ASTNode) generator.astRewrite.get(node, MethodInvocation.EXPRESSION_PROPERTY);
        if (arg1.getParent() != null)
            arg1 = ASTNode.copySubtree(generator.ast, arg1);

        //原参数1作为参数2
        ASTNode arg2 = null;
        if (node.arguments().size() > 0) { //只可能1个参数
            //需要转换参数后再重新加入
            ((ASTNode) node.arguments().get(0)).accept(generator);

            var listRewrite = generator.astRewrite.getListRewrite(node, MethodInvocation.ARGUMENTS_PROPERTY);
            arg2 = (ASTNode) listRewrite.getRewrittenList().get(0);
            if (arg2.getParent() != null)
                arg2 = ASTNode.copySubtree(generator.ast, arg2);
        } else if (isSqlStore) {
            arg2 = generator.ast.newNullLiteral();
        }

        //替换为DataStore的方法
        Expression expression = null;
        if (isSqlStore) {
            var storeId       = entityModel.sqlStoreOptions().storeModelId();
            var storeTypeName = SqlStore.class.getName();

            var getStoreMethod = generator.ast.newMethodInvocation();
            getStoreMethod.setName(generator.ast.newSimpleName("get"));
            getStoreMethod.setExpression(generator.ast.newName(storeTypeName));
            getStoreMethod.arguments().add(generator.ast.newNumberLiteral(storeId + "L"));

            expression = getStoreMethod;
        } else {
            expression = generator.ast.newName(EntityStore.class.getName());
        }

        var saveMethod = generator.ast.newMethodInvocation();
        saveMethod.setName(generator.ast.newSimpleName(node.getName().getIdentifier()));
        saveMethod.setExpression(expression);
        saveMethod.arguments().add(arg1);
        if (arg2 != null)
            saveMethod.arguments().add(arg2);

        generator.astRewrite.replace(node, saveMethod, null);

        return false;
    }

}
