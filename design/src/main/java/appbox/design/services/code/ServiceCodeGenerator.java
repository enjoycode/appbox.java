package appbox.design.services.code;

import appbox.data.EntityId;
import appbox.design.DesignHub;
import appbox.design.tree.ModelNode;
import appbox.logging.Log;
import appbox.model.DataStoreModel;
import appbox.model.EntityModel;
import appbox.model.ModelType;
import appbox.model.ServiceModel;
import appbox.model.entity.EntityMemberModel;
import appbox.model.entity.EntityRefModel;
import appbox.model.entity.EntitySetModel;
import appbox.runtime.InvokeArgs;
import appbox.runtime.RuntimeContext;
import appbox.store.SqlStore;
import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.internal.corext.dom.GenericVisitor;

import java.util.*;

/** 用于生成运行时的服务代码 */
@SuppressWarnings("unchecked")
public final class ServiceCodeGenerator extends GenericVisitor {

    //region ====拦截器====
    private static final Map<String, ICtorInterceptor> ctorInterceptors = new HashMap<>() {{
        put("SqlQuery", new SqlQueryCtorInterceptor());
    }};

    protected static final Map<String, IMethodInterceptor> methodInterceptors = new HashMap<>() {{
        put("AsyncAwait", new AsyncAwaitInterceptor());
        put("SqlQueryWhere", new SqlQueryWhereInterceptor());
        put("SqlQueryMapper", new SqlQueryMapperInterceptor());
        put("SqlQuerySelect", new SqlQuerySelectInterceptor());
        put("SqlUpdateSet", new SqlUpdateSetInterceptor());
        put("InvokeService", new InvokeServiceInterceptor());
        put("SaveEntity", new SaveEntityInterceptor());
        put("InvokeStatic", new InvokeStaticInterceptor());
    }};
    //endregion

    private       TypeDeclaration         _serviceTypeDeclaration;
    /** 公开的服务方法集合 */
    private final List<MethodDeclaration> publicMethods = new ArrayList<>();
    private final Map<String, ModelNode>  usedEntities  = new HashMap<>();

    protected final DesignHub    hub;
    protected final String       appName;
    protected final ServiceModel serviceModel;
    protected final ASTRewrite   astRewrite;
    protected final AST          ast;
    public          boolean      hasAwaitInvocation;

    public ServiceCodeGenerator(DesignHub hub, String appName,
                                ServiceModel serviceModel, ASTRewrite astRewrite) {
        this.hub          = hub;
        this.appName      = appName;
        this.serviceModel = serviceModel;
        this.astRewrite   = astRewrite;
        this.ast          = astRewrite.getAST();
    }

    //region ====visit methods====
    @Override
    public boolean visit(ImportDeclaration node) {
        if (node.getName().isQualifiedName()) {
            var identifier = getIdentifier(node.getName());
            if (hub.designTree.findApplicationNodeByName(identifier) != null) {
                var newNode = ast.newLineComment();
                astRewrite.replace(node, newNode, null);
                return false;
            }
        }

        return super.visit(node);
    }

    @Override
    public boolean visit(TypeDeclaration node) {
        if (TypeHelper.isServiceClass(node, appName, serviceModel.name())) {
            _serviceTypeDeclaration = node;

            var serviceType = ast.newSimpleType(ast.newName("appbox.runtime.IService"));
            //astRewrite.set(node, TypeDeclaration.SUPERCLASS_TYPE_PROPERTY, serviceType, null );
            var listRewrite = astRewrite.getListRewrite(node, TypeDeclaration.SUPER_INTERFACE_TYPES_PROPERTY);
            listRewrite.insertFirst(serviceType, null);
        }

        return true;
    }

    @Override
    public boolean visit(SimpleType node) {
        var entityType = TypeHelper.getEntityType(node);
        if (entityType != null) {
            //转换为运行时类型
            if (!node.isVar()) {
                var entityRuntimeType = makeEntityRuntimeType(entityType);
                astRewrite.replace(node, entityRuntimeType, null);
            }
            return false;
        }

        return super.visit(node);
    }

    @Override
    public boolean visit(MethodDeclaration node) {
        //判断方法是否服务方法
        if (TypeHelper.isServiceClass((TypeDeclaration) node.getParent(), appName, serviceModel.name())
                && TypeHelper.isServiceMethod(node)) {
            addAsServiceMethod(node);
        }

        return super.visit(node);
    }

