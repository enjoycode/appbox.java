package appbox.design.services.code;

import org.eclipse.jdt.core.dom.ClassInstanceCreation;

public interface ICtorInterceptor {

    boolean visit(ClassInstanceCreation node, ServiceCodeGenerator generator);

}
