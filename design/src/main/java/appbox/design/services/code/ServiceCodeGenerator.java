package appbox.design.services.code;

import appbox.data.EntityId;
import appbox.data.SysEntity;
import appbox.design.DesignHub;
import appbox.design.tree.ModelNode;
import appbox.exceptions.UnknownEntityMember;
import appbox.model.EntityModel;
import appbox.model.ModelType;
import appbox.model.ServiceModel;
import appbox.model.entity.DataFieldModel;
import appbox.model.entity.EntityMemberModel;
import appbox.serialization.IEntityMemberReader;
import appbox.serialization.IEntityMemberWriter;
import appbox.utils.StringUtil;
import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.internal.corext.dom.GenericVisitor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

/** 用于生成运行时的服务代码 */
public final class ServiceCodeGenerator extends GenericVisitor {

    private       TypeDeclaration         _serviceTypeDeclaration;
    /** 公开的服务方法集合 */
    private final List<MethodDeclaration> publicMethods = new ArrayList<>();
    private final Map<String, ModelNode>  usedEntities  = new HashMap<>();

    private final DesignHub    hub;
    private final String       appName;
    private final ServiceModel serviceModel;
    private final ASTRewrite   astRewrite;
    private final AST          ast;

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
        var entityType = TypeHelper.isEntityClass(node);
        if (entityType != null) {
            var entityFullName  = entityType.getQualifiedName();
            var entityModelNode = usedEntities.get(entityFullName);
            if (entityModelNode == null) {
                var pkg     = entityType.getPackage().getJavaElement();
                var appName = pkg.getPath().segment(1);
                var appNode = hub.designTree.findApplicationNodeByName(appName);
                entityModelNode = hub.designTree.findModelNodeByName(
                        appNode.model.id(), ModelType.Entity, entityType.getName());
                usedEntities.put(entityFullName, entityModelNode);
            }
            //转换为运行时类型
            if (!node.isVar()) {
                var entityRuntimeType = ast.newSimpleType(ast.newName(makeEntityClassName(entityModelNode)));
                astRewrite.replace(node, entityRuntimeType, null);
            }
            return false;
        }

        //在这里转换虚拟类型为运行时类型
        //if (node.getName().isSimpleName() && node.getName().getFullyQualifiedName().equals("String")) {
        //    var newType = ast.newSimpleType(ast.newName("Object"));
        //    astRewrite.replace(node, newType, null);
        //}
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
                newNode.setName(ast.newSimpleName("set"
                        + StringUtil.firstUpperCase(qfn.getName().getIdentifier())));
                var newOwner = (Expression) ASTNode.copySubtree(ast, owner);
                newNode.setExpression(newOwner);
                var newArg = (Expression) ASTNode.copySubtree(ast, node.getRightHandSide());
                newNode.arguments().add(newArg);
                astRewrite.replace(node, newNode, null);
                return super.visit(newNode);
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
            var newNode = ast.newMethodInvocation();
            newNode.setName(ast.newSimpleName("get"
                    + StringUtil.firstUpperCase(node.getName().getIdentifier())));
            var newOwner = (Expression) ASTNode.copySubtree(ast, owner);
            newNode.setExpression(newOwner);
            astRewrite.replace(node, newNode, null);

