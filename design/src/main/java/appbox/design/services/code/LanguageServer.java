package appbox.design.services.code;

import appbox.design.IDeveloperSession;
import appbox.design.jdt.DefaultVMType;
import appbox.design.jdt.Document;
import appbox.design.jdt.ModelWorkspace;
import appbox.design.tree.ModelNode;
import appbox.design.utils.ReflectUtil;
import appbox.logging.Log;
import appbox.runtime.RuntimeContext;

import org.eclipse.core.internal.preferences.DefaultPreferences;
import org.eclipse.core.internal.preferences.EclipsePreferences;
import org.eclipse.core.internal.preferences.InstancePreferences;
import org.eclipse.core.internal.preferences.PreferencesService;
import org.eclipse.core.internal.resources.ProjectPreferences;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jdt.core.*;
import org.eclipse.jdt.internal.core.*;
import org.eclipse.jdt.internal.core.search.indexing.IndexManager;
import org.eclipse.jdt.internal.launching.JREContainerInitializer;
import org.eclipse.jdt.internal.ui.util.StringMatcher;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.ls.core.internal.DocumentAdapter;
import org.eclipse.jdt.ls.core.internal.JDTUtils;
import org.eclipse.jdt.ls.core.internal.JVMConfigurator;
import org.eclipse.jdt.ls.core.internal.JavaLanguageServerPlugin;
import org.eclipse.jdt.ls.core.internal.contentassist.CompletionProposalRequestor;
import org.eclipse.jdt.ls.core.internal.contentassist.TypeFilter;
import org.eclipse.jdt.ls.core.internal.preferences.PreferenceManager;
import org.eclipse.jdt.ls.core.internal.syntaxserver.ModelBasedCompletionEngine;
import org.eclipse.lsp4j.ClientCapabilities;
import org.eclipse.lsp4j.CompletionItem;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;

/**
 * 一个TypeSystem对应一个实例，管理JavaProject及相应的虚拟文件
 */
public final class LanguageServer {
    private static final JREContainerInitializer jreContainerInitializer   = new JREContainerInitializer();
    private static final ProjectPreferences      defaultProjectPreferences = new ProjectPreferences();
    private static final PreferenceManager       lsPreferenceManager;

    static {
        defaultProjectPreferences.put(JavaCore.COMPILER_SOURCE, "11");
        defaultProjectPreferences.put(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, "11");
        defaultProjectPreferences.put(JavaCore.COMPILER_COMPLIANCE, "11");

        try {
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
            var indexPath    = new Path(System.getProperty("java.io.tmpdir")).append("appbox_index_data");
            var indexManager = new IndexManager(indexPath);
            ReflectUtil.setField(JavaModelManager.class, "indexManager", JavaModelManager.getJavaModelManager(), indexManager);
            ReflectUtil.setField(JavaModelManager.class, "cache", JavaModelManager.getJavaModelManager(), new JavaModelCache());
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
                            new StringMatcher("com.sun.*", false, false)
                    });

            //init default preferences (主要用于初始化JavaModelManager.optionNames,考虑直接设置)
            new JavaCorePreferenceInitializer().initializeDefaultPreferences();
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        //hack ResourcesPlugin
        ResourcesPlugin.workspaceSupplier =
                () -> ((IDeveloperSession) RuntimeContext.current()
                        .currentSession()).getDesignHub().typeSystem.languageServer.jdtWorkspace;

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

