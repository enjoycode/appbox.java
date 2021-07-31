package appbox.design.lang.java;

import appbox.design.IDeveloperSession;
import appbox.design.lang.java.jdt.*;
import appbox.design.services.code.ServiceMethodInfo;
import appbox.design.tree.ModelNode;
import appbox.design.utils.PathUtil;
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
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jdt.core.*;
import org.eclipse.jdt.internal.core.*;
import org.eclipse.jdt.internal.core.search.indexing.IndexManager;
import org.eclipse.jdt.internal.launching.JREContainerInitializer;
import org.eclipse.jdt.internal.ui.util.StringMatcher;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.ls.core.internal.*;
import org.eclipse.jdt.ls.core.internal.DocumentAdapter;
import org.eclipse.jdt.ls.core.internal.contentassist.CompletionProposalRequestor;
import org.eclipse.jdt.ls.core.internal.contentassist.TypeFilter;
import org.eclipse.jdt.ls.core.internal.preferences.PreferenceManager;
import org.eclipse.jdt.ls.core.internal.syntaxserver.ModelBasedCompletionEngine;
import org.eclipse.lsp4j.*;

import java.io.InputStream;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * 一个TypeSystem对应一个实例，管理JavaProject及相应的虚拟文件
 */
public final class JdtLanguageServer {
    //region ====static====
    public static final String BUILD_OUTPUT = "bin";

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
            var indexManager = new IndexManager(PathUtil.INDEX_DATA);
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
    //endregion

    public final  long                    sessionId;
    public final  ModelWorkspace          jdtWorkspace;
    private final HashMap<Long, Document> openedFiles = new HashMap<>();

    public Function<IPath, InputStream> loadFileDelegate; //仅用于测试环境 TODO: move to MockRuntimeContext

    public JdtLanguageServer(long sessionId) {
        this.sessionId = sessionId;
        jdtWorkspace   = new ModelWorkspace(this);
        //TODO:如果不能共用JavaModelManager,在这里初始化
    }

    /**
     * 仅用于单元测试
     * @param loadFileDelegate 委托加载指定路径的测试文件
     */
    public JdtLanguageServer(Function<IPath, InputStream> loadFileDelegate) { //TODO: remove it
        sessionId                         = 0;
        jdtWorkspace                      = new ModelWorkspace(this);
        this.loadFileDelegate             = loadFileDelegate;
        ResourcesPlugin.workspaceSupplier = () -> jdtWorkspace;
    }

    //region ====Project Management====
    public static String makeServiceProjectName(ModelNode serviceNode) {
        return Long.toUnsignedString(serviceNode.model().id());
    }

    private static IClasspathEntry[] makeBuildPaths(IProject project, IClasspathEntry[] deps) {
        int buildPathCount = 2;
        if (deps != null) {
            buildPathCount += deps.length;
        }
        IClasspathEntry[] buildPath = new IClasspathEntry[buildPathCount];
        buildPath[0] = JavaRuntime.getDefaultJREContainerEntry();
        buildPath[1] = JavaCore.newSourceEntry(project.getFullPath());
        if (deps != null) {
            System.arraycopy(deps, 0, buildPath, 2, deps.length);
        }
        return buildPath;
    }

    /**
     * 创建指定名称的项目
     * @param deps 所依赖的内部项目列表，可为null
     */
    public IProject createProject(String name, IClasspathEntry[] deps) throws Exception {
        //TODO:check exists
        var project = jdtWorkspace.getRoot().getProject(name);
        project.create(null);
        //project.open(null);

        var perProjectInfo = JavaModelManager.getJavaModelManager()
                .getPerProjectInfo(project, true);
        perProjectInfo.preferences = defaultProjectPreferences;

        var javaProject = JavaCore.create(project);
        JVMConfigurator.configureJVMSettings(javaProject, JavaRuntime.getDefaultVMInstall());

        //TODO: 待检查setRawClasspath的referencedEntries参数
        final var buildPath = makeBuildPaths(project, deps);
        final var outPath   = project.getFullPath().append(BUILD_OUTPUT);
        perProjectInfo.setRawClasspath(buildPath, outPath, JavaModelStatus.VERIFIED_OK);

        return project;
    }

