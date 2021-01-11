package appbox.design.services.code;

import appbox.data.EntityId;
import appbox.data.PersistentState;
import appbox.data.SqlEntity;
import appbox.data.SysEntity;
import appbox.design.tree.ModelNode;
import appbox.exceptions.UnknownEntityMember;
import appbox.logging.Log;
import appbox.model.EntityModel;
import appbox.model.ModelType;
import appbox.model.entity.DataFieldModel;
import appbox.model.entity.EntityMemberModel;
import appbox.model.entity.EntityRefModel;
import appbox.model.entity.EntitySetModel;
import appbox.serialization.IEntityMemberReader;
import appbox.serialization.IEntityMemberWriter;
import org.eclipse.jdt.core.dom.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

//TODO:考虑仅生成服务使用到的成员

/** 生成实体的运行时代码 */
public final class EntityCodeGenerator {
    private EntityCodeGenerator() {}

    /** 生成实体的运行时代码 */
    static TypeDeclaration makeEntityRuntimeCode(ServiceCodeGenerator generator, ModelNode modelNode) {
        final AST ast             = generator.ast;
        var       model           = (EntityModel) modelNode.model();
        var       entityClass     = ast.newTypeDeclaration();
        var       entityClassName = makeEntityClassName(modelNode);
        entityClass.setName(ast.newSimpleName(entityClassName));
        entityClass.modifiers().add(ast.newModifier(Modifier.ModifierKeyword.PUBLIC_KEYWORD));
        entityClass.modifiers().add(ast.newModifier(Modifier.ModifierKeyword.STATIC_KEYWORD));

        //extends
        if (model.sysStoreOptions() != null) {
            entityClass.setSuperclassType(ast.newSimpleType(ast.newName(SysEntity.class.getName())));
        } else if (model.sqlStoreOptions() != null) {
            entityClass.setSuperclassType(ast.newSimpleType(ast.newName(SqlEntity.class.getName())));
        }

        //ctor
        makeEntityCtorMethod(ast, entityClass, model);

        //get and set
        for (var member : model.getMembers()) {
            if (member.type() == EntityMemberModel.EntityMemberType.DataField) {
                makeEntityMember(generator, entityClass, makeDataFieldType(ast, (DataFieldModel) member), member);
            } else if (member.type() == EntityMemberModel.EntityMemberType.EntityRef) {
                makeEntityRef(generator, entityClass, (EntityRefModel) member);
            } else if (member.type() == EntityMemberModel.EntityMemberType.EntitySet) {
                makeEntitySet(generator, entityClass, (EntitySetModel) member);
            } else {
                //TODO:
                Log.warn("暂未实现生成实体运行时代码，成员类型: " + member.type().name());
            }
        }

        //overrides
        entityClass.bodyDeclarations().add(makeEntityWriteMemberMethod(ast, model, entityClassName));
        entityClass.bodyDeclarations().add(makeEntityReadMemberMethod(generator, model, entityClassName));

        return entityClass;
    }

    /** 生成运行时实体名称 eg: SYS_Employee */
    static String makeEntityClassName(ModelNode modelNode) {
        return String.format("%s_%s", modelNode.appNode.model.name().toUpperCase(), modelNode.model().name());
    }

