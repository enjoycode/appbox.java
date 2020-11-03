import appbox.design.services.code.LanguageServer;
import appbox.design.utils.ReflectUtil;
import org.eclipse.core.internal.events.BuildContext;
import org.eclipse.core.internal.events.InternalBuilder;
import org.eclipse.core.internal.resources.BuildConfiguration;
import org.eclipse.core.resources.IBuildConfiguration;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.internal.core.builder.JavaBuilder;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

public class TestBuild {

    public class TestJavaBuilder extends JavaBuilder {
        public TestJavaBuilder(IBuildConfiguration configuration) {
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

    @Test
    public void testBuild() throws Exception {
        var testCode = "public class Emploee {\n";
        testCode += "public String getName() {\n";
        testCode += " var name = \"rick\";\n";
        testCode += " return name;\n";
        testCode += "}\n";
        testCode +="}";
        //注意编译器会重复调用加载文件内容
        String                       finalTestCode = testCode;
        Function<IPath, InputStream> loadDelegate  =
                (path) -> new ByteArrayInputStream(finalTestCode.getBytes(StandardCharsets.UTF_8));

        var ls = new LanguageServer(loadDelegate);
        var project = ls.createProject("testbuild", null, null);
        var file = project.getFile("Emploee.java");
        file.create(null, true, null);

        var config = new BuildConfiguration(project);
        var builder = new TestJavaBuilder(config);
        builder.build();
    }

}
