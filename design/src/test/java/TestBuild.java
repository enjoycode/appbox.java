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
        var testCode = "public class Emploee{}";
        //testCode += "public class Emploee";
        //testCode += "{\npublic String getName() {return \"rick\";}\n";
        //testCode += "public void setName(String value) {}\n}\n";
        var inputStream = new ByteArrayInputStream(testCode.getBytes(StandardCharsets.UTF_8));
        Function<IPath, InputStream> loadDelegate = (path) -> inputStream;

        var ls = new LanguageServer(loadDelegate);
        //var outPath = Path.forPosix("/media/psf/Home/Projects/AppBoxFuture/appbox.java/design/src/test/testdata/bin");
        var project = ls.createProject("testbuild", null, null);
        var file = project.getFile("Emploee.java");
        file.create(null, true, null);

        //EclipseCompiler/ JavaCompiler

        var config = new BuildConfiguration(project);
        var builder = new TestJavaBuilder(config);
        builder.build();
    }

}