    private static void makeEntityCtorMethod(AST ast, TypeDeclaration entityClass, EntityModel model) {
        var ctor = ast.newMethodDeclaration();
        ctor.setConstructor(true);
        ctor.setName(ast.newSimpleName(entityClass.getName().getIdentifier()));
        ctor.modifiers().add(ast.newModifier(Modifier.ModifierKeyword.PUBLIC_KEYWORD));

        var body          = ast.newBlock();
        var superCall     = ast.newSuperConstructorInvocation();
        var supperCallArg = ast.newNumberLiteral(model.id() + "L");
        superCall.arguments().add(supperCallArg);
        body.statements().add(superCall);

        ctor.setBody(body);
        entityClass.bodyDeclarations().add(ctor);

        //绑定至存储且具备主键的生成带主键的构造, eg: Employee(String pkName) { _Name = pkName; }
        if (model.sqlStoreOptions() != null && model.sqlStoreOptions().hasPrimaryKeys()) {
            ctor = ast.newMethodDeclaration();
            ctor.setConstructor(true);
            ctor.setName(ast.newSimpleName(entityClass.getName().getIdentifier()));
            ctor.modifiers().add(ast.newModifier(Modifier.ModifierKeyword.PUBLIC_KEYWORD));

            for (var pk : model.sqlStoreOptions().primaryKeys()) {
                var ctorPara = ast.newSingleVariableDeclaration();
                var pkMember = (DataFieldModel) model.getMember(pk.memberId);
                ctorPara.setName(ast.newSimpleName("pk" + pkMember.name()));
                ctorPara.setType(makeDataFieldType(ast, pkMember));
                ctor.parameters().add(ctorPara);
            }

            body          = ast.newBlock();
            superCall     = ast.newSuperConstructorInvocation();
            supperCallArg = ast.newNumberLiteral(model.id() + "L");
            superCall.arguments().add(supperCallArg);
            body.statements().add(superCall);

            for (var pk : model.sqlStoreOptions().primaryKeys()) {
                var pkMember = (DataFieldModel) model.getMember(pk.memberId);
                var pkAssign = ast.newAssignment();
                pkAssign.setLeftHandSide(ast.newSimpleName("_" + pkMember.name()));
                pkAssign.setRightHandSide(ast.newSimpleName("pk" + pkMember.name()));
                body.statements().add(ast.newExpressionStatement(pkAssign));
            }

            ctor.setBody(body);
            entityClass.bodyDeclarations().add(ctor);
        }
    }

    private static void makeEntityRef(ServiceCodeGenerator generator
            , TypeDeclaration entityClass, EntityRefModel entityRef) {
        final var tree         = generator.hub.designTree;
        final var refModelNode = tree.findModelNode(ModelType.Entity, entityRef.getRefModelIds().get(0));

        var entityType = makeNavigationEntityType(refModelNode, entityRef.isAggregationRef(), generator.ast);
        makeEntityMember(generator, entityClass, entityType, entityRef);
    }

    private static void makeEntitySet(ServiceCodeGenerator generator
            , TypeDeclaration entityClass, EntitySetModel entitySet) {
        final var ast          = generator.ast;
        final var tree         = generator.hub.designTree;
        final var setModelNode = tree.findModelNode(ModelType.Entity, entitySet.refModelId());

        var entityType = makeNavigationEntityType(setModelNode, false, generator.ast);
        var listType   = ast.newParameterizedType(ast.newSimpleType(ast.newName(List.class.getName())));
        listType.typeArguments().add(entityType);

        final var fieldName = "_" + entitySet.name();
        makeEntityPrivateField(ast, entityClass, listType, fieldName);

        var getMethod = ast.newMethodDeclaration();
        getMethod.setName(ast.newSimpleName("get" + entitySet.name()));
        getMethod.modifiers().add(ast.newModifier(Modifier.ModifierKeyword.PUBLIC_KEYWORD));
        getMethod.setReturnType2((Type) ASTNode.copySubtree(generator.ast, listType));
        var getBody = ast.newBlock();

        var ifcon1 = ast.newInfixExpression();
        ifcon1.setOperator(InfixExpression.Operator.EQUALS);
        var ifcon1Left = ast.newMethodInvocation();
        ifcon1Left.setName(ast.newSimpleName("persistentState"));
        ifcon1.setLeftOperand(ifcon1Left);
        ifcon1.setRightOperand(ast.newName(PersistentState.class.getName() + "." + PersistentState.Detached.name()));

        var ifcon2 = ast.newInfixExpression();
        ifcon2.setOperator(InfixExpression.Operator.EQUALS);
        ifcon2.setLeftOperand(ast.newSimpleName(fieldName));
        ifcon2.setRightOperand(ast.newNullLiteral());

        var ifcondition = ast.newInfixExpression();
        ifcondition.setOperator(InfixExpression.Operator.CONDITIONAL_AND);
        ifcondition.setLeftOperand(ifcon1);
        ifcondition.setRightOperand(ifcon2);

        var ifst = ast.newIfStatement();
        ifst.setExpression(ifcondition);
        var assignment = ast.newAssignment();
        assignment.setLeftHandSide(ast.newSimpleName(fieldName));
        var arrayListType = ast.newParameterizedType(ast.newSimpleType(ast.newName(ArrayList.class.getName())));
        arrayListType.typeArguments().add(makeNavigationEntityType(setModelNode, false, generator.ast));
        var creation = ast.newClassInstanceCreation();
        creation.setType(arrayListType);
        assignment.setRightHandSide(creation);
        ifst.setThenStatement(ast.newExpressionStatement(assignment));
        getBody.statements().add(ifst);

        var getReturn = ast.newReturnStatement();
        getReturn.setExpression(ast.newSimpleName(fieldName));
        getBody.statements().add(getReturn);

        getMethod.setBody(getBody);
        entityClass.bodyDeclarations().add(getMethod);
    }

