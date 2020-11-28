package appbox.design.services.code;

import appbox.design.DesignHub;
import appbox.model.ServiceModel;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.SimpleType;
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

    public ServiceCodeGenerator(DesignHub hub, String appName,
                                ServiceModel serviceModel, ASTRewrite astRewrite) {
        this.hub          = hub;
        this.appName      = appName;
        this.serviceModel = serviceModel;
        this.astRewrite   = astRewrite;
    }

    @Override
    public boolean visit(SimpleType node) {
        if (node.getName().isSimpleName() && node.getName().getFullyQualifiedName().equals("String")) {
            var newType = astRewrite.getAST()
                    .newSimpleType(astRewrite.getAST().newName("Object"));
            astRewrite.replace(node, newType, null);
        }
        return super.visit(node);
    }

    @Override
    public boolean visit(MethodDeclaration node) {
        //测试转换返回类型
        //var listRewrite = astRewrite.getListRewrite(node.getBody(), Block.STATEMENTS_PROPERTY);
        //
        //var newInvocation = astRewrite.getAST().newMethodInvocation();
        //newInvocation.setName(astRewrite.getAST().newSimpleName("add"));
        //var newStatement = astRewrite.getAST().newExpressionStatement(newInvocation);
        //
        //listRewrite.insertFirst(newInvocation, null);

        //var newReturnType = astRewrite.getAST()
        //        .newSimpleType(astRewrite.getAST().newName("Object"));
        //node.setReturnType2(newReturnType);

        return super.visit(node);
    }

}
