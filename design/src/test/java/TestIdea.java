import com.intellij.codeInsight.CodeInsightSettings;
import com.intellij.codeInsight.JavaProjectCodeInsightSettings;
import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.guess.GuessManager;
import com.intellij.codeInsight.guess.impl.GuessManagerImpl;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInspection.SuppressManager;
import com.intellij.codeInspection.SuppressManagerImpl;
import com.intellij.core.*;
import com.intellij.ide.highlighter.JavaClassFileType;
import com.intellij.lang.LanguageParserDefinitions;
import com.intellij.lang.java.JavaLanguage;
import com.intellij.lang.jvm.JvmClass;
import com.intellij.lang.jvm.facade.JvmElementProvider;
import com.intellij.model.psi.PsiSymbolService;
import com.intellij.model.psi.impl.PsiSymbolServiceImpl;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.impl.ApplicationImpl;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.extensions.Extensions;
import com.intellij.openapi.fileTypes.BinaryFileDecompiler;
import com.intellij.openapi.fileTypes.BinaryFileTypeDecompilers;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.projectRoots.DefaultJdkConfigurator;
import com.intellij.openapi.projectRoots.JavaSdk;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.projectRoots.SdkType;
import com.intellij.openapi.projectRoots.impl.JavaAwareProjectJdkTableImpl;
import com.intellij.openapi.projectRoots.impl.JavaSdkImpl;
import com.intellij.openapi.roots.FileIndexFacade;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.patterns.ElementPattern;
import com.intellij.pom.java.LanguageLevel;
import com.intellij.psi.*;
import com.intellij.psi.augment.PsiAugmentProvider;
import com.intellij.psi.compiled.ClassFileDecompilers;
import com.intellij.psi.impl.*;
import com.intellij.psi.impl.compiled.ClassFileDecompiler;
import com.intellij.psi.impl.file.impl.JavaFileManagerImpl;
import com.intellij.psi.impl.file.impl.ResolveScopeManagerImpl;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.ProjectScopeBuilder;
import com.intellij.psi.util.JavaClassSupers;
import com.intellij.util.Consumer;
import org.jetbrains.java.decompiler.IdeaDecompiler;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.List;
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
        var _lastDisposable = Disposer.newDisposable();
        var app             = new JavaCoreApplicationEnvironment(_lastDisposable, true);
        //var rootArea = Extensions.getRootArea();
        JavaCoreApplicationEnvironment.registerApplicationExtensionPoint(PsiAugmentProvider.EP_NAME, RecordAugmentProvider.class);
        JavaCoreApplicationEnvironment.registerApplicationExtensionPoint(JavaModuleSystem.EP_NAME, JavaPlatformModuleSystem.class);
        JavaCoreApplicationEnvironment.registerApplicationExtensionPoint(SdkType.EP_NAME, JavaSdkImpl.class);
        JavaCoreApplicationEnvironment.registerApplicationExtensionPoint(OrderRootType.EP_NAME, OrderRootType.class);

        app.registerApplicationService(JavaClassSupers.class, new JavaClassSupersImpl()); //getVariants需要
        app.registerApplicationService(PsiSymbolService.class, new PsiSymbolServiceImpl());
        app.registerApplicationService(CompletionService.class, new BaseCompletionService());
        app.registerApplicationService(CodeInsightSettings.class, new CodeInsightSettings());
        app.registerApplicationService(SuppressManager.class, new TestSuppressManager()/*new SuppressManagerImpl()*/); //code completion

        //替换ClassFileViewProviderFactory测试
        var old = FileTypeFileViewProviders.INSTANCE.findSingle(JavaClassFileType.INSTANCE);
        FileTypeFileViewProviders.INSTANCE.removeExplicitExtension(JavaClassFileType.INSTANCE, old);
        FileTypeFileViewProviders.INSTANCE.addExplicitExtension(JavaClassFileType.INSTANCE, new TestClassFileViewProviderFactory());

        var epname1 = ExtensionPointName.create("com.intellij.filetype.decompiler");
        JavaCoreApplicationEnvironment.registerApplicationExtensionPoint(epname1, BinaryFileTypeDecompilers.class);

        var CFD_EP_NAME = ExtensionPointName.create("com.intellij.psi.classFileDecompiler");
        JavaCoreApplicationEnvironment.registerApplicationExtensionPoint(CFD_EP_NAME, ClassFileDecompilers.Light.class);
        //JavaCoreApplicationEnvironment.registerApplicationExtensionPoint(CFD_EP_NAME, ClassFileDecompilers.Full.class);
        //app.addExtension(CFD_EP_NAME, new TestClsDecompilerImpl());

        var ep = Extensions.getRootArea().getExtensionPoint(ClassFileDecompilers.getInstance().EP_NAME);
        app.addExtension(ClassFileDecompilers.getInstance().EP_NAME, new IdeaDecompiler());
        var d = app.getApplication().getService(ClassFileDecompilers.class);
        d.EP_NAME.getExtensions();

        var prj = new JavaCoreProjectEnvironment(_lastDisposable, app);
        prj.registerProjectComponent(ProjectRootManager.class, new TestProjectRootManager()); //completion
        //prj.registerProjectComponent(GuessManager.class, new GuessManagerImpl(prj.getProject())); //completion
        prj.getProject().registerService(GuessManager.class, new GuessManagerImpl(prj.getProject())); //completion
        prj.getProject().registerService(JavaProjectCodeInsightSettings.class, new JavaProjectCodeInsightSettings()); //completion

        prj.registerProjectExtensionPoint(PsiElementFinder.EP_NAME, PsiElementFinderImpl.class);
        prj.addProjectExtension(PsiElementFinder.EP_NAME, new PsiElementFinderImpl(prj.getProject()));
        //prj.registerProjectExtensionPoint(JvmElementProvider.EP_NAME, JvmElementProvider.class);
        //prj.addProjectExtension(JvmElementProvider.EP_NAME, new JvmElementProvider() {
        //    @Override
        //    public List<? extends JvmClass> getClasses(String s, GlobalSearchScope globalSearchScope) {
        //        return null;
        //    }
        //});
        //替换前需要取消注册旧的
        //替换ResolveScopeManager测试
        //prj.getProject().registerService(ResolveScopeManager.class, new ResolveScopeManagerImpl(prj.getProject()));
        //替换ProjectScopeBuilder测试
        var fileIndexFacade = prj.getProject().getService(FileIndexFacade.class);
        var oldpsb1         = prj.getProject().getService(ProjectScopeBuilder.class);
        var oldProjectScopeBuilder =
                prj.getProject().getPicoContainer().unregisterComponent(ProjectScopeBuilder.class.getName());
        prj.getProject().registerService(ProjectScopeBuilder.class, new TestProjectScopeBuilder(prj.getProject(), fileIndexFacade));

        //测试JDK
        ////JavaSdk jdk = JavaSdk.getInstance();
        //var jdk = new JavaSdkImpl();
        //var sdk = jdk.createJdk("jdk13", "/usr/lib/jvm/java-13-openjdk-amd64");
        ////JavaAwareProjectJdkTableImpl jdks = new JavaAwareProjectJdkTableImpl();

        prj.addJarToClassPath(new File("/media/psf/Home/Projects/intellij-community/java/mockJDK-11/jre/lib/rt.jar"));
        var root = new TestVirtualFile("", System.currentTimeMillis());
        var file1 = new TestVirtualFile("A.java",
                "package s.impl; class A {A b; void say(){b.toString();}}", System.currentTimeMillis());
        prj.addSourcesToClasspath(root);

        var psiFile = PsiManager.getInstance(prj.getProject()).findFile(file1);
        //var ref     = psiFile.findReferenceAt(43); //25, 41
        //var res     = ref.resolve();
        ////var res = ((PsiJavaCodeReferenceElement) ref).advancedResolve(true);
        ////代码必须设package
        //var vars = ref.getVariants();

        var cusor = 43;
        var position    = psiFile.findElementAt(cusor);
        var contributor = new JavaCompletionContributor();

        var cParameters = new CompletionParameters(position, psiFile, CompletionType.BASIC,
                cusor, 0, new TestEditor(), new CompletionProcess() {
            @Override
            public boolean isAutopopupCompletion() {
                return false;
            }
        });

        //测试使用CompletionService
        //var cpService = app.getApplication().getService(CompletionService.class);
        //cpService.performCompletion(cParameters, new Consumer<CompletionResult>() {
        //    @Override
        //    public void consume(CompletionResult completionResult) {
        //        System.out.println(completionResult);
        //    }
        //});

        //测试直接使用JavaCompletionContributor
        var cResultSet = new TestCompletionResultSet(
                completionResult -> System.out.println(completionResult),
                PrefixMatcher.ALWAYS_TRUE, contributor, cParameters, null, null);

        contributor.fillCompletionVariants(cParameters, cResultSet);
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
