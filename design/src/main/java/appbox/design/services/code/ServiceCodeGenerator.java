package appbox.design.services.code;

import appbox.design.DesignHub;
import appbox.model.ServiceModel;
import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.internal.corext.dom.GenericVisitor;

import java.util.ArrayList;
import java.util.List;

/** 用于生成运行时的服务代码 */
public final class ServiceCodeGenerator extends GenericVisitor {

    /** 公开的服务方法集合 */
    private final List<MethodDeclaration> publicMethods = new ArrayList<>();

    private final DesignHub    hub;
    private final String       appName;
    private final ServiceModel serviceModel;
    private final ASTRewrite   astRewrite;
    private final AST          ast;

    private TypeDeclaration _serviceTypeDeclaration;

    public ServiceCodeGenerator(DesignHub hub, String appName,
                                ServiceModel serviceModel, ASTRewrite astRewrite) {
        this.hub          = hub;
        this.appName      = appName;
        this.serviceModel = serviceModel;
        this.astRewrite   = astRewrite;
        this.ast          = astRewrite.getAST();
    }

    public void finish() {
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
        var typeList = ast.newSimpleType(ast.newName("java.util.List"));
        var type2 = ast.newParameterizedType(typeList);
        type2.typeArguments().add(ast.newSimpleType(ast.newName("appbox.runtime.InvokeArg")));
        para2.setType(type2);
        para2.setName(ast.newSimpleName("args"));
        invokeMethod.parameters().add(para2);

        var body = ast.newBlock();
        var returnStament = ast.newReturnStatement();
        returnStament.setExpression(ast.newNullLiteral());
        body.statements().add(returnStament);
        invokeMethod.setBody(body);

        var listRewrite =
                astRewrite.getListRewrite(_serviceTypeDeclaration, TypeDeclaration.BODY_DECLARATIONS_PROPERTY);
        listRewrite.insertLast(invokeMethod, null);
    }

    @Override
    public boolean visit(TypeDeclaration node) {
        _serviceTypeDeclaration = node;

        var serviceType = ast.newSimpleType(ast.newName("appbox.runtime.IService"));
        //astRewrite.set(node, TypeDeclaration.SUPERCLASS_TYPE_PROPERTY, serviceType, null );
        var listRewrite = astRewrite.getListRewrite(node, TypeDeclaration.SUPER_INTERFACE_TYPES_PROPERTY);
        listRewrite.insertFirst(serviceType, null);

        return true;
    }

    @Override
    public boolean visit(SimpleType node) {
        if (node.getName().isSimpleName() && node.getName().getFullyQualifiedName().equals("String")) {
            var newType = ast.newSimpleType(ast.newName("Object"));
            astRewrite.replace(node, newType, null);
        }
        return super.visit(node);
    }

    @Override
    public boolean visit(MethodDeclaration node) {
        return super.visit(node);
    }

}
