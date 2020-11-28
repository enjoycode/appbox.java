import appbox.design.jdt.JavaBuilderWrapper;
import appbox.design.services.code.LanguageServer;
import org.eclipse.core.internal.resources.BuildConfiguration;
import org.eclipse.core.runtime.IPath;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

public class TestBuild {

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
        var project = ls.createProject("testbuild", null);
        var file = project.getFile("Emploee.java");
        file.create(null, true, null);

        var config = new BuildConfiguration(project);
        var builder = new JavaBuilderWrapper(config);
        builder.build();
    }

}
