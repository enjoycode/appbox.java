package appbox.design.services;

import appbox.data.EntityId;
import appbox.design.DesignHub;
import appbox.design.services.code.TypeHelper;
import appbox.design.tree.DataStoreNode;
import appbox.design.tree.DesignTree;
import appbox.design.tree.ModelNode;
import appbox.logging.Log;
import appbox.model.DataStoreModel;
import appbox.model.EntityModel;
import appbox.model.ModelType;
import appbox.model.entity.DataFieldModel;
import appbox.model.entity.EntityRefModel;
import appbox.model.entity.EntitySetModel;
import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.ls.core.internal.JDTUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 用于生成各模型的虚拟代码
 */
public class CodeGenService {

    /** 生成所有存储的虚拟代码 */
    public static String getStoresDummyCode(DesignTree designTree) {
        var sb = new StringBuilder();
        sb.append("import sys.*;");
        sb.append("public final class DataStore {\n");

        for (int i = 0; i < designTree.storeRootNode().nodes.count(); i++) {
            var node = (DataStoreNode) designTree.storeRootNode().nodes.get(i);
            sb.append("public static final ");
            if (node.model().kind() == DataStoreModel.DataStoreKind.Sql) {
                sb.append("SqlStore ");
                sb.append(node.model().name());
                sb.append(" = null;");
            } else {
                //TODO:
            }
        }

        sb.append("}");
        return sb.toString();
    }

    /** 根据实体模型生成虚拟代码 */
    public static String genEntityDummyCode(EntityModel model, String appName, DesignTree designTree) {
        var sb = new StringBuilder(150);
        sb.append("package ");
        sb.append(appName);
        sb.append(".entities;\n");

        sb.append("import sys.*;\n");

        var className = model.name();
        sb.append("public class ");
        sb.append(className);
        //根据存储选项继承不同基类
        if (model.sysStoreOptions() != null) {
            sb.append(" extends SysEntityBase");
        } else if (model.sqlStoreOptions() != null) {
            sb.append(" extends SqlEntityBase");
        }
        sb.append(" {\n");

        //ctor (仅具备主键的sql存储的实体)
        if (model.sqlStoreOptions() != null && model.sqlStoreOptions().hasPrimaryKeys()) {
            //无参构造用于扩展查询输出
            sb.append("\tpublic ");
            sb.append(className);
            sb.append("(){\n");
            for (int i = 0; i < model.sqlStoreOptions().primaryKeys().length; i++) {
                var pk = model.sqlStoreOptions().primaryKeys()[i];
                var pkField = (DataFieldModel) model.getMember(pk.memberId);
                sb.append("\t\t");
                sb.append(pkField.name());
                sb.append('=');
                sb.append(getPKDefaultValue(pkField));
                sb.append(";\n");
            }
            sb.append("\t}\n");

            //主键作为参数的构造
            sb.append("\tpublic ");
            sb.append(className);
            sb.append("(");
            var sb2 = new StringBuilder(20);
            for (int i = 0; i < model.sqlStoreOptions().primaryKeys().length; i++) {
                var pk = model.sqlStoreOptions().primaryKeys()[i];
                if (i != 0)
                    sb.append(',');
                var pkField = (DataFieldModel) model.getMember(pk.memberId);
                sb.append(getDataFieldTypeString(pkField));
                sb.append(" pk");
                sb.append(pkField.name());

                sb2.append("\t\t");
                sb2.append(pkField.name());
                sb2.append("=pk");
                sb2.append(pkField.name());
                sb2.append(";\n");
            }
            sb.append("){\n");
            sb.append(sb2);
            sb.append("\t}\n");
        }

        //fields
        for (var memberModel : model.getMembers()) {
            switch (memberModel.type()) {
                case DataField:
                    genDataFieldMember((DataFieldModel) memberModel, sb);
                    break;
                case EntityRef:
                    genEntityRefMember((EntityRefModel) memberModel, sb, designTree);
                    break;
                case EntitySet:
                    genEntitySetMember((EntitySetModel) memberModel, sb, designTree);
                    break;
            }
        }

        sb.append("}");
        return sb.toString();
    }

