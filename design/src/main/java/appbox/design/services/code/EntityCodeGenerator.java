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
import java.util.List;
import java.util.UUID;

//TODO:考虑仅生成服务使用到的成员

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
        entityClass.bodyDeclarations().add(makeEntityCtorMethod(ast, model, entityClassName));

        //get and set
        for (var member : model.getMembers()) {
            if (member.type() == EntityMemberModel.EntityMemberType.DataField) {
                makeDataField(ast, entityClass, (DataFieldModel) member);
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
        entityClass.bodyDeclarations().add(makeEntityReadMemberMethod(ast, model, entityClassName));

        return entityClass;
    }

    /** 生成运行时实体名称 eg: Sys_Employee */
    static String makeEntityClassName(ModelNode modelNode) {
        return String.format("%s_%s", modelNode.appNode.model.name(), modelNode.model().name());
    }

    private static MethodDeclaration makeEntityCtorMethod(AST ast, EntityModel model, String className) {
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

    private static void makeDataField(AST ast, TypeDeclaration entityClass, DataFieldModel dataField) {
        makeEntityMember(ast, entityClass, makeDataFieldType(ast, dataField), dataField.name());
    }

    private static void makeEntityRef(ServiceCodeGenerator generator
            , TypeDeclaration entityClass, EntityRefModel entityRef) {
        final var ast          = generator.ast;
        final var tree         = generator.hub.designTree;
        final var refModelNode = tree.findModelNode(ModelType.Entity, entityRef.getRefModelIds().get(0));
        //注意非服务使用到的实体类转换为基类
        final var useEntityBaseType = entityRef.isAggregationRef() ||
                generator.isUsedEntity(String.format("%s.entities.%s",
                        refModelNode.appNode.model.name(), refModelNode.model().name()));

        var entityType = makeNavigationEntityType(refModelNode, useEntityBaseType, generator.ast);
        makeEntityMember(generator.ast, entityClass, entityType, entityRef.name());
    }

    private static void makeEntitySet(ServiceCodeGenerator generator
            , TypeDeclaration entityClass, EntitySetModel entitySet) {
        final var ast            = generator.ast;
        final var tree           = generator.hub.designTree;
        final var refModelNode   = tree.findModelNode(ModelType.Entity, entitySet.refModelId());
        final var refEntityModel = (EntityModel) refModelNode.model();
        //注意非服务使用到的实体类转换为基类
        final var useEntityBaseType = generator.isUsedEntity(String.format("%s.entities.%s",
                refModelNode.appNode.model.name(), refModelNode.model().name()));

        var entityType = makeNavigationEntityType(refModelNode, useEntityBaseType, generator.ast);
        var listType   = ast.newParameterizedType(ast.newSimpleType(ast.newName(List.class.getName())));
        listType.typeArguments().add(entityType);

        final var fieldName = "_" + entitySet.name();
        makeEntityPrivateField(ast, entityClass, listType, fieldName);

        var getMethod = ast.newMethodDeclaration();
        getMethod.setName(ast.newSimpleName("get" + entitySet.name()));
        getMethod.modifiers().add(ast.newModifier(Modifier.ModifierKeyword.PUBLIC_KEYWORD));
        getMethod.setReturnType2(listType);
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
        var creation = ast.newClassInstanceCreation();
        creation.setType(listType);
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
    private static void makeEntityMember(AST ast, TypeDeclaration entityClass, Type memberType, String memberName) {
        final var fieldName = "_" + memberName;

        makeEntityPrivateField(ast, entityClass, memberType, memberName);

        var getMethod = ast.newMethodDeclaration();
        getMethod.setName(ast.newSimpleName("get" + memberName));
        getMethod.modifiers().add(ast.newModifier(Modifier.ModifierKeyword.PUBLIC_KEYWORD));
        getMethod.setReturnType2(memberType);
        var getBody   = ast.newBlock();
        var getReturn = ast.newReturnStatement();
        getReturn.setExpression(ast.newSimpleName(fieldName));
        getBody.statements().add(getReturn);
        getMethod.setBody(getBody);
        entityClass.bodyDeclarations().add(getMethod);

        var setMethod = ast.newMethodDeclaration();
        setMethod.setName(ast.newSimpleName("set" + memberName));
        setMethod.modifiers().add(ast.newModifier(Modifier.ModifierKeyword.PUBLIC_KEYWORD));
        var setPara = ast.newSingleVariableDeclaration();
        setPara.setName(ast.newSimpleName("value"));
        setPara.setType(memberType);
        setMethod.parameters().add(setPara);
        var setBody       = ast.newBlock();
        var setAssignment = ast.newAssignment();
        setAssignment.setLeftHandSide(ast.newSimpleName(fieldName));
        setAssignment.setRightHandSide(ast.newSimpleName("value"));
        setBody.statements().add(ast.newExpressionStatement(setAssignment));
        setMethod.setBody(setBody);
        entityClass.bodyDeclarations().add(setMethod);
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
            if (memeber.type() == EntityMemberModel.EntityMemberType.DataField) {
                var dataField = (DataFieldModel) memeber;
                var fieldName = "_" + dataField.name();

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
        switchst.statements().add(makeThrowUnknownMember(ast, className));

        body.statements().add(switchst);
        method.setBody(body);
        return method;
    }

    private static MethodDeclaration makeEntityReadMemberMethod(AST ast, EntityModel model, String className) {
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
                var fieldName = "_" + dataField.name();

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
        switchst.statements().add(makeThrowUnknownMember(ast, className));

        body.statements().add(switchst);
        method.setBody(body);
        return method;
    }

    private static ThrowStatement makeThrowUnknownMember(AST ast, String className) {
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
