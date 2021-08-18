package appbox.design.lang.java.jdt;

import appbox.design.utils.PathUtil;
import appbox.design.utils.ReflectUtil;
import org.eclipse.core.internal.filesystem.InternalFileSystemCore;
import org.eclipse.core.internal.filesystem.NullFileSystem;
import org.eclipse.core.internal.preferences.DefaultPreferences;
import org.eclipse.core.internal.preferences.EclipsePreferences;
import org.eclipse.core.internal.preferences.InstancePreferences;
import org.eclipse.core.internal.preferences.PreferencesService;
import org.eclipse.core.internal.resources.ProjectPreferences;
import org.eclipse.core.internal.resources.Workspace;
import org.eclipse.core.internal.runtime.DataArea;
import org.eclipse.core.internal.runtime.InternalPlatform;
import org.eclipse.core.internal.runtime.MetaDataKeeper;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.core.runtime.RegistryFactory;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IPreferencesService;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jdt.core.IBuffer;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.WorkingCopyOwner;
import org.eclipse.jdt.internal.core.JavaCorePreferenceInitializer;
import org.eclipse.jdt.internal.core.JavaModelCache;
import org.eclipse.jdt.internal.core.JavaModelManager;
import org.eclipse.jdt.internal.core.search.indexing.IndexManager;
import org.eclipse.jdt.internal.launching.JREContainerInitializer;
import org.eclipse.jdt.internal.ui.util.StringMatcher;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.ls.core.internal.DocumentAdapter;
import org.eclipse.jdt.ls.core.internal.JavaLanguageServerPlugin;
import org.eclipse.jdt.ls.core.internal.contentassist.TypeFilter;
import org.eclipse.jdt.ls.core.internal.preferences.PreferenceManager;
import org.eclipse.lsp4j.ClientCapabilities;
import org.osgi.util.tracker.ServiceTracker;

import java.util.HashMap;
import java.util.HashSet;

public final class JdtHacker {

