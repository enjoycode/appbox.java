package appbox.design.idea;

import appbox.logging.Log;
import com.intellij.DynamicBundle;
import com.intellij.codeInsight.CodeInsightSettings;
import com.intellij.codeInsight.ContainerProvider;
import com.intellij.codeInsight.JavaContainerProvider;
import com.intellij.codeInsight.completion.BaseCompletionService;
import com.intellij.codeInsight.completion.CompletionService;
import com.intellij.codeInsight.folding.CodeFoldingSettings;
import com.intellij.codeInsight.folding.JavaCodeFoldingSettings;
import com.intellij.codeInsight.folding.impl.JavaCodeFoldingSettingsBase;
import com.intellij.codeInsight.folding.impl.JavaFoldingBuilderBase;
import com.intellij.codeInspection.SuppressManager;
import com.intellij.concurrency.Job;
import com.intellij.concurrency.JobLauncher;
import com.intellij.core.*;
import com.intellij.ide.highlighter.ArchiveFileType;
import com.intellij.ide.highlighter.JavaClassFileType;
import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.ide.plugins.DisabledPluginsState;
import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.lang.*;
import com.intellij.lang.folding.LanguageFolding;
import com.intellij.lang.impl.PsiBuilderFactoryImpl;
import com.intellij.lang.java.JavaLanguage;
import com.intellij.lang.java.JavaParserDefinition;
import com.intellij.mock.MockApplication;
import com.intellij.mock.MockFileDocumentManagerImpl;
import com.intellij.model.psi.PsiSymbolService;
import com.intellij.model.psi.impl.PsiSymbolServiceImpl;
import com.intellij.navigation.ItemPresentationProviders;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.TransactionGuard;
import com.intellij.openapi.application.TransactionGuardImpl;
import com.intellij.openapi.application.impl.ApplicationInfoImpl;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.command.impl.CoreCommandProcessor;
import com.intellij.openapi.editor.impl.DocumentImpl;
import com.intellij.openapi.extensions.*;
import com.intellij.openapi.extensions.impl.BeanExtensionPoint;
import com.intellij.openapi.extensions.impl.ExtensionPointImpl;
import com.intellij.openapi.extensions.impl.InterfaceExtensionPoint;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileTypes.BinaryFileTypeDecompilers;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeExtension;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.impl.CoreProgressManager;
import com.intellij.openapi.projectRoots.JavaVersionService;
import com.intellij.openapi.projectRoots.SdkType;
import com.intellij.openapi.projectRoots.impl.JavaSdkImpl;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.util.ClassExtension;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.vfs.VirtualFileManagerListener;
import com.intellij.openapi.vfs.VirtualFileSystem;
import com.intellij.openapi.vfs.encoding.EncodingManager;
import com.intellij.openapi.vfs.impl.CoreVirtualFilePointerManager;
import com.intellij.openapi.vfs.impl.VirtualFileManagerImpl;
import com.intellij.openapi.vfs.impl.jar.CoreJarFileSystem;
import com.intellij.openapi.vfs.local.CoreLocalFileSystem;
import com.intellij.openapi.vfs.pointers.VirtualFilePointerManager;
import com.intellij.psi.*;
import com.intellij.psi.augment.PsiAugmentProvider;
import com.intellij.psi.compiled.ClassFileDecompilers;
import com.intellij.psi.impl.*;
import com.intellij.psi.impl.compiled.ClassFileStubBuilder;
import com.intellij.psi.impl.file.PsiPackageImplementationHelper;
import com.intellij.psi.impl.search.MethodSuperSearcher;
import com.intellij.psi.impl.source.resolve.reference.ReferenceProvidersRegistry;
import com.intellij.psi.impl.source.resolve.reference.ReferenceProvidersRegistryImpl;
import com.intellij.psi.impl.source.tree.JavaASTFactory;
import com.intellij.psi.presentation.java.*;
import com.intellij.psi.search.searches.SuperMethodsSearch;
import com.intellij.psi.stubs.BinaryFileStubBuilders;
import com.intellij.psi.stubs.CoreStubTreeLoader;
import com.intellij.psi.stubs.StubTreeLoader;
import com.intellij.psi.util.JavaClassSupers;
import com.intellij.util.Consumer;
import com.intellij.util.KeyedLazyInstanceEP;
import com.intellij.util.Processor;
import com.intellij.util.QueryExecutor;
import com.intellij.util.graph.GraphAlgorithms;
import com.intellij.util.graph.impl.GraphAlgorithmsImpl;
import org.jetbrains.java.decompiler.IdeaDecompiler;
import org.picocontainer.MutablePicoContainer;

