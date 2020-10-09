package appbox.design.idea;

import com.intellij.codeInsight.JavaProjectCodeInsightSettings;
import com.intellij.codeInsight.guess.GuessManager;
import com.intellij.codeInsight.guess.impl.GuessManagerImpl;
import com.intellij.core.*;
import com.intellij.lang.injection.InjectedLanguageManager;
import com.intellij.lang.jvm.facade.JvmFacade;
import com.intellij.lang.jvm.facade.JvmFacadeImpl;
import com.intellij.mock.*;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.DumbUtil;
import com.intellij.openapi.roots.FileIndexFacade;
import com.intellij.openapi.roots.LanguageLevelProjectExtension;
import com.intellij.openapi.roots.PackageIndex;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;
import com.intellij.psi.codeStyle.JavaCodeStyleSettingsFacade;
import com.intellij.psi.controlFlow.ControlFlowFactory;
import com.intellij.psi.impl.*;
import com.intellij.psi.impl.file.PsiDirectoryFactory;
import com.intellij.psi.impl.file.PsiDirectoryFactoryImpl;
import com.intellij.psi.impl.file.impl.JavaFileManager;
import com.intellij.psi.impl.smartPointers.SmartPointerManagerImpl;
import com.intellij.psi.impl.source.resolve.JavaResolveCache;
import com.intellij.psi.impl.source.resolve.PsiResolveHelperImpl;
import com.intellij.psi.impl.source.resolve.ResolveCache;
import com.intellij.psi.search.ProjectScopeBuilder;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.psi.util.PsiModificationTracker;
import com.intellij.util.CachedValuesManagerImpl;
import com.intellij.util.messages.MessageBus;
import org.picocontainer.PicoContainer;

import java.io.File;

public final class IdeaProjectEnvironment {
    private final Disposable                 myParentDisposable;
    private final IdeaApplicationEnvironment myEnvironment;

    protected final FileIndexFacade myFileIndexFacade;
    protected final PsiManagerImpl  myPsiManager;
    protected final MockProject     myProject;
    protected final MessageBus      myMessageBus;

    private final JavaFileManager myFileManager;
    private final PackageIndex    myPackageIndex;

    public IdeaProjectEnvironment(Disposable parentDisposable, IdeaApplicationEnvironment applicationEnvironment) {
        myParentDisposable = parentDisposable;
        myEnvironment      = applicationEnvironment;
        myProject          = createProject(myEnvironment.getApplication().getPicoContainer(), myParentDisposable);

        preregisterServices();

        myFileIndexFacade = createFileIndexFacade();
        myMessageBus      = myProject.getMessageBus();

        PsiModificationTrackerImpl modificationTracker = new PsiModificationTrackerImpl(myProject);
        myProject.registerService(PsiModificationTracker.class, modificationTracker);
        myProject.registerService(FileIndexFacade.class, myFileIndexFacade);
        myProject.registerService(ResolveCache.class, new ResolveCache(myProject));

        myPsiManager = new PsiManagerImpl(myProject);
        myProject.registerService(PsiManager.class, myPsiManager);
        myProject.registerService(SmartPointerManager.class, SmartPointerManagerImpl.class);
        //myProject.registerService(DocumentCommitProcessor.class, new MockDocumentCommitProcessor());
        myProject.registerService(PsiDocumentManager.class, new IdeaPsiDocumentManager(myProject));

        myProject.registerService(ResolveScopeManager.class, createResolveScopeManager(myPsiManager));

        myProject.registerService(PsiFileFactory.class, new PsiFileFactoryImpl(myPsiManager));
        myProject.registerService(CachedValuesManager.class, new CachedValuesManagerImpl(myProject, new PsiCachedValuesFactory(myProject)));
        myProject.registerService(PsiDirectoryFactory.class, new PsiDirectoryFactoryImpl(myProject));
        myProject.registerService(ProjectScopeBuilder.class, createProjectScopeBuilder());
        myProject.registerService(DumbService.class, new MockDumbService(myProject));
        myProject.registerService(DumbUtil.class, new MockDumbUtil());
        myProject.registerService(CoreEncodingProjectManager.class, CoreEncodingProjectManager.class);
        myProject.registerService(InjectedLanguageManager.class, CoreInjectedLanguageManager.class /*new CoreInjectedLanguageManager()*/);

        //----Java----
        myProject.registerService(PsiElementFactory.class, new PsiElementFactoryImpl(myProject));
        myProject.registerService(JavaPsiImplementationHelper.class, new CoreJavaPsiImplementationHelper(myProject));
        myProject.registerService(PsiResolveHelper.class, new PsiResolveHelperImpl(myProject));
        myProject.registerService(LanguageLevelProjectExtension.class, new CoreLanguageLevelProjectExtension());
        myProject.registerService(JavaResolveCache.class, new JavaResolveCache(myProject));
        myProject.registerService(JavaCodeStyleSettingsFacade.class, new CoreJavaCodeStyleSettingsFacade());
        myProject.registerService(JavaCodeStyleManager.class, new CoreJavaCodeStyleManager());
        myProject.registerService(ControlFlowFactory.class, new ControlFlowFactory(myProject));

        myPackageIndex = new CorePackageIndex(); //createCorePackageIndex();
        myProject.registerService(PackageIndex.class, myPackageIndex);

        myFileManager = new CoreJavaFileManager(myPsiManager); //createCoreFileManager();
        myProject.registerService(JavaFileManager.class, myFileManager);

        myProject.registerService(JvmPsiConversionHelper.class, new JvmPsiConversionHelperImpl());
        myProject.registerService(JavaPsiFacade.class, new JavaPsiFacadeImpl(myProject));
        myProject.registerService(JvmFacade.class, new JvmFacadeImpl(myProject, myMessageBus));

        //----Java Others----
        registerProjectComponent(ProjectRootManager.class, new IdeaProjectRootManager()); //completion
        myProject.registerService(GuessManager.class, new GuessManagerImpl(myProject)); //completion
        myProject.registerService(JavaProjectCodeInsightSettings.class, new JavaProjectCodeInsightSettings()); //completion
        registerProjectExtensionPoint(PsiElementFinder.EP_NAME, PsiElementFinderImpl.class);
        addProjectExtension(PsiElementFinder.EP_NAME, new PsiElementFinderImpl(myProject));
    }

