package appbox.design.services.code;

import appbox.data.EntityId;
import appbox.data.SqlEntity;
import appbox.data.SysEntity;
import appbox.design.tree.ModelNode;
import appbox.exceptions.UnknownEntityMember;
import appbox.model.EntityModel;
import appbox.model.entity.DataFieldModel;
import appbox.model.entity.EntityMemberModel;
import appbox.serialization.IEntityMemberReader;
import appbox.serialization.IEntityMemberWriter;
import appbox.utils.StringUtil;
import org.eclipse.jdt.core.dom.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public final class EntityCodeGenerator {
    private EntityCodeGenerator() {}

    /** 生成实体的运行时代码 */
    static TypeDeclaration makeEntityRuntimeCode(AST ast, ModelNode modelNode) {
        var model           = (EntityModel) modelNode.model();
        var entityClass     = ast.newTypeDeclaration();
        var entityClassName = makeEntityClassName(modelNode);
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
                var dataField = (DataFieldModel) member;
                var fieldName = "_" + StringUtil.firstLowerCase(member.name());

                var vdf = ast.newVariableDeclarationFragment();
                vdf.setName(ast.newSimpleName(fieldName));
                var field = ast.newFieldDeclaration(vdf);
                field.setType(makeDataFieldType(ast, dataField));
                field.modifiers().add(ast.newModifier(Modifier.ModifierKeyword.PRIVATE_KEYWORD));
                entityClass.bodyDeclarations().add(field);

                var getMethod = ast.newMethodDeclaration();
                getMethod.setName(ast.newSimpleName("get" + StringUtil.firstUpperCase(member.name())));
                getMethod.modifiers().add(ast.newModifier(Modifier.ModifierKeyword.PUBLIC_KEYWORD));
                getMethod.setReturnType2(makeDataFieldType(ast, dataField));
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
                setPara.setType(makeDataFieldType(ast, dataField));
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
        entityClass.bodyDeclarations().add(makeEntityWriteMemberMethod(ast, model, entityClassName));
        entityClass.bodyDeclarations().add(makeEntityReadMemberMethod(ast, model, entityClassName));

        return entityClass;
    }

    /** 生成运行时实体名称 eg: Sys_Employee */
    static String makeEntityClassName(ModelNode modelNode) {
        return String.format("%s_%s",
                StringUtil.firstUpperCase(modelNode.appNode.model.name()), modelNode.model().name());
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