    @Override
    public boolean visit(Assignment node) {
        if (node.getLeftHandSide() instanceof QualifiedName) {
            var qfn       = (QualifiedName) node.getLeftHandSide();
            var owner     = qfn.getQualifier();
            var ownerType = owner.resolveTypeBinding();
            if (TypeHelper.isEntityType(ownerType)) {
                var newNode = ast.newMethodInvocation();
                newNode.setName(ast.newSimpleName("set" + qfn.getName().getIdentifier()));

                astRewrite.replace(node, newNode, null);
                owner.accept(this);
                node.getRightHandSide().accept(this);

                var newOwner = (ASTNode) astRewrite.get(qfn, QualifiedName.QUALIFIER_PROPERTY);
                if (newOwner.getParent() == null)
                    newNode.setExpression((Expression) newOwner);
                else
                    newNode.setExpression((Expression) astRewrite.createCopyTarget(newOwner));

                var newArg = (ASTNode) astRewrite.get(node, Assignment.RIGHT_HAND_SIDE_PROPERTY);
                if (newArg.getParent() == null)
                    newNode.arguments().add(newArg);
                else
                    newNode.arguments().add(astRewrite.createCopyTarget(newArg));

                return false;
            }
        } else if (node.getLeftHandSide() instanceof FieldAccess) {
            var fa        = (FieldAccess) node.getLeftHandSide();
            var owner     = fa.getExpression();
            var ownerType = owner.resolveTypeBinding();
            if (TypeHelper.isEntityType(ownerType)) {
                var newNode = ast.newMethodInvocation();
                newNode.setName(ast.newSimpleName("set" + fa.getName().getIdentifier()));

                astRewrite.replace(node, newNode, null);
                owner.accept(this);
                node.getRightHandSide().accept(this);

                var newOwner = (ASTNode) astRewrite.get(fa, FieldAccess.EXPRESSION_PROPERTY);
                if (newOwner.getParent() == null)
                    newNode.setExpression((Expression) newOwner);
                else
                    newNode.setExpression((Expression) astRewrite.createCopyTarget(newOwner));

                var newArg = (ASTNode) astRewrite.get(node, Assignment.RIGHT_HAND_SIDE_PROPERTY);
                if (newArg.getParent() == null)
                    newNode.arguments().add(newArg);
                else
                    newNode.arguments().add(astRewrite.createCopyTarget(newArg));

                return false;
            }
        }
        return super.visit(node);
    }

    @Override
    public boolean visit(QualifiedName node) {
        var owner     = node.getQualifier();
        var ownerType = owner.resolveTypeBinding();
        if (TypeHelper.isEntityType(ownerType)) {
            //TODO:判断是否实体属性
            var newNode = makeEntityGetMember(owner, node.getName().getIdentifier());
            astRewrite.replace(node, newNode, null);

            owner.accept(this);

            var newExp = (ASTNode) astRewrite.get(node, QualifiedName.QUALIFIER_PROPERTY);
            if (newExp.getParent() == null)
                newNode.setExpression((Expression) newExp);

            return false;
        } else if (TypeHelper.isDataStoreType(ownerType) && owner.isSimpleName()) {
            String storeName     = node.getName().getIdentifier();
            String storeTypeName = null;
            var    storeNode     = hub.designTree.findDataStoreNodeByName(storeName);
            if (storeNode.model().kind() == DataStoreModel.DataStoreKind.Sql) {
                storeTypeName = SqlStore.class.getName();
                var newNode = ast.newMethodInvocation();
                newNode.setName(ast.newSimpleName("get"));
                newNode.setExpression(ast.newName(storeTypeName));
                newNode.arguments().add(ast.newNumberLiteral(storeNode.model().id() + "L"));
                astRewrite.replace(node, newNode, null);

                return false;
            } else {
                //TODO:
                Log.warn("转换其他存储的方法暂未实现");
            }
        } else {
            var appNode = TypeHelper.isPermissionType(ownerType, hub.designTree);
            if (appNode != null) {
                var permissionName = node.getName().getIdentifier();
                var permissionNode = hub.designTree.findModelNodeByName(
                        appNode.model.id(), ModelType.Permission, permissionName);
                
                var newNode = ast.newMethodInvocation();
                newNode.setName(ast.newSimpleName("hasPermission"));
                newNode.setExpression(ast.newName(RuntimeContext.class.getName()));
                newNode.arguments().add(ast.newNumberLiteral(permissionNode.model().id() + "L"));
                astRewrite.replace(node, newNode, null);
                return false;
            }
        }

        return super.visit(node);
    }

