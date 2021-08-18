import appbox.design.lang.java.jdt.JavaBuilderWrapper;
import appbox.design.lang.java.jdt.ModelProject;
import appbox.design.lang.java.jdt.ProgressMonitor;
import appbox.design.lang.java.lsp.ReferencesHandler;
import appbox.model.ModelType;
import org.eclipse.core.internal.resources.BuildConfiguration;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IJavaModelMarker;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.internal.core.JavaModelManager;
import org.eclipse.jdt.internal.core.SourceMethod;
import org.eclipse.jdt.ls.core.internal.JDTUtils;
import org.eclipse.lsp4j.Location;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

public class TestJDT {

    private IProject createTestProject() throws Exception {
        var hub = TestHelper.makeDesignHub(null, false);
        var ls  = hub.typeSystem.javaLanguageServer;
        return ls.createProject(ModelProject.ModelProjectType.Test, "testProject", null);
    }

    @Test
    public void testCreateProject() throws Exception {
        var project = createTestProject();
        assertTrue(project.exists());
        assertTrue(project.isOpen());
    }

    @Test
    public void testDeleteProject() throws Exception {
        var project = createTestProject();
        var file    = project.getFile("TestFile.txt");
        file.create(null, true, null);
        project.delete(true, null);
        assertFalse(file.exists());
        assertFalse(project.exists());
    }

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
        var project = ls.createProject(ModelProject.ModelProjectType.Test, "testbuild", null);
        var file    = project.getFile("Emploee.java");
        file.create(null, true, null);

        var config  = new BuildConfiguration(project);
        var builder = new JavaBuilderWrapper(config);
        builder.build();
    }

    @Test
    public void testBuildWithError() throws Exception {
        final var testCode = "public class Emploee {aa";
        //注意编译器会重复调用加载文件内容
        Function<IPath, InputStream> loadDelegate =
                (path) -> new ByteArrayInputStream(testCode.getBytes(StandardCharsets.UTF_8));

        var hub     = TestHelper.makeDesignHub(loadDelegate, false);
        var ls      = hub.typeSystem.javaLanguageServer;
        var project = ls.createProject(ModelProject.ModelProjectType.Test, "testbuild", null);
        var file    = project.getFile("Emploee.java");
        file.create(null, true, null);

        var config  = new BuildConfiguration(project);
        var builder = new JavaBuilderWrapper(config);
        builder.build();

        //参考WorkspaceDiagnosticsHandler
        var markers = project.findMarkers(
                IJavaModelMarker.JAVA_MODEL_PROBLEM_MARKER/*null*/, true, IResource.DEPTH_INFINITE);
        assertNotNull(markers);
        assertTrue(markers.length > 0);
        for (var mark : markers) {
            System.out.println(mark);
        }
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
        var project = ls.createProject(ModelProject.ModelProjectType.Test, "testbuild", null);
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
        ReferencesHandler.search(symbol, locations, new ProgressMonitor());
        for (var loc : locations) {
            //hub.typeSystem.javaLanguageServer.findModelNodeByModelFile(loc.)
            System.out.println(loc.getUri() + " " + loc.getRange());
        }

        assertTrue(locations.size() > 4);
    }

    @Test
    public void testIndex() throws Exception {
        final var hub       = TestHelper.makeDesignHub(null, false);
        final var ls        = hub.typeSystem.javaLanguageServer;
        final var project   = ls.createProject(ModelProject.ModelProjectType.Test, "testIndex", null);
        var       src       = "class Order {String name;}";
        var       srcStream = new ByteArrayInputStream(src.getBytes(StandardCharsets.UTF_8));
        var       file      = project.getFile("Order.java");
        file.create(srcStream, true, null);

        var indexManager  = JavaModelManager.getIndexManager();
        var cu = JDTUtils.resolveCompilationUnit(file);
        //cu.becomeWorkingCopy(null);
        var elementParser = indexManager.getSourceElementParser(cu.getJavaProject(), null);
        indexManager.addSource(file, file.getProject().getFullPath(), elementParser);
        Thread.sleep(5000); //wait for build index

        //测试更新
        var newSrc = "class Order2 {String Name2;}";
        Files.write(file.getLocation().toFile().toPath(), newSrc.getBytes(StandardCharsets.UTF_8));
        cu = JDTUtils.resolveCompilationUnit(file);
        elementParser = indexManager.getSourceElementParser(cu.getJavaProject(), null);
        indexManager.addSource(file, file.getProject().getFullPath(), elementParser);

        Thread.sleep(5000); //wait for build index
        //cu.discardWorkingCopy();

        //project.delete(true, null);
        //Thread.sleep(5000);
        System.out.println("Done.");
    }

}
