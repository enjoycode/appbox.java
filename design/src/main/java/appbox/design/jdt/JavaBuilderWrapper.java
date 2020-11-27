package appbox.design.jdt;

import appbox.design.utils.ReflectUtil;
import org.eclipse.core.internal.events.BuildContext;
import org.eclipse.core.internal.events.InternalBuilder;
import org.eclipse.core.resources.IBuildConfiguration;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.internal.core.builder.JavaBuilder;

public final class JavaBuilderWrapper extends JavaBuilder {

    public JavaBuilderWrapper(IBuildConfiguration configuration) {
        try {
            ReflectUtil.setField(InternalBuilder.class, "buildConfiguration", this, configuration);
            ReflectUtil.setField(InternalBuilder.class, "context", this, new BuildContext(configuration));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void build() throws CoreException {
        this.build(IncrementalProjectBuilder.FULL_BUILD, null, null);
    }

}