    @Override
    public boolean visit(FieldAccess node) {
        var ownerType = node.getExpression().resolveTypeBinding();
        if (TypeHelper.isEntityType(ownerType)) {
            //TODO:判断是否实体属性
            var newNode = makeEntityGetMember(node.getExpression(), node.getName().getIdentifier());
            astRewrite.replace(node, newNode, null);

            node.getExpression().accept(this);

            var newExp = (ASTNode) astRewrite.get(node, FieldAccess.EXPRESSION_PROPERTY);
            if (newExp.getParent() == null)
                newNode.setExpression((Expression) newExp);

            return false;
        }

        return super.visit(node);
    }

    @Override
    public boolean visit(ClassInstanceCreation node) {
        //判断有无构造拦截器
        var ctorInterceptor = TypeHelper.getCtorInterceptor(node.resolveTypeBinding());
        if (ctorInterceptor != null) {
            return ctorInterceptors.get(ctorInterceptor).visit(node, this);
        }

        return super.visit(node);
    }

    @Override
    public boolean visit(MethodInvocation node) {
        var methodInterceptor = TypeHelper.getMethodInterceptor(node.resolveMethodBinding());
        if (methodInterceptor != null) {
            var res = methodInterceptors.get(methodInterceptor).visit(node, this);
            //注意类似q.groupBy().having()的调用
            if (!res)
                node.getExpression().accept(this);
            return res;
        }

        return super.visit(node);
    }

    //endregion

    /** 根据实体名称(eg: sys.entities.Employee)获取对应的ModelNode，如果不存在加入使用列表 */
    protected ModelNode getUsedEntity(ITypeBinding entityType) {
        var pkg     = entityType.getPackage().getJavaElement();
        var appName = pkg.getPath().segment(1);
        return getUsedEntity(appName, entityType.getName());
    }

    protected ModelNode getUsedEntity(long modelId) {
        var entityModelNode = hub.designTree.findModelNode(ModelType.Entity, modelId);
        return getUsedEntity(entityModelNode.appNode.model.name(), entityModelNode.model().name());
    }

    private ModelNode getUsedEntity(String appName, String entityName) {
        var fullName        = String.format("%s.entities.%s", appName, entityName);
        var entityModelNode = usedEntities.get(fullName);
        if (entityModelNode == null) {
            var appNode = hub.designTree.findApplicationNodeByName(appName);
            entityModelNode = hub.designTree.findModelNodeByName(
                    appNode.model.id(), ModelType.Entity, entityName);
            usedEntities.put(fullName, entityModelNode);

            //加入当前实体导航属性使用到的相关实体
            var entityModel = (EntityModel) entityModelNode.model();
            for (var member : entityModel.getMembers()) {
                if (member.type() == EntityMemberModel.EntityMemberType.EntityRef) {
                    var entityRef = (EntityRefModel) member;
                    if (!entityRef.isAggregationRef()) { //聚合引用不用处理
                        var refModelNode = hub.designTree.findModelNode(ModelType.Entity
                                , entityRef.getRefModelIds().get(0));
                        getUsedEntity(refModelNode.appNode.model.name(), refModelNode.model().name());
                    }
                } else if (member.type() == EntityMemberModel.EntityMemberType.EntitySet) {
                    var entitySet = (EntitySetModel) member;
                    var setModelNode = hub.designTree.findModelNode(ModelType.Entity
                            , entitySet.refModelId());
                    getUsedEntity(setModelNode.appNode.model.name(), setModelNode.model().name());
                }
            }
        }
        return entityModelNode;
    }

    /**
     * 用于生成实体运行时导航属性时判断
     * @param entityFullName eg: sys.entities.Employee
     */
    protected boolean isUsedEntity(String entityFullName) {
        return usedEntities.get(entityFullName) != null;
    }

