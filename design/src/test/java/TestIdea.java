import com.intellij.core.*;
import com.intellij.lang.LanguageParserDefinitions;
import com.intellij.lang.java.JavaLanguage;
import com.intellij.lang.jvm.JvmClass;
import com.intellij.lang.jvm.facade.JvmElementProvider;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.impl.ApplicationImpl;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.extensions.Extensions;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.projectRoots.DefaultJdkConfigurator;
import com.intellij.openapi.projectRoots.JavaSdk;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.projectRoots.SdkType;
import com.intellij.openapi.projectRoots.impl.JavaAwareProjectJdkTableImpl;
import com.intellij.openapi.projectRoots.impl.JavaSdkImpl;
import com.intellij.openapi.roots.FileIndexFacade;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.util.Disposer;
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
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;

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

        //var CFD_EP_NAME = ExtensionPointName.create("com.intellij.psi.classFileDecompiler");
        //JavaCoreApplicationEnvironment.registerApplicationExtensionPoint(CFD_EP_NAME, ClassFileDecompilers.Full.class);
        //app.addExtension(CFD_EP_NAME, new ClsDecompilerImpl());

        var prj = new JavaCoreProjectEnvironment(_lastDisposable, app);
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
        var oldpsb1 = prj.getProject().getService(ProjectScopeBuilder.class);
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
                "class A {A b; void say(){b.toString();}}", System.currentTimeMillis());
        prj.addSourcesToClasspath(root);

        var psi = PsiManager.getInstance(prj.getProject()).findFile(file1);
        var ref = psi.findReferenceAt(9); //25
        var res = ref.resolve();
        //var res = ((PsiJavaCodeReferenceElement) ref).advancedResolve(true);
    }

}