            //newOwner.accept(this);
            return false;
        }

        return super.visit(node);
    }

    //endregion

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
        var invokeMethod = generateIServiceImplements();

        var listRewrite =
                astRewrite.getListRewrite(_serviceTypeDeclaration, TypeDeclaration.BODY_DECLARATIONS_PROPERTY);
        listRewrite.insertLast(invokeMethod, null);

        //附加用到的实体
        for (var modelNode : usedEntities.values()) {
            listRewrite.insertLast(generateEntityRuntimeCode(modelNode), null);
        }
    }

    /** 生成实现IService的代码 */
    private MethodDeclaration generateIServiceImplements() {
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

        var para2    = ast.newSingleVariableDeclaration();
        var typeList = ast.newSimpleType(ast.newName("java.util.List"));
        var type2    = ast.newParameterizedType(typeList);
        type2.typeArguments().add(ast.newSimpleType(ast.newName("appbox.runtime.InvokeArg")));
        para2.setType(type2);
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
                var para = makeInvokeArgsGet((SingleVariableDeclaration) method.parameters().get(i), i);
                invokeEx.arguments().add(para);
            }

            var castEx = ast.newMethodInvocation(); //暂全部转换为CompletableFuture<Object>
            castEx.setName(ast.newSimpleName("thenApply"));
            var castLambda = ast.newLambdaExpression();
            //castLambda.setParentheses(false);
            var lambdaPara = ast.newVariableDeclarationFragment();
            lambdaPara.setName(ast.newSimpleName("r"));
            castLambda.parameters().add(lambdaPara);
            var lambdaBody = ast.newCastExpression();
            lambdaBody.setType(ast.newSimpleType(ast.newSimpleName("Object")));
            lambdaBody.setExpression(ast.newSimpleName("r"));
            castLambda.setBody(lambdaBody);

            castEx.setExpression(invokeEx);
            castEx.arguments().add(castLambda);

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

    private MethodInvocation makeInvokeArgsGet(SingleVariableDeclaration para, int index) {
        var argIndex      = ast.newNumberLiteral(Integer.toString(index));
        var listGetMethod = ast.newMethodInvocation();
        listGetMethod.setExpression(ast.newSimpleName("args"));
        listGetMethod.setName(ast.newSimpleName("get"));
        listGetMethod.arguments().add(argIndex);

        var getMethod = ast.newMethodInvocation();
        getMethod.setExpression(listGetMethod);

        var paraType = para.getType();
        if (paraType.isPrimitiveType()) {
            var primitiveType = (PrimitiveType) paraType;
            var typeCode      = primitiveType.getPrimitiveTypeCode();
            if (typeCode == PrimitiveType.INT) {
                getMethod.setName(ast.newSimpleName("getInt"));
            } else if (typeCode == PrimitiveType.LONG) {
                getMethod.setName(ast.newSimpleName("getLong"));
            } else {
                throw new RuntimeException("未实现");
            }
        } else if (paraType.isSimpleType()) {
            var simpleType = (SimpleType) paraType;
            var typeName   = simpleType.getName().getFullyQualifiedName();
            if (typeName.equals("String")) {
                getMethod.setName(ast.newSimpleName("getString"));
            } else {
                throw new RuntimeException("未实现");
            }
        } else {
            throw new RuntimeException("未实现");
        }

        return getMethod;
    }

    private static String makeEntityClassName(ModelNode modelNode) {
        return String.format("%s_%s",
                StringUtil.firstUpperCase(modelNode.appNode.model.name()), modelNode.model().name());
    }

    /** 生成实体的运行时代码 */
    private TypeDeclaration generateEntityRuntimeCode(ModelNode modelNode) {
        var entityClass     = ast.newTypeDeclaration();
        var entityClassName = makeEntityClassName(modelNode);
        entityClass.setName(ast.newSimpleName(entityClassName));
        entityClass.modifiers().add(ast.newModifier(Modifier.ModifierKeyword.PRIVATE_KEYWORD));
        entityClass.modifiers().add(ast.newModifier(Modifier.ModifierKeyword.STATIC_KEYWORD));
        entityClass.setSuperclassType(ast.newSimpleType(ast.newName(SysEntity.class.getName())));

        var model = (EntityModel) modelNode.model();
        //ctor
        entityClass.bodyDeclarations().add(generateEntityCtorMethod(model, entityClassName));

        //get and set
        for (var member : model.getMembers()) {
            if (member.type() == EntityMemberModel.EntityMemberType.DataField) {
                var dataField = (DataFieldModel) member;
                var fieldName = "_" + StringUtil.firstLowerCase(member.name());

                var vdf = ast.newVariableDeclarationFragment();
                vdf.setName(ast.newSimpleName(fieldName));
                var field = ast.newFieldDeclaration(vdf);
                field.setType(makeDataFieldType(dataField));
                field.modifiers().add(ast.newModifier(Modifier.ModifierKeyword.PRIVATE_KEYWORD));
                entityClass.bodyDeclarations().add(field);

                var getMethod = ast.newMethodDeclaration();
                getMethod.setName(ast.newSimpleName("get" + StringUtil.firstUpperCase(member.name())));
                getMethod.modifiers().add(ast.newModifier(Modifier.ModifierKeyword.PUBLIC_KEYWORD));
                getMethod.setReturnType2(makeDataFieldType(dataField));
                var getBody   = ast.newBlock();
                var getReturn = ast.newReturnStatement();
                getReturn.setExpression(ast.newSimpleName(fieldName));
                getBody.statements().add(getReturn);
                getMethod.setBody(getBody);
                entityClass.bodyDeclarations().add(getMethod);

                var setMethod = ast.newMethodDeclaration();
                setMethod.setName(ast.newSimpleName("set" + StringUtil.firstUpperCase(member.name())));
                setMethod.modifiers().add(ast.newModifier(Modifier.ModifierKeyword.PUBLIC_KEYWORD));
                var setPara = ast.newSingleVariableDeclaration();
                setPara.setName(ast.newSimpleName("value"));
                setPara.setType(makeDataFieldType(dataField));
                setMethod.parameters().add(setPara);
                var setBody       = ast.newBlock();
                var setAssignment = ast.newAssignment();
                setAssignment.setLeftHandSide(ast.newSimpleName(fieldName));
                setAssignment.setRightHandSide(ast.newSimpleName("value"));
                setBody.statements().add(ast.newExpressionStatement(setAssignment));
                setMethod.setBody(setBody);
                entityClass.bodyDeclarations().add(setMethod);
            }
        }

        //overrides
        entityClass.bodyDeclarations().add(generateEntityWriteMemberMethod(model, entityClassName));
        entityClass.bodyDeclarations().add(generateEntityReadMemberMethod(model, entityClassName));

        return entityClass;
    }

    private MethodDeclaration generateEntityCtorMethod(EntityModel model, String className) {
        var ctor = ast.newMethodDeclaration();
        ctor.setConstructor(true);
        ctor.setName(ast.newSimpleName(className));
        ctor.modifiers().add(ast.newModifier(Modifier.ModifierKeyword.PUBLIC_KEYWORD));

        var body      = ast.newBlock();
        var superCall = ast.newSuperConstructorInvocation();
        var arg       = ast.newNumberLiteral(model.id() + "L");
        superCall.arguments().add(arg);
        body.statements().add(superCall);

        ctor.setBody(body);
        return ctor;
    }

    private MethodDeclaration generateEntityWriteMemberMethod(EntityModel model, String className) {
        var method = ast.newMethodDeclaration();
        method.setName(ast.newSimpleName("writeMember"));

        var overrideAnnotation = ast.newMarkerAnnotation();
        overrideAnnotation.setTypeName(ast.newSimpleName("Override"));
        method.modifiers().add(overrideAnnotation);
        method.modifiers().add(ast.newModifier(Modifier.ModifierKeyword.PUBLIC_KEYWORD));

        var para1 = ast.newSingleVariableDeclaration();
        para1.setName(ast.newSimpleName("id"));
        para1.setType(ast.newPrimitiveType(PrimitiveType.SHORT));
        var para2 = ast.newSingleVariableDeclaration();
        para2.setName(ast.newSimpleName("bs"));
        para2.setType(ast.newSimpleType(ast.newName(IEntityMemberWriter.class.getName())));
        var para3 = ast.newSingleVariableDeclaration();
        para3.setName(ast.newSimpleName("flags"));
        para3.setType(ast.newPrimitiveType(PrimitiveType.BYTE));
        method.parameters().add(para1);
        method.parameters().add(para2);
        method.parameters().add(para3);

        var body     = ast.newBlock();
        var switchst = ast.newSwitchStatement();
        switchst.setExpression(ast.newSimpleName("id"));

        for (var memeber : model.getMembers()) {
            var switchCase = ast.newSwitchCase();
            if (memeber.type() == EntityMemberModel.EntityMemberType.DataField) {
                var dataField = (DataFieldModel) memeber;
                var fieldName = "_" + StringUtil.firstLowerCase(dataField.name());

                var castExp = ast.newCastExpression();
                castExp.setType(ast.newPrimitiveType(PrimitiveType.SHORT));
                castExp.setExpression(ast.newNumberLiteral(Short.toString(memeber.memberId())));
                switchCase.expressions().add(castExp);
                switchst.statements().add(switchCase);

                var invokeExp = ast.newMethodInvocation();
                invokeExp.setExpression(ast.newSimpleName("bs"));
                invokeExp.setName(ast.newSimpleName("writeMember"));
                invokeExp.arguments().add(ast.newSimpleName("id"));
                invokeExp.arguments().add(ast.newSimpleName(fieldName));
                invokeExp.arguments().add(ast.newSimpleName("flags"));
                switchst.statements().add(ast.newExpressionStatement(invokeExp));

                switchst.statements().add(ast.newBreakStatement());
            } else {
                //TODO:
            }
        }

        //switch default
        switchst.statements().add(ast.newSwitchCase());
        switchst.statements().add(generateThrowUnknownMember(className));

        body.statements().add(switchst);
        method.setBody(body);
        return method;
    }

    private MethodDeclaration generateEntityReadMemberMethod(EntityModel model, String className) {
        var method = ast.newMethodDeclaration();
        method.setName(ast.newSimpleName("readMember"));

        var overrideAnnotation = ast.newMarkerAnnotation();
        overrideAnnotation.setTypeName(ast.newSimpleName("Override"));
        method.modifiers().add(overrideAnnotation);
        method.modifiers().add(ast.newModifier(Modifier.ModifierKeyword.PUBLIC_KEYWORD));

        var para1 = ast.newSingleVariableDeclaration();
        para1.setName(ast.newSimpleName("id"));
        para1.setType(ast.newPrimitiveType(PrimitiveType.SHORT));
        var para2 = ast.newSingleVariableDeclaration();
        para2.setName(ast.newSimpleName("bs"));
        para2.setType(ast.newSimpleType(ast.newName(IEntityMemberReader.class.getName())));
        var para3 = ast.newSingleVariableDeclaration();
        para3.setName(ast.newSimpleName("flags"));
        para3.setType(ast.newPrimitiveType(PrimitiveType.INT));
        method.parameters().add(para1);
        method.parameters().add(para2);
        method.parameters().add(para3);

        var body = ast.newBlock();

        var switchst = ast.newSwitchStatement();
        switchst.setExpression(ast.newSimpleName("id"));

        for (var memeber : model.getMembers()) {
            var switchCase = ast.newSwitchCase();
            if (memeber.type() == EntityMemberModel.EntityMemberType.DataField) {
                var dataField = (DataFieldModel) memeber;
                var fieldName = "_" + StringUtil.firstLowerCase(dataField.name());

                var castExp = ast.newCastExpression();
                castExp.setType(ast.newPrimitiveType(PrimitiveType.SHORT));
                castExp.setExpression(ast.newNumberLiteral(Short.toString(memeber.memberId())));
                switchCase.expressions().add(castExp);
                switchst.statements().add(switchCase);

                var assignExp = ast.newAssignment();
                assignExp.setLeftHandSide(ast.newSimpleName(fieldName));
                var invokeExp = ast.newMethodInvocation();
                invokeExp.setExpression(ast.newSimpleName("bs"));
                invokeExp.setName(ast.newSimpleName("read" + getDataFieldType(dataField, true) + "Member"));
                invokeExp.arguments().add(ast.newSimpleName("flags"));
                assignExp.setRightHandSide(invokeExp);

                switchst.statements().add(ast.newExpressionStatement(assignExp));

                switchst.statements().add(ast.newBreakStatement());
            } else {
                //TODO:
            }
        }

        //switch default
        switchst.statements().add(ast.newSwitchCase());
        switchst.statements().add(generateThrowUnknownMember(className));

        body.statements().add(switchst);
        method.setBody(body);
        return method;
    }

    private ThrowStatement generateThrowUnknownMember(String className) {
        var throwError = ast.newThrowStatement();
        var newError   = ast.newClassInstanceCreation();
        newError.setType(ast.newSimpleType(ast.newName(UnknownEntityMember.class.getName())));
        var arg1 = ast.newTypeLiteral();
        arg1.setType(ast.newSimpleType(ast.newSimpleName(className)));
        newError.arguments().add(arg1);
        newError.arguments().add(ast.newSimpleName("id"));
        throwError.setExpression(newError);
        return throwError;
    }

    private Type makeDataFieldType(DataFieldModel field) {
        switch (field.dataType()) {
            case EntityId:
                return ast.newSimpleType(ast.newName(EntityId.class.getName()));
            case String:
                return ast.newSimpleType(ast.newSimpleName("String"));
            case DateTime:
                return ast.newSimpleType(ast.newName(LocalDateTime.class.getName()));
            case Short:
                return field.allowNull() ?
                        ast.newSimpleType(ast.newSimpleName("Short")) :
                        ast.newPrimitiveType(PrimitiveType.SHORT);
            case Int:
            case Enum:
                return field.allowNull() ?
                        ast.newSimpleType(ast.newSimpleName("Integer")) :
                        ast.newPrimitiveType(PrimitiveType.INT);
            case Long:
                return field.allowNull() ?
                        ast.newSimpleType(ast.newSimpleName("Long")) :
                        ast.newPrimitiveType(PrimitiveType.LONG);
            case Decimal:
                return ast.newSimpleType(ast.newName(BigDecimal.class.getName()));
            case Bool:
                return field.allowNull() ?
                        ast.newSimpleType(ast.newSimpleName("Boolean")) :
                        ast.newPrimitiveType(PrimitiveType.BOOLEAN);
            case Guid:
                return ast.newSimpleType(ast.newName(UUID.class.getName()));
            case Byte:
                return field.allowNull() ?
                        ast.newSimpleType(ast.newSimpleName("Byte")) :
                        ast.newPrimitiveType(PrimitiveType.BYTE);
            case Binary:
                return ast.newArrayType(ast.newPrimitiveType(PrimitiveType.BYTE));
            case Float:
                return field.allowNull() ?
                        ast.newSimpleType(ast.newSimpleName("Float")) :
                        ast.newPrimitiveType(PrimitiveType.FLOAT);
            case Double:
                return field.allowNull() ?
                        ast.newSimpleType(ast.newSimpleName("Double")) :
                        ast.newPrimitiveType(PrimitiveType.DOUBLE);
            default:
                return ast.newSimpleType(ast.newSimpleName("Object"));
        }
    }

    private String getDataFieldType(DataFieldModel field, boolean forRead) {
        switch (field.dataType()) {
            case DateTime:
                return "Date";
            case Enum:
                return "Int";
            case Guid:
                return "UUID";
            default:
                return field.dataType().name();
        }
    }

}