    /** 添加为服务方法,如果有重名抛异常 */
    private void addAsServiceMethod(MethodDeclaration node) {
        var exists = publicMethods.stream().filter(
                m -> m.getName().getIdentifier().equals(node.getName().getIdentifier())).findAny();
        if (exists.isPresent())
            throw new RuntimeException("Service method has exists:" + node.getName().toString());

        publicMethods.add(node);
    }

    /** 最后附加服务接口实现及使用的实体类 */
    public void finish() {
        //附加IService.invokeAsync()
        var invokeMethod = makeIServiceImplements();

        var listRewrite =
                astRewrite.getListRewrite(_serviceTypeDeclaration, TypeDeclaration.BODY_DECLARATIONS_PROPERTY);
        listRewrite.insertLast(invokeMethod, null);

        //附加用到的实体
        for (var modelNode : usedEntities.values()) {
            listRewrite.insertLast(EntityCodeGenerator.makeEntityRuntimeCode(this, modelNode), null);
        }
    }

    /** 生成实现IService的代码 */
    private MethodDeclaration makeIServiceImplements() {
        //返回类型
        var typeCompletableFuture =
                ast.newSimpleType(ast.newName("java.util.concurrent.CompletableFuture"));
        var returnType = ast.newParameterizedType(typeCompletableFuture);
        returnType.typeArguments().add(ast.newSimpleType(ast.newName("Object")));

        var invokeMethod = ast.newMethodDeclaration();
        invokeMethod.setName(ast.newSimpleName("invokeAsync"));
        invokeMethod.setReturnType2(returnType);
        invokeMethod.modifiers().add(ast.newModifier(Modifier.ModifierKeyword.PUBLIC_KEYWORD));

        var para1 = ast.newSingleVariableDeclaration();
        para1.setType(ast.newSimpleType(ast.newName("CharSequence")));
        para1.setName(ast.newSimpleName("method"));
        invokeMethod.parameters().add(para1);

        var para2 = ast.newSingleVariableDeclaration();
        para2.setType(ast.newSimpleType(ast.newName(InvokeArgs.class.getName())));
        para2.setName(ast.newSimpleName("args"));
        invokeMethod.parameters().add(para2);

        var body = ast.newBlock();
        //switch处理各公开方法
        var methodToString = ast.newMethodInvocation(); //TODO:暂转换为method.toString()
        methodToString.setName(ast.newSimpleName("toString"));
        methodToString.setExpression(ast.newSimpleName("method"));
        var switchSt = ast.newSwitchStatement();
        switchSt.setExpression(methodToString);

        for (var method : publicMethods) {
            var methodName = ast.newStringLiteral();
            methodName.setLiteralValue(method.getName().getIdentifier());
            var caseSt = ast.newSwitchCase();
            caseSt.expressions().add(methodName);
            switchSt.statements().add(caseSt);

            var invokeEx = ast.newMethodInvocation();
            invokeEx.setName(ast.newSimpleName(method.getName().getIdentifier()));
            //处理参数
            for (int i = 0; i < method.parameters().size(); i++) {
                var para = makeInvokeArgsGet((SingleVariableDeclaration) method.parameters().get(i));
                invokeEx.arguments().add(para);
            }

            //TODO: 暂全部转换为CompletableFuture<Object>，忽略已经是该类型的
            var castEx   = makeFutureCast(invokeEx, ast.newSimpleType(ast.newSimpleName("Object")));
            var returnSt = ast.newReturnStatement();
            returnSt.setExpression(castEx);
            switchSt.statements().add(returnSt);
        }
        //add switch default
        var caseDefault = ast.newSwitchCase();
        switchSt.statements().add(caseDefault);

        var exceptionInfo = ast.newStringLiteral();
        exceptionInfo.setLiteralValue("Unknown method");
        var newException = ast.newClassInstanceCreation();
        newException.setType(ast.newSimpleType(ast.newSimpleName("RuntimeException")));
        newException.arguments().add(exceptionInfo);

        var failedFuture = ast.newMethodInvocation();
        failedFuture.setName(ast.newSimpleName("failedFuture"));
        failedFuture.setExpression(ast.newName("java.util.concurrent.CompletableFuture"));
        failedFuture.arguments().add(newException);

        var returnExSt = ast.newReturnStatement();
        returnExSt.setExpression(failedFuture);
        switchSt.statements().add(returnExSt);

        body.statements().add(switchSt);
        invokeMethod.setBody(body);

        return invokeMethod;
    }

