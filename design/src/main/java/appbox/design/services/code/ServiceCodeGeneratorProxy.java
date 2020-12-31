package appbox.design.services.code;

import org.eclipse.jdt.core.dom.*;

abstract class ServiceCodeGeneratorProxy extends ASTVisitor {

    protected final ServiceCodeGenerator generator;

    public ServiceCodeGeneratorProxy(ServiceCodeGenerator generator) {
        this.generator = generator;
    }

    //以下转发的需要与ServiceCodeGenerator重写的一致

    @Override
    public boolean visit(TypeDeclaration node) {
        return generator.visit(node);
    }

    @Override
    public boolean visit(SimpleType node) {
        return generator.visit(node);
    }

    @Override
    public boolean visit(MethodDeclaration node) {
        return generator.visit(node);
    }

    @Override
    public boolean visit(Assignment node) {
        return generator.visit(node);
    }

    @Override
    public boolean visit(QualifiedName node) {
        return generator.visit(node);
    }

    @Override
    public boolean visit(ClassInstanceCreation node) {
        return generator.visit(node);
    }

    @Override
    public boolean visit(MethodInvocation node) {
        return generator.visit(node);
    }


}