    /** 更新服务模型的第三方依赖(引用的jar包) */
    public void updateServiceReferences(ModelNode node, IClasspathEntry[] deps) {
        final var prjName = makeServiceProjectName(node);
        final var project = jdtWorkspace.getRoot().getProject(prjName);
        final var perProjectInfo = JavaModelManager.getJavaModelManager()
                .getPerProjectInfo(project, false);
        final var buildPath = makeBuildPaths(project, deps);
        perProjectInfo.setRawClasspath(buildPath, perProjectInfo.outputLocation, JavaModelStatus.VERIFIED_OK);
    }

    //endregion

    //region ====open/close/change Document====
    public Document openDocument(ModelNode node) throws JavaModelException {
        //TODO:仅允许打开特定类型的

        //先判断是否已打开，如果已打开可能是前端签出时发现变更要求重新从存储加载
        var doc = findOpenedDocument(node.model().id());
        if (doc != null) {
            //TODO:强制重新加载
            return doc;
        }

        var fileName    = String.format("%s.java", node.model().name());
        var projectName = makeServiceProjectName(node);

        var project = jdtWorkspace.getRoot().getProject(projectName);
        var file    = (IFile) project.findMember(fileName);
        var cu      = JDTUtils.resolveCompilationUnit(file);
        cu.becomeWorkingCopy(null); //must call
        doc = (Document) cu.getBuffer();
        openedFiles.put(node.model().id(), doc);
        return doc;
    }

    public Document findOpenedDocument(long modelId) {
        return openedFiles.get(modelId);
    }

    public void changeDocument(Document doc, int offset, int length, String newText) {
        doc.changeText(offset, length, newText);
        //TODO:检查是否需要同步结构
        //CompliationUnit.makeConsistent(null);
    }

    public void changeDocument(Document doc, int startLine, int startColumn,
                               int endLine, int endColumn, String newText) {
        doc.changeText(startLine, startColumn, endLine, endColumn, newText);
        //TODO:检查是否需要同步结构
        //CompliationUnit.makeConsistent(null);
    }

