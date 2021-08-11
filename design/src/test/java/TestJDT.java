import appbox.design.lang.java.jdt.JavaBuilderWrapper;
import appbox.design.lang.java.jdt.ProgressMonitor;
import appbox.design.lang.java.lsp.ReferencesHandler;
import appbox.design.utils.PathUtil;
import appbox.design.utils.ReflectUtil;
import appbox.model.ModelType;
import org.eclipse.core.internal.resources.BuildConfiguration;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.internal.core.JavaModelManager;
import org.eclipse.jdt.internal.core.SourceMethod;
import org.eclipse.jdt.internal.core.search.indexing.IndexManager;
import org.eclipse.jdt.ls.core.internal.JDTUtils;
import org.eclipse.lsp4j.Location;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

public class TestJDT {

    @Test
    public void testBuild() throws Exception {
        var testCode = "public class Emploee {\n";
        testCode += "public String getName() {\n";
        testCode += " var name = \"rick\";\n";
        testCode += " return name;\n";
        testCode += "}\n";
        testCode += "}";
        //注意编译器会重复调用加载文件内容
        String finalTestCode = testCode;
        Function<IPath, InputStream> loadDelegate =
                (path) -> new ByteArrayInputStream(finalTestCode.getBytes(StandardCharsets.UTF_8));

        var hub     = TestHelper.makeDesignHub(loadDelegate, false);
        var ls      = hub.typeSystem.javaLanguageServer;
        var project = ls.createProject("testbuild", null);
        var file    = project.getFile("Emploee.java");
        file.create(null, true, null);

        var config  = new BuildConfiguration(project);
        var builder = new JavaBuilderWrapper(config);
        builder.build();
    }

    @Test
    public void testFindAtLineAndColumn() throws Exception {
        var testCode = "public class Emploee {\n";
        testCode += "public String getName(int age) {}\n";
        testCode += "}";
        //注意编译器会重复调用加载文件内容
        String finalTestCode = testCode;
        Function<IPath, InputStream> loadDelegate =
                (path) -> new ByteArrayInputStream(finalTestCode.getBytes(StandardCharsets.UTF_8));

        var hub     = TestHelper.makeDesignHub(loadDelegate, false);
        var ls      = hub.typeSystem.javaLanguageServer;
        var project = ls.createProject("testbuild", null);
        var file    = project.getFile("Emploee.java");
        file.create(null, true, null);

        var cu             = JDTUtils.resolveCompilationUnit(file);
        var buffer         = cu.getBuffer();
        var chars          = buffer.getCharacters();
        var element        = (SourceMethod) cu.getElementAt(38);
        var paraTypeString = Signature.toString(element.getParameters()[0].getTypeSignature());
        assertNotNull(element);
    }

    @Test
    public void testGetEntitySymbol() throws Exception {
        final var hub = TestHelper.makeDesignHub(null, true);
        //准备测试模型
        final var dataStoreModel = TestHelper.makeSqlDataStoreModel();
        final var entityModel    = TestHelper.makeEmployeeModel(dataStoreModel);
        TestHelper.injectAndLoadTree(dataStoreModel, List.of(entityModel));

        final var symbolFinder = hub.typeSystem.javaLanguageServer.symbolFinder;

        var symbol = symbolFinder.getModelSymbol(ModelType.Entity, "sys", "Employee");
        assertNotNull(symbol);

        var name = symbolFinder.getEntityMemberSymbol("sys", "Employee", "Name");
        assertNotNull(name);

        var notExists = symbolFinder.getEntityMemberSymbol("sys", "Employee", "NotExists");
        assertNull(notExists);
    }

    @Test
    public void testSearchRefernces() throws Exception {
        final var hub = TestHelper.makeDesignHub(TestHelper::loadTestServiceCode, true);
        //准备测试模型
        final var dataStoreModel   = TestHelper.makeSqlDataStoreModel();
        final var entityModel      = TestHelper.makeEmployeeModel(dataStoreModel);
        final var testServiceModel = TestHelper.makeServiceModel(10, "TestService");
        TestHelper.injectAndLoadTree(dataStoreModel, List.of(entityModel, testServiceModel));

        final var symbol = hub.typeSystem.javaLanguageServer.symbolFinder
                .getModelSymbol(ModelType.Entity, "sys", "Employee");
        final List<Location> locations = new ArrayList<>();
        var javaCore = new JavaCore();
        JavaModelManager.getIndexManager().reset();
        ReflectUtil.setField(IndexManager.class, "javaPluginLocation", JavaModelManager.getIndexManager(), PathUtil.PLUGIN);
        ReferencesHandler.search(symbol, locations, new ProgressMonitor());
        for (var loc : locations) {
            //hub.typeSystem.javaLanguageServer.findModelNodeByModelFile(loc.)
            System.out.println(loc.getUri() + " " + loc.getRange());
        }

    }

}