import java.lang.reflect.Modifier;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

public final class IdeaApplicationEnvironment {
    private final   CoreFileTypeRegistry myFileTypeRegistry;
    protected final MockApplication      myApplication;
    private final   CoreLocalFileSystem  myLocalFileSystem;
    protected final VirtualFileSystem    myJarFileSystem;
    private final   VirtualFileSystem    myJrtFileSystem;
    private final   Disposable           myParentDisposable;
    private final   boolean              myUnitTestMode;

    public IdeaApplicationEnvironment(Disposable parentDisposable) {
        this(parentDisposable, true);
    }

    public IdeaApplicationEnvironment(Disposable parentDisposable, boolean unitTestMode) {
        myParentDisposable = parentDisposable;
        myUnitTestMode     = unitTestMode;

        DisabledPluginsState.dontLoadDisabledPlugins();

        myFileTypeRegistry = new CoreFileTypeRegistry();

        myApplication = createApplication(myParentDisposable);
        ApplicationManager.setApplication(myApplication, () -> myFileTypeRegistry, myParentDisposable);
        myLocalFileSystem = createLocalFileSystem();
        myJarFileSystem   = createJarFileSystem();
        myJrtFileSystem   = createJrtFileSystem();

        registerApplicationService(FileDocumentManager.class, new MockFileDocumentManagerImpl(charSequence -> {
            //TODO:重新实现Document
            return new DocumentImpl(charSequence, true);
        }, null));

        registerApplicationExtensionPoint(new ExtensionPointName<>("com.intellij.virtualFileManagerListener"),
                VirtualFileManagerListener.class);
        List<VirtualFileSystem> fs = myJrtFileSystem != null
                ? Arrays.asList(myLocalFileSystem, myJarFileSystem, myJrtFileSystem)
                : Arrays.asList(myLocalFileSystem, myJarFileSystem);
        registerApplicationService(VirtualFileManager.class, new VirtualFileManagerImpl(fs));

        //fake EP for cleaning resources after area disposing (otherwise KeyedExtensionCollector listener will be copied to the next area)
        registerApplicationExtensionPoint(new ExtensionPointName<>("com.intellij.virtualFileSystem"), KeyedLazyInstanceEP.class);

        registerApplicationService(EncodingManager.class, new CoreEncodingRegistry());
        registerApplicationService(VirtualFilePointerManager.class, createVirtualFilePointerManager());
        registerApplicationService(DefaultASTFactory.class, new DefaultASTFactoryImpl());
        registerApplicationService(PsiBuilderFactory.class, new PsiBuilderFactoryImpl());
        registerApplicationService(ReferenceProvidersRegistry.class, new ReferenceProvidersRegistryImpl());
        registerApplicationService(StubTreeLoader.class, new CoreStubTreeLoader());
        registerApplicationService(PsiReferenceService.class, new PsiReferenceServiceImpl());
        registerApplicationService(ProgressManager.class, createProgressIndicatorProvider());
        registerApplicationService(JobLauncher.class, createJobLauncher());
        registerApplicationService(CodeFoldingSettings.class, new CodeFoldingSettings());
        registerApplicationService(CommandProcessor.class, new CoreCommandProcessor());
        registerApplicationService(GraphAlgorithms.class, new GraphAlgorithmsImpl());

        myApplication.registerService(ApplicationInfo.class, ApplicationInfoImpl.class);

        registerApplicationExtensionPoint(DynamicBundle.LanguageBundleEP.EP_NAME, DynamicBundle.LanguageBundleEP.class);

        //----Java----
        registerFileType(JavaClassFileType.INSTANCE, "class");
        registerFileType(JavaFileType.INSTANCE, "java");
        registerFileType(ArchiveFileType.INSTANCE, "jar;zip");

        addExplicitExtension(FileTypeFileViewProviders.INSTANCE, JavaClassFileType.INSTANCE,
                new IdeaClassFileViewProviderFactory() /*new ClassFileViewProviderFactory()*/);
        addExplicitExtension(BinaryFileStubBuilders.INSTANCE, JavaClassFileType.INSTANCE, new ClassFileStubBuilder());

        addExplicitExtension(LanguageASTFactory.INSTANCE, JavaLanguage.INSTANCE, new JavaASTFactory());
        registerParserDefinition(new JavaParserDefinition());
        //addExplicitExtension(LanguageConstantExpressionEvaluator.INSTANCE, JavaLanguage.INSTANCE, new PsiExpressionEvaluator());

        registerApplicationExtensionPoint(ContainerProvider.EP_NAME, ContainerProvider.class);
        addExtension(ContainerProvider.EP_NAME, new JavaContainerProvider());

        myApplication.registerService(PsiPackageImplementationHelper.class, new CorePsiPackageImplementationHelper());

        myApplication.registerService(PsiSubstitutorFactory.class, new PsiSubstitutorFactoryImpl());
        myApplication.registerService(JavaDirectoryService.class, new CoreJavaDirectoryService());
        myApplication.registerService(JavaVersionService.class, new JavaVersionService());

        addExplicitExtension(ItemPresentationProviders.INSTANCE, PsiPackage.class, new PackagePresentationProvider());
        addExplicitExtension(ItemPresentationProviders.INSTANCE, PsiClass.class, new ClassPresentationProvider());
        addExplicitExtension(ItemPresentationProviders.INSTANCE, PsiMethod.class, new MethodPresentationProvider());
        addExplicitExtension(ItemPresentationProviders.INSTANCE, PsiField.class, new FieldPresentationProvider());
        addExplicitExtension(ItemPresentationProviders.INSTANCE, PsiLocalVariable.class, new VariablePresentationProvider());
        addExplicitExtension(ItemPresentationProviders.INSTANCE, PsiParameter.class, new VariablePresentationProvider());

        registerApplicationService(JavaCodeFoldingSettings.class, new JavaCodeFoldingSettingsBase());
        addExplicitExtension(LanguageFolding.INSTANCE, JavaLanguage.INSTANCE, new JavaFoldingBuilderBase() {
            @Override
            protected boolean shouldShowExplicitLambdaType(PsiAnonymousClass anonymousClass, PsiNewExpression expression) {
                return false;
            }

            @Override
            protected boolean isBelowRightMargin(PsiFile file, int lineLength) {
                return false;
            }
        });

        registerApplicationExtensionPoint(SuperMethodsSearch.EP_NAME, QueryExecutor.class);
        addExtension(SuperMethodsSearch.EP_NAME, new MethodSuperSearcher());

        registerApplicationDynamicExtensionPoint("com.intellij.filetype.decompiler", BinaryFileTypeDecompilers.class);
        registerApplicationDynamicExtensionPoint("com.intellij.psi.classFileDecompiler", ClassFileDecompilers.Decompiler.class);
        //addExtension(ClassFileDecompilers.getInstance().EP_NAME, new ClsDecompilerImpl());
        addExtension(ClassFileDecompilers.getInstance().EP_NAME, new IdeaDecompiler());

        //----Java Others----
        registerApplicationExtensionPoint(PsiAugmentProvider.EP_NAME, RecordAugmentProvider.class);
        registerApplicationExtensionPoint(JavaModuleSystem.EP_NAME, JavaPlatformModuleSystem.class);
        registerApplicationExtensionPoint(SdkType.EP_NAME, JavaSdkImpl.class);
        registerApplicationExtensionPoint(OrderRootType.EP_NAME, OrderRootType.class);

        registerApplicationService(JavaClassSupers.class, new JavaClassSupersImpl()); //getVariants需要
        registerApplicationService(PsiSymbolService.class, new PsiSymbolServiceImpl());
        registerApplicationService(CompletionService.class, new BaseCompletionService());
        registerApplicationService(CodeInsightSettings.class, new CodeInsightSettings());
        registerApplicationService(SuppressManager.class, new IdeaSuppressManager()); //completion

        registerApplicationService(TransactionGuard.class, new TransactionGuardImpl()); //document commit
        //myApplication.registerService(DocumentCommitProcessor.class, DocumentCommitThread.class); //document commit
    }