    /** 专用于实体导航属性的目标类型生成 */
    private static Type makeNavigationEntityType(ModelNode refModelNode, boolean useEntityBaseType, AST ast) {
        final var refEntityModel = (EntityModel) refModelNode.model();
        Type      entityType     = null;
        if (useEntityBaseType) {
            var baseEntityTypeName = appbox.data.Entity.class.getName();
            if (refEntityModel.sqlStoreOptions() != null)
                baseEntityTypeName = appbox.data.SqlEntity.class.getName();
            else if (refEntityModel.sysStoreOptions() != null)
                baseEntityTypeName = appbox.data.SysEntity.class.getName();
            entityType = ast.newSimpleType(ast.newName(baseEntityTypeName));
        } else {
            var refEntityTypeName = makeEntityClassName(refModelNode);
            entityType = ast.newSimpleType(ast.newName(refEntityTypeName));
        }
        return entityType;
    }

    private static void makeEntityPrivateField(AST ast, TypeDeclaration entityClass, Type fieldType, String fieldName) {
        var vdf = ast.newVariableDeclarationFragment();
        vdf.setName(ast.newSimpleName(fieldName));
        var field = ast.newFieldDeclaration(vdf);
        field.setType(fieldType);
        field.modifiers().add(ast.newModifier(Modifier.ModifierKeyword.PRIVATE_KEYWORD));
        entityClass.bodyDeclarations().add(field);
    }

