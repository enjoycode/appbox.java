import appbox.design.idea.IdeaApplicationEnvironment;
import appbox.design.idea.IdeaProjectEnvironment;
import com.intellij.codeInsight.completion.*;
import com.intellij.lang.java.JavaLanguage;
import com.intellij.openapi.util.Disposer;
import com.intellij.pom.java.LanguageLevel;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.jar.JarFile;
import java.util.jar.JarEntry;

import static org.junit.jupiter.api.Assertions.*;

public class TestIdea {

    @Test
    public void testIdea() {
        //LanguageParserDefinitions.INSTANCE.addExplicitExtension(JavaLanguage.INSTANCE, TestFileViewProvider.parserDefinition);
        var temp = LanguageLevel.HIGHEST; //new CoreLanguageLevelProjectExtension();

        //var app = new ApplicationImpl(true, false, true, true);
        var app = new TestApplication();

        var project = new TestProject();

        var module = new TestModule(project, "Module1");

        var root = new TestVirtualFile("", System.currentTimeMillis());
        var file1 = new TestVirtualFile("A.java",
                "class A {}", System.currentTimeMillis());
        root.addChild(file1);
        project.fileManager.addToClasspath(root);

        //测试VirtualFile -> PsiFile
        var fileViewProvider = new TestFileViewProvider(project.psiManager, file1);
        var psiFile          = fileViewProvider.getPsi(JavaLanguage.INSTANCE);
        var childs           = psiFile.getChildren();

        //FileViewProviderFactory factory = LanguageFileViewProviders.INSTANCE.forLanguage(JavaLanguage.INSTANCE);
        //var vfp = new SingleRootFileViewProvider(project.psiManager, file1);


        //var psiFile = project.psiManager.findFile(file1);

        var cs = project.fileManager.findClass("A", GlobalSearchScope.projectScope(project));

        //var psiManager = new PsiManagerImpl(project);

        //PsiJavaParserFacadeImpl psiParserFacade = new PsiJavaParserFacadeImpl(project);
        //JavaPsiFacadeImpl psiFacade  = new JavaPsiFacadeImpl(project);
        //PsiManager        psiManager = PsiManager.getInstance(project);
        //ModuleManager.getInstance(project).newModule(moduleFile, EmptyModuleType.EMPTY_MODULE);

        //var m = psiFacade.findModule("TestModule",
        //        GlobalSearchScope.EMPTY_SCOPE
        //        /*GlobalSearchScope.allScope(project)*/);

    }

    @Test
    public void testIdea2() {
        var lastDisposable = Disposer.newDisposable();
        var app            = new IdeaApplicationEnvironment(lastDisposable);
        var prj            = new IdeaProjectEnvironment(lastDisposable, app);

        prj.addJarToClassPath(new File("/media/psf/Home/Projects/intellij-community/java/mockJDK-11/jre/lib/rt.jar"));
        var root = new TestVirtualFile("", System.currentTimeMillis());
        var file1 = new TestVirtualFile("A.java",
                "package s.impl; class A {A b; void say(){b.toString();}}", System.currentTimeMillis());
        prj.addSourcesToClasspath(root);

        var psiFile = PsiManager.getInstance(prj.getProject()).findFile(file1);
        //var ref     = psiFile.findReferenceAt(43); //25, 41
        //var res     = ref.resolve();
        //var res = ((PsiJavaCodeReferenceElement) ref).advancedResolve(true);
        //var vars = ref.getVariants(); //代码必须设package

        //for (int i = 0; i < 10; i++) {
        var cusor       = 43;
        var position    = psiFile.findElementAt(cusor);
        var contributor = new JavaCompletionContributor();

        var cParameters = new CompletionParameters(position, psiFile, CompletionType.BASIC,
                cusor, 0, new TestEditor(), new CompletionProcess() {
            @Override
            public boolean isAutopopupCompletion() {
                return false;
            }
        });

        //测试直接使用JavaCompletionContributor
        var cResultSet = new TestCompletionResultSet(
                completionResult -> System.out.println(completionResult),
                PrefixMatcher.ALWAYS_TRUE, contributor, cParameters, null, null);

        contributor.fillCompletionVariants(cParameters, cResultSet);
        //}
    }

    @Test
    public void testJarFile() throws IOException {
        var                   jarFile = new JarFile("/media/psf/Home/Projects/intellij-community/java/mockJDK-11/jre/lib/rt.jar");
        JarEntry              entry;
        Enumeration<JarEntry> e       = jarFile.entries();
        while (e.hasMoreElements()) {
            entry = e.nextElement();
            if (entry != null && !entry.isDirectory() && entry.getName().endsWith(".class")) {
                String name = entryPathToClassName(entry.getName());
                //classpathElements.put(name, new ClasspathElement(jarFile, entry));
            }
        }
    }

    private String entryPathToClassName(String entryPath) {
        if (!entryPath.endsWith(".class")) {
            throw new IllegalStateException();
        }
        String className = entryPath.substring(0, entryPath.length() - ".class".length());
        className = className.replace('/', '.');
        className = className.replace('$', '.');
        return className;
    }
}
