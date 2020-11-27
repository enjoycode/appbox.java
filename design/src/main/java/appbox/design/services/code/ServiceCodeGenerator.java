package appbox.design.services.code;

import appbox.design.DesignHub;
import appbox.model.ServiceModel;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.internal.corext.dom.ASTFlattener;

import java.util.ArrayList;
import java.util.List;

/** 用于生成运行时的服务代码 */
public final class ServiceCodeGenerator extends ASTFlattener {

    /** 公开的服务方法集合 */
    private final List<MethodDeclaration> publicMethods = new ArrayList<>();

    private final DesignHub    hub;
    private final String       appName;
    private final ServiceModel serviceModel;

    public ServiceCodeGenerator(DesignHub hub, String appName, ServiceModel serviceModel) {
        this.hub          = hub;
        this.appName      = appName;
        this.serviceModel = serviceModel;
    }

    @Override
    public boolean visit(MethodDeclaration node) {
        return super.visit(node);
    }

}