    /** DataField or EntityRef's getXXX and setXXX */
    private static void makeEntityMember(ServiceCodeGenerator generator, TypeDeclaration entityClass,
                                         Type memberType, EntityMemberModel member) {
        final var ast       = generator.ast;
        final var fieldName = "_" + member.name();

        makeEntityPrivateField(ast, entityClass, memberType, fieldName);

        var getMethod = ast.newMethodDeclaration();
        getMethod.setName(ast.newSimpleName("get" + member.name()));
        getMethod.modifiers().add(ast.newModifier(Modifier.ModifierKeyword.PUBLIC_KEYWORD));
        getMethod.setReturnType2((Type) ASTNode.copySubtree(ast, memberType));
        var getBody = ast.newBlock();
        //TODO: EntityRef成员判断外键成员是否有值，有值但私有成员无值则抛未加载异常
        var getReturn = ast.newReturnStatement();
        getReturn.setExpression(ast.newSimpleName(fieldName));
        getBody.statements().add(getReturn);
        getMethod.setBody(getBody);
        entityClass.bodyDeclarations().add(getMethod);

        if (member.type() == EntityMemberModel.EntityMemberType.DataField
                && ((DataFieldModel) member).isPrimaryKey()) {
            return; //主键成员不生成setXXX()
        }

        var setMethod = ast.newMethodDeclaration();
        setMethod.setName(ast.newSimpleName("set" + member.name()));
        setMethod.modifiers().add(ast.newModifier(Modifier.ModifierKeyword.PUBLIC_KEYWORD));
        var setPara = ast.newSingleVariableDeclaration();
        setPara.setName(ast.newSimpleName("value"));
        setPara.setType((Type) ASTNode.copySubtree(ast, memberType));
        setMethod.parameters().add(setPara);
        var setBody = ast.newBlock();

        if (member.type() == EntityMemberModel.EntityMemberType.DataField) {
            final var dataField = (DataFieldModel) member;
            if (dataField.isForeignKey()) {
                //如果是外键成员直接清空对应的EntityRef的私有字段, eg: _City = null;
                var entityRefMember   = dataField.getEntityRefModelByForeignKey();
                var setEntityRef2Null = ast.newAssignment();
                setEntityRef2Null.setLeftHandSide(ast.newSimpleName("_" + entityRefMember.name()));
                setEntityRef2Null.setRightHandSide(ast.newNullLiteral());
                setBody.statements().add(ast.newExpressionStatement(setEntityRef2Null));
            }
        } else if (member.type() == EntityMemberModel.EntityMemberType.EntityRef) {
            final var entityRef = (EntityRefModel) member;
            //1.判断不允许null的EntityRef
            if (!entityRef.allowNull()) {
                setBody.statements().add(makeCheckNullStatement(ast, "value", "Not allow null"));
            }
            //2.映射至存储的需要设置外键成员的值
            if (entityRef.owner.sqlStoreOptions() != null) {
                if (entityRef.isAggregationRef()) {
                    //TODO: eg: setCostSourceFK1(value == null : null ? getCostSourcePK(value, 0))
                    //其中getConstSourcePK通过实例类型判断
                    throw new RuntimeException("未实现生成聚合EntityRef成员的setXXX()");
                } else {
                    var refModelNode = generator.hub.designTree
                            .findModelNode(ModelType.Entity, entityRef.getRefModelIds().get(0));
                    var refModel = (EntityModel) refModelNode.model();
                    for (int i = 0; i < entityRef.getFKMemberIds().length; i++) {
                        //eg: setCityId(value == null : null ? value.getId())
                        var fkMemberName = entityRef.owner.getMember(entityRef.getFKMemberIds()[i]).name();
                        var pkMemberId   = refModel.sqlStoreOptions().primaryKeys()[i].memberId;
                        var pkMemberName = refModel.getMember(pkMemberId).name();
                        var setMember = makeSetEntityMemberValue(ast, fkMemberName,
                                makeConditionalGetEntityValue(ast, "get" + pkMemberName));
                        setBody.statements().add(ast.newExpressionStatement(setMember));
                    }
                }
            } else if (entityRef.owner.sysStoreOptions() != null) {
                var fkMemberName = entityRef.owner.getMember(entityRef.getFKMemberIds()[0]).name();
                var setMember = makeSetEntityMemberValue(ast, fkMemberName
                        , makeConditionalGetEntityValue(ast, "id"));
                setBody.statements().add(ast.newExpressionStatement(setMember));
            }

            if (entityRef.owner.storeOptions() != null && entityRef.isAggregationRef()) {
                //eg: setBillType(value == null : null ? value.modelId())
                var typeMemberName = entityRef.owner.getMember(entityRef.typeMemberId()).name();
                var setMember = makeSetEntityMemberValue(ast, typeMemberName
                        , makeConditionalGetEntityValue(ast, "modelId"));
                setBody.statements().add(ast.newExpressionStatement(setMember));
            }
        }

        var setAssignment = ast.newAssignment();
        setAssignment.setLeftHandSide(ast.newSimpleName(fieldName));
        setAssignment.setRightHandSide(ast.newSimpleName("value"));
        setBody.statements().add(ast.newExpressionStatement(setAssignment));

        //如果是DataField且映射至存储，调用onPropertyChanged()
        if (member.owner.storeOptions() != null && member.type() == EntityMemberModel.EntityMemberType.DataField) {
            var onChanged = ast.newMethodInvocation();
            onChanged.setName(ast.newSimpleName("onPropertyChanged"));
            var cast = ast.newCastExpression();
            cast.setType(ast.newPrimitiveType(PrimitiveType.SHORT));
            cast.setExpression(ast.newNumberLiteral(Short.toString(member.memberId())));
            onChanged.arguments().add(cast);
            setBody.statements().add(ast.newExpressionStatement(onChanged));
        }

        setMethod.setBody(setBody);
        entityClass.bodyDeclarations().add(setMethod);
    }

