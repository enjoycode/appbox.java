package appbox.design.lang.java;

import appbox.design.IDeveloperSession;
import appbox.design.lang.java.jdt.DefaultVMType;
import appbox.design.lang.java.jdt.Document;
import appbox.design.lang.java.jdt.HackRegistryProvider;
import appbox.design.utils.PathUtil;
import appbox.design.utils.ReflectUtil;
import appbox.runtime.RuntimeContext;
import org.eclipse.core.internal.filesystem.InternalFileSystemCore;
import org.eclipse.core.internal.filesystem.NullFileSystem;
import org.eclipse.core.internal.preferences.DefaultPreferences;
import org.eclipse.core.internal.preferences.EclipsePreferences;
import org.eclipse.core.internal.preferences.InstancePreferences;
import org.eclipse.core.internal.preferences.PreferencesService;
import org.eclipse.core.internal.resources.ProjectPreferences;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.RegistryFactory;
import org.eclipse.core.runtime.preferences.DefaultScope;
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

import java.util.HashMap;
import java.util.HashSet;

final class JdtHacker {

    static PreferenceManager hack(JREContainerInitializer jreContainerInitializer,
                                  ProjectPreferences defaultProjectPreferences) throws Exception {
        defaultProjectPreferences.put(JavaCore.COMPILER_SOURCE, "11");
        defaultProjectPreferences.put(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, "11");
        defaultProjectPreferences.put(JavaCore.COMPILER_COMPLIANCE, "11");

        //init defaut java runtime
        DefaultVMType.init();
        //hack JAVA_LIKE_EXTENSIONS
        char[][] extensions = new char[1][];
        extensions[0] = "java".toCharArray();
        ReflectUtil.setField(org.eclipse.jdt.internal.core.util.Util.class, "JAVA_LIKE_EXTENSIONS", null, extensions);
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

        //hack JavaModelManager //TODO:*** 暂共用JavaModelManager
        var indexManager = new IndexManager(PathUtil.INDEX_DATA);
        //ReflectUtil.setField(IndexManager.class, "javaPluginLocation", indexManager, PathUtil.PLUGIN);
        ReflectUtil.setField(JavaModelManager.class, "indexManager", JavaModelManager.getJavaModelManager(), indexManager);
        ReflectUtil.setField(JavaModelManager.class, "cache", JavaModelManager.getJavaModelManager(), new JavaModelCache());
        var NO_PARTICIPANTS = ReflectUtil.getField(JavaModelManager.class, "NO_PARTICIPANTS", null);
        ReflectUtil.setField(JavaModelManager.CompilationParticipants.class, "registeredParticipants",
                JavaModelManager.getJavaModelManager().compilationParticipants, NO_PARTICIPANTS);
        ReflectUtil.setField(JavaModelManager.CompilationParticipants.class, "managedMarkerTypes",
                JavaModelManager.getJavaModelManager().compilationParticipants, new HashSet<String>());
        //JavaModelManager.getJavaModelManager().initializePreferences();
        JavaModelManager.getJavaModelManager().preferencesLookup[0] = instancePreferences;
        JavaModelManager.getJavaModelManager().preferencesLookup[1] = defaultPreferences;
        JavaModelManager.getJavaModelManager().containerInitializersCache.put(JavaRuntime.JRE_CONTAINER, jreContainerInitializer);

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

        //hack ResourcesPlugin (主要用于每个会话对应一个Workspace)
        ResourcesPlugin.workspaceSupplier =
                () -> ((IDeveloperSession) RuntimeContext.current()
                        .currentSession()).getDesignHub().typeSystem.javaLanguageServer.jdtWorkspace;

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
        RegistryFactory.setDefaultRegistryProvider(new HackRegistryProvider());
        ReflectUtil.setField(InternalFileSystemCore.class, "fileSystems", InternalFileSystemCore.getInstance(),
                new HashMap<String, Object>() {{
                    put("file", new NullFileSystem());
                }});

        return lsPreferenceManager;
    }
}
