package appbox.design.lang.java.code;

import org.eclipse.jdt.core.dom.ClassInstanceCreation;

public interface ICtorInterceptor {

    boolean visit(ClassInstanceCreation node, ServiceCodeGenerator generator);

}