    public <T> void registerApplicationService(Class<T> serviceInterface, T serviceImplementation) {
        myApplication.registerService(serviceInterface, serviceImplementation);
    }

    protected VirtualFilePointerManager createVirtualFilePointerManager() {
        return new CoreVirtualFilePointerManager();
    }

    protected MockApplication createApplication(Disposable parentDisposable) {
        return new MockApplication(parentDisposable) {
            @Override
            public boolean isUnitTestMode() {
                return myUnitTestMode;
            }

            @Override
            public boolean isDispatchThread() {
                return true; //Rick changed
            }

            //@Override
            //public boolean isHeadlessEnvironment() {
            //    return true; //Rick changed
            //}

            @Override
            public boolean hasWriteAction(Class<?> actionClass) {
                return true; //Rick changed
            }
        };
    }

    protected JobLauncher createJobLauncher() {
        return new JobLauncher() {
            @Override
            public <T> boolean invokeConcurrentlyUnderProgress(List<? extends T> things,
                                                               ProgressIndicator progress,
                                                               boolean runInReadAction,
                                                               boolean failFastOnAcquireReadAction,
                                                               Processor<? super T> thingProcessor) {
                for (T thing : things) {
                    if (!thingProcessor.process(thing))
                        return false;
                }
                return true;
            }


            @Override
            public Job<Void> submitToJobThread(Runnable action, Consumer<? super Future<?>> onDoneCallback) {
                action.run();
                if (onDoneCallback != null) {
                    onDoneCallback.consume(CompletableFuture.completedFuture(null));
                }
                return Job.NULL_JOB;
            }
        };
    }