    private static void genDataFieldMember(DataFieldModel field, StringBuilder sb) {
        sb.append("\tpublic ");
        if (field.isPrimaryKey()) { //主键不允许修改
            sb.append("final ");
        }
        sb.append(getDataFieldTypeString(field));
        sb.append(' ');
        sb.append(field.name());
        sb.append(";\n");
    }

    private static void genEntityRefMember(EntityRefModel entityRef, StringBuilder sb, DesignTree tree) {
        sb.append("\tpublic ");
        if (entityRef.isAggregationRef()) {
            var baseEntityTypeName = "sys.EntityBase";
            if (entityRef.owner.sqlStoreOptions() != null)
                baseEntityTypeName = "sys.SqlEntityBase";
            else if (entityRef.owner.sysStoreOptions() != null)
                baseEntityTypeName = "sys.SysEntityBase";
            sb.append(baseEntityTypeName);
        } else {
            var refModelNode = tree.findModelNode(ModelType.Entity, entityRef.getRefModelIds().get(0));
            var refEntityTypeName = String.format("%s.entities.%s",
                    refModelNode.appNode.model.name(), refModelNode.model().name());
            sb.append(refEntityTypeName);
        }
        sb.append(' ');
        sb.append(entityRef.name());
        sb.append(";\n");
    }

    private static void genEntitySetMember(EntitySetModel entitySet, StringBuilder sb, DesignTree tree) {
        sb.append("\tpublic final "); //暂final
        sb.append("java.util.List<");
        var refModelNode = tree.findModelNode(ModelType.Entity, entitySet.refModelId());
        var refEntityTypeName = String.format("%s.entities.%s",
                refModelNode.appNode.model.name(), refModelNode.model().name());
        sb.append(refEntityTypeName);
        sb.append("> ");
        sb.append(entitySet.name());
        sb.append(";\n");
    }

    public static String getDataFieldTypeString(DataFieldModel field) {
        switch (field.dataType()) {
            case EntityId:
                return EntityId.class.getName();
            case String:
                return "String";
            case DateTime:
                return LocalDateTime.class.getName();
            case Short:
                return field.allowNull() ? "Short" : "short";
            case Int:
            case Enum:
                return field.allowNull() ? "Integer" : "int";
            case Long:
                return field.allowNull() ? "Long" : "long";
            case Decimal:
                return BigDecimal.class.getName();
            case Bool:
                return field.allowNull() ? "Boolean" : "boolean";
            case Guid:
                return UUID.class.getName();
            case Byte:
                return field.allowNull() ? "Byte" : "byte";
            case Binary:
                return "byte[]";
            case Float:
                return field.allowNull() ? "Float" : "float";
            case Double:
                return field.allowNull() ? "Double" : "double";
            default:
                return "Object";
        }
    }

    private static String getPKDefaultValue(DataFieldModel field) {
        switch (field.dataType()) {
            case String:
                return "\"\"";
            case Byte:
                return "(byte)0";
            case Short:
                return "(short)0";
            case Int:
            case Enum:
            case Long:
            case Float:
            case Double:
                return "0";
            case Bool:
                return "false";
            default:
                return "null";
        }
    }