    private static MethodInvocation makeSetEntityMemberValue(AST ast, String memberName, Expression valueExpression) {
        var setMember = ast.newMethodInvocation();
        setMember.setName(ast.newSimpleName("set" + memberName));
        setMember.arguments().add(valueExpression);
        return setMember;
    }

    /** eg: value == null : null ? value.getId() */
    private static ConditionalExpression makeConditionalGetEntityValue(AST ast, String getMember) {
        var checkNull = ast.newInfixExpression();
        checkNull.setOperator(InfixExpression.Operator.EQUALS);
        checkNull.setLeftOperand(ast.newSimpleName("value"));
        checkNull.setRightOperand(ast.newNullLiteral());

        var getValue = ast.newMethodInvocation();
        getValue.setName(ast.newSimpleName(getMember));
        getValue.setExpression(ast.newSimpleName("value"));

        var conditional = ast.newConditionalExpression();
        conditional.setExpression(checkNull);
        conditional.setThenExpression(ast.newNullLiteral());
        conditional.setElseExpression(getValue);
        return conditional;
    }

    private static MethodDeclaration makeEntityWriteMemberMethod(AST ast, EntityModel model, String className) {
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
            //暂DataField\EntityRef\EntitySet通用 TODO:其他特殊类型待定
            var fieldName = "_" + memeber.name();

            switchCase.expressions().add(makeCastMemberId(ast, memeber.memberId()));
            switchst.statements().add(switchCase);

            var invokeExp = ast.newMethodInvocation();
            invokeExp.setExpression(ast.newSimpleName("bs"));
            invokeExp.setName(ast.newSimpleName("writeMember"));
            invokeExp.arguments().add(ast.newSimpleName("id"));
            invokeExp.arguments().add(ast.newSimpleName(fieldName));
            invokeExp.arguments().add(ast.newSimpleName("flags"));
            switchst.statements().add(ast.newExpressionStatement(invokeExp));

            switchst.statements().add(ast.newBreakStatement());
        }

        //switch default
        switchst.statements().add(ast.newSwitchCase());
        switchst.statements().add(makeThrowUnknownMember(ast, className));