    public void closeDocument(long modelId) {
        var doc = findOpenedDocument(modelId);
        if (doc != null) {
            var file = (IFile) doc.getUnderlyingResource();
            var unit = JDTUtils.resolveCompilationUnit(file);
            try {
                if (unit.hasUnsavedChanges()) {
                    Log.debug(String.format("Close document[%s] with unsaved changes.", file.getName()));
                }
                unit.discardWorkingCopy();
            } catch (Exception ex) {
                ex.printStackTrace();
            } finally {
                openedFiles.remove(modelId);
            }
        }
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

    public List<DiagnosticsHandler.Diagnostic> diagnostics(Document doc) {
        var cu = JDTUtils.resolveCompilationUnit((IFile) doc.getUnderlyingResource());

        var diagnosticsHandler = new DiagnosticsHandler();
        WorkingCopyOwner wcOwner = new WorkingCopyOwner() {
            @Override
            public IBuffer createBuffer(ICompilationUnit workingCopy) {
                ICompilationUnit original = workingCopy.getPrimary();
                IResource        resource = original.getResource();
                if (resource instanceof IFile) {
                    return new DocumentAdapter(workingCopy, (IFile) resource);
                }
                return DocumentAdapter.Null;
            }

            @Override
            public IProblemRequestor getProblemRequestor(ICompilationUnit workingCopy) {
                return diagnosticsHandler;
            }

        };

        int flags = ICompilationUnit.FORCE_PROBLEM_DETECTION
                | ICompilationUnit.ENABLE_BINDINGS_RECOVERY
                | ICompilationUnit.ENABLE_STATEMENTS_RECOVERY;
        try {
            cu.reconcile(ICompilationUnit.NO_AST, flags, wcOwner, null);
            return diagnosticsHandler.getProblems();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public String[] hover(Document doc, int line, int column) {
        List<String> res     = new ArrayList<>();
        var          unit    = JDTUtils.resolveCompilationUnit((IFile) doc.getUnderlyingResource());
        var          monitor = new ProgressMonitor();
        try {
            var elements = JDTUtils.findElementsAtSelection(unit, line, column, lsPreferenceManager, monitor);
            if (elements == null || elements.length == 0)
                return null;

            IJavaElement curr = null;
            if (elements.length != 1) {
                IPackageFragment packageFragment = (IPackageFragment) unit.getParent();
                IJavaElement found = Stream.of(elements)
                        .filter((e) -> e.equals(packageFragment))
                        .findFirst().orElse(null);
                if (found == null) {
                    curr = elements[0];
                } else {
                    curr = found;
                }
            } else {
                curr = elements[0];
            }

            var signature = HoverInfoProvider.computeSignature(curr);
            if (signature != null)
                res.add(signature.getValue());

            //var javadoc = HoverInfoProvider.computeJavadoc(curr);
            //if (javadoc != null)
            //    res.add(javadoc.getValue());

            return res.toArray(String[]::new);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }

        // hoverInfoProvider.computeHover(line, column, monitor);
    }

    //region ====Find Methods====

    /** 根据行号列号找到服务方法相关信息,找不到返回null */
    public ServiceMethodInfo findServiceMethod(ModelNode serviceNode, int line, int column) {
        var     projectName = makeServiceProjectName(serviceNode);
        var     project     = jdtWorkspace.getRoot().getProject(projectName);
        var     file        = project.getFile(String.format("%s.java", serviceNode.model().name()));
        var     cu          = JDTUtils.resolveCompilationUnit(file);
        IBuffer buffer      = null;
        try {
            cu.makeConsistent(null);
            buffer = cu.getBuffer();
            var position = positionToOffset(buffer, line, column);
            var element  = cu.getElementAt(position);
            if (element instanceof SourceMethod) {
                return makeMethodInfo((SourceMethod) element);
            } else {
                Log.debug("find elemet[" + element.getClass().getSimpleName() + "] is not SourceMethod");
            }
        } catch (Exception ex) {
            Log.warn("findServiceMethod: " + ex.getMessage());
        }
        return null;
    }

    /** 根据方法名称找到服务方法相关信息,找不到返回null */
    public ServiceMethodInfo findServiceMethod(ModelNode serviceNode, String methodName) {
        var projectName = makeServiceProjectName(serviceNode);
        var project     = jdtWorkspace.getRoot().getProject(projectName);
        var file        = project.getFile(String.format("%s.java", serviceNode.model().name()));
        var cu          = JDTUtils.resolveCompilationUnit(file);
        try {
            cu.makeConsistent(null);
            var methods = cu.getTypes()[0].getMethods();
            for (var method : methods) {
                if (!method.isConstructor() && method.getElementName().equals(methodName)) {
                    return makeMethodInfo(method);
                }
            }
        } catch (Exception ex) {
            Log.warn("findServiceMethod: " + ex.getMessage());
        }
        return null;
    }

    private static ServiceMethodInfo makeMethodInfo(IMethod method) throws JavaModelException {
        var methodInfo = new ServiceMethodInfo();
        methodInfo.Name = method.getElementName();
        for (var para : method.getParameters()) {
            methodInfo.addParameter(para.getElementName(),
                    Signature.toString(para.getTypeSignature()));
        }
        return methodInfo;
    }

    //TODO: remove this
    private static int positionToOffset(IBuffer buffer, int line, int column) {
        if (line == 0) {
            return column;
        }

        int curLine = 0;
        for (int i = 0; i < buffer.getLength(); i++) {
            if (buffer.getChar(i) == '\n') {
                curLine++;
                if (curLine == line) {
                    return i + column + 1;
                }
            }
        }
        return -1;
    }
    //endregion
}