    /** 生成服务模型的虚拟代理类 */
    public static String genServiceProxyCode(DesignHub hub, ModelNode serviceNode) {
        var serviceName = serviceNode.model().name();
        var sb          = new StringBuilder(200);

        sb.append("package ");
        sb.append(serviceNode.appNode.model.name());
        sb.append(".services;\n");

        //获取服务实现文件
        var file = hub.typeSystem.findFileForServiceModel(serviceNode);
        var unit = JDTUtils.resolveCompilationUnit(file);

        var astParser = ASTParser.newParser(AST.JLS15);
        astParser.setSource(unit);
        astParser.setIgnoreMethodBodies(true);
        astParser.setStatementsRecovery(true);
        var astNode = (CompilationUnit) astParser.createAST(null);

        for (var ip : astNode.imports()) {
            sb.append(ip.toString());
            sb.append('\n');
        }

        sb.append("public final class ");
        sb.append(serviceName);
        sb.append(" {\n");

        var serviceType = (TypeDeclaration) astNode.types().get(0);
        var methods     = serviceType.getMethods();
        for (var method : methods) {
            if (TypeHelper.isServiceMethod(method)) {
                sb.append("@sys.MethodInterceptor(name=\"InvokeService\")\n");

                method.modifiers().add(astNode.getAST().newModifier(Modifier.ModifierKeyword.STATIC_KEYWORD));
                var rst = astNode.getAST().newReturnStatement();
                rst.setExpression(astNode.getAST().newNullLiteral());
                method.getBody().statements().add(rst);
                sb.append(method.toString());
            }
        }

        sb.append("}");

        return sb.toString();
    }

    public static String genServiceDeclareCode(DesignHub hub, ModelNode serviceNode) {
        var serviceName = serviceNode.model().name();

        //获取服务实现文件
        var file = hub.typeSystem.findFileForServiceModel(serviceNode);
        var unit = JDTUtils.resolveCompilationUnit(file);

        var astParser = ASTParser.newParser(AST.JLS15);
        astParser.setSource(unit);
        astParser.setResolveBindings(true); //需要解析类型
        astParser.setIgnoreMethodBodies(true);
        astParser.setStatementsRecovery(true);
        var astNode = (CompilationUnit) astParser.createAST(null);

        var sb = new StringBuilder(200);
        sb.append("declare namespace ");
        sb.append(serviceNode.appNode.model.name());
        sb.append(".Services.");
        sb.append(serviceName);
        sb.append('{');

        var serviceType = (TypeDeclaration) astNode.types().get(0);
        var methods     = serviceType.getMethods();
        for (var method : methods) {
            if (TypeHelper.isServiceMethod(method)) {
                sb.append("function ");
                sb.append(method.getName().getIdentifier());
                sb.append('(');
                //处理参数列表
                boolean sep = false;
                for (var para : method.parameters()) {
                    var p = (SingleVariableDeclaration) para;
                    if (sep)
                        sb.append(',');
                    else
                        sep = true;
                    sb.append(p.getName().getIdentifier());
                    sb.append(':');
                    sb.append(toScriptType(p.getType()));
                }
                sb.append("):Promise<");
                //处理返回类型，皆为Promise<?>类型
                var returnType = (ParameterizedType) method.getReturnType2();
                sb.append(toScriptType((Type) returnType.typeArguments().get(0)));
                sb.append(">;");
            }
        }

        sb.append('}');
        return sb.toString();
    }

    /** 转为类型为前端typescript的类型 */
    private static String toScriptType(Type type) {
        //TODO:集合类型处理
        if (type.isPrimitiveType()) {
            var primitiveType = (PrimitiveType) type;
            var typeCode      = primitiveType.getPrimitiveTypeCode();
            if (typeCode == PrimitiveType.BOOLEAN) {
                return "boolean";
            } else if (typeCode == PrimitiveType.VOID) {
                return "void";
            } else {
                return "number";
            }
        } else if (type.isSimpleType()) {
            var          simpleType = (SimpleType) type;
            var          typeName   = simpleType.getName().getFullyQualifiedName();
            ITypeBinding entityType;
            if (typeName.equals("String")) {
                return "string";
            } else if ((entityType = TypeHelper.getEntityType(simpleType)) != null) {
                return String.format("%s.Entities.%s",
                        entityType.getPackage().getNameComponents()[0],
                        entityType.getName());
            } else {
                return "any";
            }
        }
        return "any";
    }

}
