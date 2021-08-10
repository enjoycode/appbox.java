package appbox.design.lang.java.code;

import org.eclipse.jdt.core.dom.MethodInvocation;

public interface IMethodInterceptor {

    boolean visit(MethodInvocation node, ServiceCodeGenerator generator);

}