        lsPreferenceManager = new PreferenceManager();
        lsPreferenceManager.updateClientPrefences(new ClientCapabilities(), new HashMap<>());
        JavaLanguageServerPlugin.setPreferencesManager(lsPreferenceManager);
    }

    public final  ModelWorkspace          jdtWorkspace;
    private final HashMap<Long, Document> openedFiles = new HashMap<>();

    public LanguageServer() {
        jdtWorkspace = new ModelWorkspace();
        //TODO:如果不能共用JavaModelManager,在这里初始化
    }

    //region ====create XXX====

    /**
     * 创建指定名称的项目
     * @param name
     * @param deps 所依赖的内部项目列表，可为null
     */
    protected IProject createProject(String name, IProject[] deps) throws Exception {
        //TODO:check exists
        var project = jdtWorkspace.getRoot().getProject(name);
        project.create(null);
        //project.open(null);

        var perProjectInfo = JavaModelManager.getJavaModelManager()
                .getPerProjectInfo(project, true);
        perProjectInfo.preferences = defaultProjectPreferences;

        var javaProject = JavaCore.create(project);
        JVMConfigurator.configureJVMSettings(javaProject, JavaRuntime.getDefaultVMInstall());

        //var buildPath = new IClasspathEntry[] {
        //        JavaRuntime.getDefaultJREContainerEntry(),
        //        JavaCore.newSourceEntry(project.getFullPath())
        //};
        int buildPathCount = 2;
        if (deps != null) {
            buildPathCount += deps.length;
        }
        IClasspathEntry[] buildPath = new IClasspathEntry[buildPathCount];
        buildPath[0] = JavaRuntime.getDefaultJREContainerEntry();
        buildPath[1] = JavaCore.newSourceEntry(project.getFullPath());
        if (deps != null) {
            for (int i = 0; i < deps.length; i++) {
                buildPath[i + 2] = JavaCore.newProjectEntry(deps[i].getFullPath());
            }
        }

        //TODO: 待检查setRawClasspath的referencedEntries参数
        var outPath = project.getFullPath().append("bin");
        perProjectInfo.setRawClasspath(buildPath, outPath, JavaModelStatus.VERIFIED_OK);

        return project;
    }
    //endregion

    //region ====open/close/change Document====
    public Document openDocument(ModelNode node) throws JavaModelException {
        //TODO:仅允许打开特定类型的
        //TODO:暂简单处理
        var appName     = node.appNode.model.name();
        var fileName    = String.format("%s.java", node.model().name());
        var projectName = String.format("%s_services_%s", appName, node.model().name());

        var project = jdtWorkspace.getRoot().getProject(projectName);
        var file    = (IFile) project.findMember(fileName);
        var cu      = JDTUtils.resolveCompilationUnit(file);
        cu.becomeWorkingCopy(null); //must call
        var doc = (Document) cu.getBuffer();
        openedFiles.put(node.model().id(), doc);
        return doc;
    }

    public Document findOpenedDocument(long modelId) {
        return openedFiles.get(modelId);
    }

    public void changeDocument(Document doc, int startLine, int startColumn,
                               int endLine, int endColumn, String newText) {
        doc.changeText(startLine, startColumn, endLine, endColumn, newText);
        //TODO:检查是否需要同步结构
        //CompliationUnit.makeConsistent(null);
    }
    //endregion

    public List<CompletionItem> completion(Document doc, int line, int column, String wordToComplete) {
        //参考CompletionHandler实现
        var offset = doc.getOffset(line, column);
        var cu     = JDTUtils.resolveCompilationUnit((IFile) doc.getUnderlyingResource());

        var collector = new CompletionProposalRequestor(cu, offset, lsPreferenceManager);
        collector.setAllowsRequiredProposals(CompletionProposal.FIELD_REF, CompletionProposal.TYPE_REF, true);
        collector.setAllowsRequiredProposals(CompletionProposal.FIELD_REF, CompletionProposal.TYPE_IMPORT, true);
        collector.setAllowsRequiredProposals(CompletionProposal.FIELD_REF, CompletionProposal.FIELD_IMPORT, true);
        collector.setAllowsRequiredProposals(CompletionProposal.METHOD_REF, CompletionProposal.TYPE_REF, true);
        collector.setAllowsRequiredProposals(CompletionProposal.METHOD_REF, CompletionProposal.TYPE_IMPORT, true);
        collector.setAllowsRequiredProposals(CompletionProposal.METHOD_REF, CompletionProposal.METHOD_IMPORT, true);
        collector.setAllowsRequiredProposals(CompletionProposal.CONSTRUCTOR_INVOCATION, CompletionProposal.TYPE_REF, true);
        collector.setAllowsRequiredProposals(CompletionProposal.ANONYMOUS_CLASS_CONSTRUCTOR_INVOCATION, CompletionProposal.TYPE_REF, true);
        collector.setAllowsRequiredProposals(CompletionProposal.ANONYMOUS_CLASS_DECLARATION, CompletionProposal.TYPE_REF, true);
        collector.setAllowsRequiredProposals(CompletionProposal.TYPE_REF, CompletionProposal.TYPE_REF, true);
        //collector.setFavoriteReferences(getFavoriteStaticMembers());

        try {
            //cu.codeComplete(offset, collector);
            ModelBasedCompletionEngine.codeComplete(cu, offset, collector, DefaultWorkingCopyOwner.PRIMARY, null);
            return collector.getCompletionItems();
        } catch (Exception ex) {
            ex.printStackTrace();
            return Collections.emptyList();
        }
    }

}