    protected ProgressManager createProgressIndicatorProvider() {
        return new CoreProgressManager();
    }

    protected VirtualFileSystem createJarFileSystem() {
        return new CoreJarFileSystem();
    }

    protected CoreLocalFileSystem createLocalFileSystem() {
        return new CoreLocalFileSystem();
    }

    protected VirtualFileSystem createJrtFileSystem() {
        return null;
    }

    public MockApplication getApplication() {
        return myApplication;
    }

    public Disposable getParentDisposable() {
        return myParentDisposable;
    }

    public <T> void registerApplicationComponent(Class<T> interfaceClass, T implementation) {
        registerComponentInstance(myApplication.getPicoContainer(), interfaceClass, implementation);
        if (implementation instanceof Disposable) {
            Disposer.register(myApplication, (Disposable) implementation);
        }
    }

    public void registerFileType(FileType fileType, String extension) {
        myFileTypeRegistry.registerFileType(fileType, extension);
    }

    public void registerParserDefinition(ParserDefinition definition) {
        addExplicitExtension(LanguageParserDefinitions.INSTANCE, definition.getFileNodeType().getLanguage(), definition);
    }

    public static <T> void registerComponentInstance(MutablePicoContainer container, Class<T> key, T implementation) {
        container.unregisterComponent(key);
        container.registerComponentInstance(key, implementation);
    }

    public <T> void addExplicitExtension(LanguageExtension<T> instance, Language language, T object) {
        instance.addExplicitExtension(language, object, myParentDisposable);
    }

    public void registerParserDefinition(Language language, ParserDefinition parserDefinition) {
        addExplicitExtension(LanguageParserDefinitions.INSTANCE, language, parserDefinition);
    }

    public <T> void addExplicitExtension(final FileTypeExtension<T> instance, final FileType fileType, final T object) {
        instance.addExplicitExtension(fileType, object, myParentDisposable);
    }

    public <T> void addExplicitExtension(final ClassExtension<T> instance, final Class aClass, final T object) {
        instance.addExplicitExtension(aClass, object, myParentDisposable);
    }

    public <T> void addExtension(ExtensionPointName<T> name, final T extension) {
        final ExtensionPoint<T> extensionPoint = Extensions.getRootArea().getExtensionPoint(name);
        //noinspection TestOnlyProblems
        extensionPoint.registerExtension(extension, myParentDisposable);
    }