    protected MockProject createProject(PicoContainer parent, Disposable parentDisposable) {
        return new MockProject(parent, parentDisposable);
    }

    protected ProjectScopeBuilder createProjectScopeBuilder() {
        return new CoreProjectScopeBuilder(myProject, myFileIndexFacade);
    }

    protected void preregisterServices() {
    }

    protected FileIndexFacade createFileIndexFacade() {
        return new MockFileIndexFacade(myProject);
    }

    protected ResolveScopeManager createResolveScopeManager(PsiManager psiManager) {
        return new MockResolveScopeManager(psiManager.getProject());
    }

    public <T> void registerProjectExtensionPoint(ExtensionPointName<T> extensionPointName, Class<? extends T> aClass) {
        IdeaApplicationEnvironment.registerExtensionPoint(myProject.getExtensionArea(), extensionPointName, aClass);
    }

    public <T> void addProjectExtension(ExtensionPointName<T> name, final T extension) {
        //noinspection TestOnlyProblems
        name.getPoint(myProject).registerExtension(extension, myParentDisposable);
    }

    public <T> void registerProjectComponent(Class<T> interfaceClass, T implementation) {
        IdeaApplicationEnvironment.registerComponentInstance(myProject.getPicoContainer(), interfaceClass, implementation);
        if (implementation instanceof Disposable) {
            Disposer.register(myProject, (Disposable) implementation);
        }
    }

    public Disposable getParentDisposable() {
        return myParentDisposable;
    }

    public IdeaApplicationEnvironment getEnvironment() {
        return myEnvironment;
    }

    public MockProject getProject() {
        return myProject;
    }

    public void addJarToClassPath(File path) {
        assert path.isFile();

        final VirtualFile root = getEnvironment().getJarFileSystem().findFileByPath(path + "!/");
        if (root == null) {
            throw new IllegalArgumentException("trying to add non-existing file to classpath: " + path);
        }

        addSourcesToClasspath(root);
    }

    public void addSourcesToClasspath(VirtualFile root) {
        assert root.isDirectory();
        ((CoreJavaFileManager) myFileManager).addToClasspath(root);
        ((CorePackageIndex) myPackageIndex).addToClasspath(root);
        ((MockFileIndexFacade) myFileIndexFacade).addLibraryRoot(root);
    }
}