        body.statements().add(switchst);
        method.setBody(body);
        return method;
    }

    private static MethodDeclaration makeEntityReadMemberMethod(ServiceCodeGenerator generator
            , EntityModel model, String className) {
        final var ast    = generator.ast;
        var       method = ast.newMethodDeclaration();
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
            //暂DataField\EntityRef\EntitySet通用 TODO:其他特殊类型待定
            var switchCase = ast.newSwitchCase();
            switchCase.expressions().add(makeCastMemberId(ast, memeber.memberId()));
            switchst.statements().add(switchCase);

            var readExp = ast.newMethodInvocation();
            readExp.setExpression(ast.newSimpleName("bs"));
            readExp.arguments().add(ast.newSimpleName("flags"));
            if (memeber.type() == EntityMemberModel.EntityMemberType.DataField) {
                readExp.setName(ast.newSimpleName("read"
                        + getDataFieldType((DataFieldModel) memeber, true) + "Member"));
            } else if (memeber.type() == EntityMemberModel.EntityMemberType.EntityRef) {
                readExp.setName(ast.newSimpleName("readRefMember"));
                var entityRef = (EntityRefModel) memeber;
                if (entityRef.isAggregationRef()) {
                    //TODO:专用readAggRefMember()
                    throw new RuntimeException("未实现");
                }
                var refModelNode = generator.hub.designTree.findModelNode(ModelType.Entity,
                        entityRef.getRefModelIds().get(0));
                var creatorArg = ast.newCreationReference();
                creatorArg.setType(ast.newSimpleType(ast.newName(makeEntityClassName(refModelNode))));
                readExp.arguments().add(creatorArg);
            } else if (memeber.type() == EntityMemberModel.EntityMemberType.EntitySet) {
                readExp.setName(ast.newSimpleName("readSetMember"));
                var entitySet = (EntitySetModel) memeber;
                var setModelNode = generator.hub.designTree.findModelNode(ModelType.Entity,
                        entitySet.refModelId());
                var creatorArg = ast.newCreationReference();
                creatorArg.setType(ast.newSimpleType(ast.newName(makeEntityClassName(setModelNode))));
                readExp.arguments().add(creatorArg);
            }

            var assignExp = ast.newAssignment();
            assignExp.setLeftHandSide(ast.newSimpleName("_" + memeber.name()));
            assignExp.setRightHandSide(readExp);

            switchst.statements().add(ast.newExpressionStatement(assignExp));
            switchst.statements().add(ast.newBreakStatement());
        }

        //switch default
        switchst.statements().add(ast.newSwitchCase());
        switchst.statements().add(makeThrowUnknownMember(ast, className));

        body.statements().add(switchst);
        method.setBody(body);
        return method;
    }

    /** (short) 128 */
    private static CastExpression makeCastMemberId(AST ast, short memberId) {
        var castExp = ast.newCastExpression();
        castExp.setType(ast.newPrimitiveType(PrimitiveType.SHORT));
        castExp.setExpression(ast.newNumberLiteral(Short.toString(memberId)));
        return castExp;
    }

    private static IfStatement makeCheckNullStatement(AST ast, String local, String errorMsg) {
        var condition = ast.newInfixExpression();
        condition.setOperator(InfixExpression.Operator.EQUALS);
        condition.setLeftOperand(ast.newName(local));
        condition.setRightOperand(ast.newNullLiteral());

        var ifSt = ast.newIfStatement();
        ifSt.setExpression(condition);
        ifSt.setThenStatement(makeThrowRuntimeException(ast, errorMsg));
        return ifSt;
    }

    private static ThrowStatement makeThrowUnknownMember(AST ast, String className) {
        var newError = ast.newClassInstanceCreation();
        newError.setType(ast.newSimpleType(ast.newName(UnknownEntityMember.class.getName())));
        var arg1 = ast.newTypeLiteral();
        arg1.setType(ast.newSimpleType(ast.newSimpleName(className)));
        newError.arguments().add(arg1);
        newError.arguments().add(ast.newSimpleName("id"));

        var throwError = ast.newThrowStatement();
        throwError.setExpression(newError);
        return throwError;
    }

    private static ThrowStatement makeThrowRuntimeException(AST ast, String message) {
        var newError = ast.newClassInstanceCreation();
        newError.setType(ast.newSimpleType(ast.newName(RuntimeException.class.getName())));
        if (message != null && !message.isEmpty()) {
            var arg = ast.newStringLiteral();
            arg.setLiteralValue(message);
            newError.arguments().add(arg);
        }

        var throwError = ast.newThrowStatement();
        throwError.setExpression(newError);
        return throwError;
    }

    private static Type makeDataFieldType(AST ast, DataFieldModel field) {
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

    private static String getDataFieldType(DataFieldModel field, boolean forRead) {
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