    private MethodInvocation makeInvokeArgsGet(SingleVariableDeclaration para) {
        var getMethod = ast.newMethodInvocation();
        getMethod.setExpression(ast.newSimpleName("args"));

        String getMethodName = null;
        //TODO:***完善以下
        var paraType = para.getType();
        if (paraType.isPrimitiveType()) {
            var primitiveType = (PrimitiveType) paraType;
            var typeCode      = primitiveType.getPrimitiveTypeCode();
            if (typeCode == PrimitiveType.BOOLEAN) {
                getMethodName = "getBool";
            } else if (typeCode == PrimitiveType.INT) {
                getMethodName = "getInt";
            } else if (typeCode == PrimitiveType.LONG) {
                getMethodName = "getLong";
            } else {
                throw new RuntimeException("makeInvokeArgsGet: 未实现");
            }
        } else if (paraType.isSimpleType()) {
            var simpleType = (SimpleType) paraType;
            var typeName   = simpleType.getName().getFullyQualifiedName(); //TODO:resolve

            ITypeBinding entityType;
            if (typeName.equals("String")) {
                getMethodName = "getString";
            } else if (typeName.equals("EntityId") || typeName.equals(EntityId.class.getName())) {
                getMethodName = "getEntityId";
            } else if ((entityType = TypeHelper.getEntityType(simpleType)) != null) {
                var entityRuntimeType = makeEntityRuntimeType(entityType);
                var creation          = ast.newCreationReference();
                creation.setType(entityRuntimeType);
                getMethodName = "getEntity";
                getMethod.arguments().add(creation);
            } else {
                throw new RuntimeException("makeInvokeArgsGet: 未实现type=" + typeName);
            }
        } else {
            throw new RuntimeException("makeInvokeArgsGet: 未实现type=" + paraType.toString());
        }

        getMethod.setName(ast.newSimpleName(getMethodName));
        return getMethod;
    }

    protected MethodInvocation makeFutureCast(Expression expression, Type castType) {
        var castEx = ast.newMethodInvocation();
        castEx.setName(ast.newSimpleName("thenApply"));

        var castLambda = ast.newLambdaExpression();
        castLambda.setParentheses(false);
        var lambdaPara = ast.newVariableDeclarationFragment();
        lambdaPara.setName(ast.newSimpleName("r"));
        castLambda.parameters().add(lambdaPara);
        var lambdaBody = ast.newCastExpression();
        lambdaBody.setType(castType);
        lambdaBody.setExpression(ast.newSimpleName("r"));
        castLambda.setBody(lambdaBody);

        castEx.setExpression(expression);
        castEx.arguments().add(castLambda);

        return castEx;
    }

    private SimpleType makeEntityRuntimeType(ITypeBinding entityType) {
        var entityModelNode = getUsedEntity(entityType);
        var entityClassName = EntityCodeGenerator.makeEntityClassName(entityModelNode);
        return ast.newSimpleType(ast.newName(entityClassName));
    }

    /** t.Name 转换为 t.m("Name") */
    protected MethodInvocation makeEntityExpression(Expression exp, String memberName) {
        var newNode = ast.newMethodInvocation();
        newNode.setName(ast.newSimpleName("m"));
        newNode.setExpression(exp);
        var member = ast.newStringLiteral();
        member.setLiteralValue(memberName);
        newNode.arguments().add(member);
        return newNode;
    }

    /** t.Name 转换为 t.getName() */
    protected MethodInvocation makeEntityGetMember(Expression owner, String memberName) {
        ASTNode newOwner = ASTNode.copySubtree(ast, owner);

        var newNode = ast.newMethodInvocation();
        newNode.setName(ast.newSimpleName("get" + memberName));
        newNode.setExpression((Expression) newOwner);

        return newNode;
    }

    /** 获取e.City.Name or e.Name的e */
    protected static String getIdentifier(Name node) {
        if (node.isSimpleName()) {
            return ((SimpleName) node).getIdentifier();
        } else {
            return getIdentifier(((QualifiedName) node).getQualifier());
        }
    }
}