    public static <T> void registerExtensionPoint(ExtensionsArea area,
                                                  ExtensionPointName<T> extensionPointName,
                                                  Class<? extends T> aClass) {
        registerExtensionPoint(area, extensionPointName.getName(), aClass);
    }

    public static <T> void registerExtensionPoint(ExtensionsArea area,
                                                  BaseExtensionPointName extensionPointName,
                                                  Class<? extends T> aClass) {
        registerExtensionPoint(area, extensionPointName.getName(), aClass);
    }

    public static <T> void registerExtensionPoint(ExtensionsArea area, String name, Class<? extends T> aClass) {
        registerExtensionPoint(area, name, aClass, false);
    }

    public static <T> void registerDynamicExtensionPoint(ExtensionsArea area, String name, Class<? extends T> aClass) {
        registerExtensionPoint(area, name, aClass, true);
    }

    private static <T> void registerExtensionPoint(ExtensionsArea area, String name, Class<? extends T> aClass, boolean dymanic) {
        if (!area.hasExtensionPoint(name)) {
            ExtensionPoint.Kind kind = aClass.isInterface() || Modifier.isAbstract(aClass.getModifiers())
                    ? ExtensionPoint.Kind.INTERFACE : ExtensionPoint.Kind.BEAN_CLASS;
            if (dymanic) {
                //Rick: 暂用反射处理area.registerDynamicExtensionPoint(name, aClass.getName(), kind);
                try {
                    var                   pluginDescriptor = new DefaultPluginDescriptor(PluginId.getId("FakeIdForTests"));
                    ExtensionPointImpl<T> point;
                    if (kind == ExtensionPoint.Kind.INTERFACE) {
                        point = new InterfaceExtensionPoint<>(name, aClass.getName(), pluginDescriptor, dymanic);
                    } else {
                        point = new BeanExtensionPoint<>(name, aClass.getName(), pluginDescriptor, dymanic);
                    }
                    var sfield = point.getClass().getSuperclass().getDeclaredField("componentManager");
                    var gfield = area.getClass().getDeclaredField("componentManager");
                    gfield.setAccessible(true);
                    sfield.setAccessible(true);
                    sfield.set(point, gfield.get(area));
                    gfield.setAccessible(false);
                    sfield.setAccessible(false);

                    var extPointsField = area.getClass().getDeclaredField("extensionPoints");
                    extPointsField.setAccessible(true);
                    ((Map<String, ExtensionPointImpl<?>>) extPointsField.get(area)).put(name, point);
                    extPointsField.setAccessible(false);

                    //var registerMethod   = area.getClass().getMethod("doRegisterExtensionPoint");
                    //registerMethod.invoke(area, name, aClass.getName(), pluginDescriptor,
                    //        kind == ExtensionPoint.Kind.INTERFACE, dymanic);
                } catch (Exception ex) {
                    Log.error("RegisterDymanicExtensionPoint failed.");
                }
            } else {
                area.registerExtensionPoint(name, aClass.getName(), kind);
            }
        }
    }

    @SuppressWarnings("deprecation")
    public static <T> void registerApplicationExtensionPoint(ExtensionPointName<T> extensionPointName, Class<? extends T> aClass) {
        registerExtensionPoint(Extensions.getRootArea(), extensionPointName, aClass);
    }

    @SuppressWarnings("deprecation")
    public static <T> void registerApplicationDynamicExtensionPoint(String extensionPointName, Class<? extends T> aClass) {
        registerDynamicExtensionPoint(Extensions.getRootArea(), extensionPointName, aClass);
    }

    public static void registerExtensionPointAndExtensions(Path pluginRoot, String fileName, ExtensionsArea area) {
        PluginManagerCore.registerExtensionPointAndExtensions(pluginRoot, fileName, area);
    }

    public CoreLocalFileSystem getLocalFileSystem() {
        return myLocalFileSystem;
    }

    public VirtualFileSystem getJarFileSystem() {
        return myJarFileSystem;
    }

    public VirtualFileSystem getJrtFileSystem() {
        return myJrtFileSystem;
    }

}