    public static PreferenceManager hack(JREContainerInitializer jreContainerInitializer,
                                         ProjectPreferences defaultProjectPreferences) throws Exception {
        defaultProjectPreferences.put(JavaCore.COMPILER_SOURCE, "11");
        defaultProjectPreferences.put(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, "11");
        defaultProjectPreferences.put(JavaCore.COMPILER_COMPLIANCE, "11");

        final var bundleContext = new HackBundleContext();
        RegistryFactory.setDefaultRegistryProvider(new HackRegistryProvider());

        //init defaut java runtime
        DefaultVMType.init();

        //hack JAVA_LIKE_EXTENSIONS
        char[][] extensions = new char[1][];
        extensions[0] = "java".toCharArray();
        ReflectUtil.setField(org.eclipse.jdt.internal.core.util.Util.class,
                "JAVA_LIKE_EXTENSIONS", null, extensions);

        //hack InternalPlatform
        ReflectUtil.setField(InternalPlatform.class, "context",
                InternalPlatform.getDefault(), bundleContext);
        ReflectUtil.setField(InternalPlatform.class, "cachedInstanceLocation",
                InternalPlatform.getDefault(), PathUtil.WORKSPACE_PATH);
        ReflectUtil.setField(InternalPlatform.class, "initialized",
                InternalPlatform.getDefault(), true);
        final var preferencesService = new HackPreferencesService();
        final var preferencesTracker = new ServiceTracker<IPreferencesService, IPreferencesService>(
                bundleContext, IPreferencesService.class, null);
        ReflectUtil.setField(ServiceTracker.class, "cachedService", preferencesTracker, preferencesService);
        ReflectUtil.setField(InternalPlatform.class, "preferencesTracker",
                InternalPlatform.getDefault(), preferencesTracker);

        //hack DataArea
        ReflectUtil.setField(DataArea.class, "location", MetaDataKeeper.getMetaArea(), PathUtil.WORKSPACE_PATH);
        ReflectUtil.invokeMethod(DataArea.class, "initializeLocation", MetaDataKeeper.getMetaArea());

        //hack JavaCore (after above)
        final var javaCore = new JavaCore();
        ReflectUtil.setField(Plugin.class, "bundle", javaCore, bundleContext.getBundle());

        //hack preferences
        var rootNode            = PreferencesService.getDefault().getRootNode();
        var instanceNode        = rootNode.node(InstanceScope.INSTANCE.getName());
        var instancePreferences = new InstancePreferences();
        var defaultNode         = rootNode.node(DefaultScope.INSTANCE.getName());
        var defaultPreferences  = new DefaultPreferences();

        HashMap<String, Object> imap = new HashMap<>() {{
            put("org.eclipse.jdt.core", instancePreferences);
        }};
        ReflectUtil.setField(EclipsePreferences.class, "children", instanceNode, imap);

        HashMap<String, Object> dmap = new HashMap<>() {{
            put("org.eclipse.jdt.core", defaultPreferences);
            put("org.eclipse.jdt.ls.core", new DefaultPreferences());
        }};
        ReflectUtil.setField(EclipsePreferences.class, "children", defaultNode, dmap);

        //hack JavaModelManager
        final var indexManager     = new IndexManager();
        final var javaModelManager = JavaModelManager.getJavaModelManager();
        //ReflectUtil.setField(IndexManager.class, "javaPluginLocation", indexManager, PathUtil.PLUGIN);
        ReflectUtil.setField(JavaModelManager.class, "indexManager", javaModelManager, indexManager);
        ReflectUtil.setField(JavaModelManager.class, "cache", javaModelManager, new JavaModelCache());
        ReflectUtil.setField(JavaModelManager.class, "assumedExternalFiles", javaModelManager, new HashSet<IPath>());
        var NO_PARTICIPANTS = ReflectUtil.getField(JavaModelManager.class, "NO_PARTICIPANTS", null);
        ReflectUtil.setField(JavaModelManager.CompilationParticipants.class, "registeredParticipants",
                JavaModelManager.getJavaModelManager().compilationParticipants, NO_PARTICIPANTS);
        ReflectUtil.setField(JavaModelManager.CompilationParticipants.class, "managedMarkerTypes",
                JavaModelManager.getJavaModelManager().compilationParticipants, new HashSet<String>());
        //javaModelManager.initializePreferences(); 等同于以下3句
        javaModelManager.preferencesLookup[0] = instancePreferences;
        javaModelManager.preferencesLookup[1] = defaultPreferences;
        javaModelManager.containerInitializersCache.put(JavaRuntime.JRE_CONTAINER, jreContainerInitializer);
        //var jreInitializer = JavaCore.getClasspathContainerInitializer(JavaRuntime.JRE_CONTAINER);

        //hack JavaLanguageServerPlugin & TypeFilter
        var javaLanguageServerPlugin = new JavaLanguageServerPlugin();
        ReflectUtil.setField(JavaLanguageServerPlugin.class, "pluginInstance", null, javaLanguageServerPlugin);
        ReflectUtil.setField(TypeFilter.class, "fStringMatchers", javaLanguageServerPlugin.getTypeFilter(),
                new StringMatcher[]{
                        new StringMatcher("java.awt.*", false, false),
                        new StringMatcher("com.sun.*", false, false),
                        new StringMatcher("org.w3c.*", false, false),
                        new StringMatcher("sun.*", false, false)
                });

        //init default preferences (主要用于初始化JavaModelManager.optionNames,考虑直接设置)
        new JavaCorePreferenceInitializer().initializeDefaultPreferences();

        //hack ResourcesPlugin
        final var resourcesPlugin = new ResourcesPlugin();
        ReflectUtil.setField(Plugin.class, "bundle", resourcesPlugin, bundleContext.getBundle());
        final var workspace = new ModelWorkspace();
        ReflectUtil.setField(ResourcesPlugin.class, "workspace", null, workspace);

        WorkingCopyOwner.setPrimaryBufferProvider(new WorkingCopyOwner() {
            @Override
            public IBuffer createBuffer(ICompilationUnit workingCopy) {
                ICompilationUnit original = workingCopy.getPrimary();
                IResource        resource = original.getResource();
                if (resource instanceof IFile) {
                    return new Document(workingCopy, (IFile) resource);
                }
                return DocumentAdapter.Null;
            }
        });

        final var lsPreferenceManager = new PreferenceManager();
        lsPreferenceManager.updateClientPrefences(new ClientCapabilities(), new HashMap<>());
        JavaLanguageServerPlugin.setPreferencesManager(lsPreferenceManager);

        //hack InternalFileSystemCore for build runtime service code
        ReflectUtil.setField(InternalFileSystemCore.class, "fileSystems", InternalFileSystemCore.getInstance(),
                new HashMap<String, Object>() {{
                    put("file", new NullFileSystem());
                }});

        //finally
        workspace.open(null);
        startupJavaModelManager(workspace);

        return lsPreferenceManager;
    }

    private static void startupJavaModelManager(Workspace workspace) {
        //// listen for resource changes
        //final var deltaState = JavaModelManager.getDeltaState();
        ////ReflectUtil.invokeMethod(DeltaProcessingState.class, "initializeRootsWithPreviousSession", deltaState);
        //workspace.addResourceChangeListener(deltaState,
        //        IResourceChangeEvent.POST_CHANGE
        //                | IResourceChangeEvent.PRE_DELETE
        //                | IResourceChangeEvent.PRE_CLOSE
        //                | IResourceChangeEvent.PRE_REFRESH);

        //start indexing
        JavaModelManager.getIndexManager().reset();
    }

}
