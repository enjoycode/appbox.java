package appbox.design.lang.java;

import appbox.design.DesignHub;
import appbox.design.lang.java.jdt.*;
import appbox.design.lang.java.lsp.*;
import appbox.design.lang.java.code.ServiceMethodInfo;
import appbox.design.lang.java.utils.ModelTypeUtil;
import appbox.design.tree.ModelNode;
import appbox.design.utils.CodeHelper;
import appbox.design.utils.PathUtil;
import appbox.logging.Log;

import appbox.model.ModelType;
import appbox.model.ServiceModel;
import appbox.runtime.IService;
import appbox.store.SqlStore;
import appbox.store.utils.AssemblyUtil;
import com.ea.async.Async;
import org.eclipse.core.internal.resources.ProjectPreferences;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.*;
import org.eclipse.jdt.internal.core.*;
import org.eclipse.jdt.internal.launching.JREContainerInitializer;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.ls.core.internal.*;
import org.eclipse.jdt.ls.core.internal.DocumentAdapter;
import org.eclipse.jdt.ls.core.internal.contentassist.CompletionProposalRequestor;
import org.eclipse.jdt.ls.core.internal.preferences.PreferenceManager;
import org.eclipse.jdt.ls.core.internal.syntaxserver.ModelBasedCompletionEngine;
import org.eclipse.lsp4j.*;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

/**
 * 一个TypeSystem对应一个实例，管理JavaProject及相应的虚拟文件
 */
public final class JdtLanguageServer {
    //region ====static====
    public static final String BUILD_OUTPUT = "bin";

    public static final IPath libEA_AsyncPath    =
            new Path(Async.class.getProtectionDomain().getCodeSource().getLocation().getPath());
    public static final IPath libAppBoxCorePath  =
            new Path(IService.class.getProtectionDomain().getCodeSource().getLocation().getPath());
    public static final IPath libAppBoxStorePath =
            new Path(SqlStore.class.getProtectionDomain().getCodeSource().getLocation().getPath());

    private static final JREContainerInitializer jreContainerInitializer   = new JREContainerInitializer();
    private static final ProjectPreferences      defaultProjectPreferences = new ProjectPreferences();
    private static final PreferenceManager       lsPreferenceManager; //TODO:move out

    static {
        PreferenceManager preferenceManager;
        try {
            preferenceManager = JdtHacker.hack(jreContainerInitializer, defaultProjectPreferences);
        } catch (Exception ex) {
            preferenceManager = null;
            ex.printStackTrace();
        }
        lsPreferenceManager = preferenceManager;
    }
    //endregion

    public static final ModelWorkspace jdtWorkspace = (ModelWorkspace) ResourcesPlugin.getWorkspace();

    public final  DesignHub               hub;
    public final  ModelFilesManager       filesManager;
    public final  ModelSymbolFinder       symbolFinder;
    private final HashMap<Long, Document> openedFiles = new HashMap<>();
    /** 实体、枚举等通用模型项目 */
    IProject modelsProject;

    public JdtLanguageServer(DesignHub hub) {
        this.hub     = hub;
        filesManager = new ModelFilesManager(this);
        symbolFinder = new ModelSymbolFinder(this);
    }

    /** 用于初始化通用项目等 */
    public void init() {
        try {
            //创建通用模型虚拟工程
            final var libs = new IClasspathEntry[]{
                    JavaCore.newLibraryEntry(libAppBoxCorePath, null, null)
            };
            final var modelsProjectName = makeModelsProjectName();
            modelsProject = createProject(ModelProject.ModelProjectType.Models, modelsProjectName, libs);
            //添加基础虚拟文件,从resources中加载
            final var sysFolder = modelsProject.getFolder("sys");
            sysFolder.create(true, true, null);
            ModelFilesManager.createDummyFiles(sysFolder, ModelFilesManager.SYS_DUMMY_FILES);
            ModelFilesManager.createDummyFiles(modelsProject, ModelFilesManager.ROOT_DUMMY_FILES);

            //TODO:考虑创建单独服务代理项目,目前服务代理均放在ModelsProject内
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public IProject getModelsProject() {return modelsProject;}

    //region ====Project Management====
    private String makeModelsProjectName() {
        return String.format("%s_models", hub.session.name());
    }

    public String makeServiceProjectName(ModelNode serviceNode) {
        return String.format("%s_%s", hub.session.name(), Long.toUnsignedString(serviceNode.model().id()));
    }

    public String makeRuntimeProjectName(ModelNode serviceNode) {
        return String.format("%s_runtime_%s", hub.session.name(), Long.toUnsignedString(serviceNode.model().id()));
    }

    private long getModelIdFromServiceProjectName(String projectName) {
        final var modelIdString = projectName.replace(hub.session.name() + "_", "");
        return Long.parseUnsignedLong(modelIdString);
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
    public IProject createProject(ModelProject.ModelProjectType type, String name, IClasspathEntry[] deps) throws Exception {
        final var project = (ModelProject) jdtWorkspace.getRoot().getProject(name);
        project.create(null);
        project.setProjectTypeAndDesignContext(type, hub);
        project.open(null); //always open it

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
    private void updateServiceReferences(ModelNode node, IClasspathEntry[] deps) {
        final var prjName = makeServiceProjectName(node);
        final var project = jdtWorkspace.getRoot().getProject(prjName);
        final var perProjectInfo = JavaModelManager.getJavaModelManager()
                .getPerProjectInfo(project, false);
        final var buildPath = makeBuildPaths(project, deps);
        perProjectInfo.setRawClasspath(buildPath, perProjectInfo.outputLocation, JavaModelStatus.VERIFIED_OK);
    }

    public CompletableFuture<Void> updateServiceReferences(ModelNode serviceNode) {
        final var serviceModel = (ServiceModel) serviceNode.model();
        //先加载解压缩第三方类库
        return AssemblyUtil.extractService3rdLibs(serviceNode.appNode.model.name(), serviceModel.getReferences())
                .thenAccept(r -> {
                    //再更新虚拟工程
                    final var libs = makeServiceProjectDeps(serviceNode, false);
                    updateServiceReferences(serviceNode, libs);
                });
    }

    /** 创建服务模型虚拟工程的依赖项,包括内置及第三方,但不包括JRE及源码 */
    public IClasspathEntry[] makeServiceProjectDeps(ModelNode serviceNode, boolean forRuntime) {
        final var serviceModel = (ServiceModel) serviceNode.model();
        final var appName      = serviceNode.appNode.model.name();

        final int baseCount = forRuntime ? 3 : 2;
        int       depsCount = baseCount;
        if (serviceModel.hasReference()) {
            depsCount += serviceModel.getReferences().size();
        }
        IClasspathEntry[] deps = new IClasspathEntry[depsCount];
        if (forRuntime) {
            deps[0] = JavaCore.newLibraryEntry(libEA_AsyncPath, null, null);
            deps[1] = JavaCore.newLibraryEntry(libAppBoxCorePath, null, null);
            deps[2] = JavaCore.newLibraryEntry(libAppBoxStorePath, null, null);
        } else {
            deps[0] = JavaCore.newLibraryEntry(libAppBoxCorePath, null, null);
            deps[1] = JavaCore.newProjectEntry(modelsProject.getFullPath());
        }

        //处理服务模型引用的第三方包
        for (int i = baseCount; i < depsCount; i++) {
            final var libPath = new Path(java.nio.file.Path.of(PathUtil.LIB_PATH, appName,
                    serviceModel.getReferences().get(i - baseCount)).toString());
            deps[i] = JavaCore.newLibraryEntry(libPath, null, null);
        }
        return deps;
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

    //region ====LSP Methods====
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

    public SignatureHelp signatureHelp(Document doc, int line, int column) {
        final var offset = doc.getOffset(line, column);
        final var cu     = JDTUtils.resolveCompilationUnit((IFile) doc.getUnderlyingResource());
        return SignatureHelpHandler.signatureHelp(cu, offset, new ProgressMonitor());
    }

    public List<? extends Location> definition(Document doc, int line, int column) {
        final var cu = JDTUtils.resolveCompilationUnit((IFile) doc.getUnderlyingResource());
        return NavigateToDefinitionHandler.definition(cu, line, column, lsPreferenceManager, new ProgressMonitor());
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

    public List<DocumentSymbol> documentSymbol(Document doc) {
        final var cu = JDTUtils.resolveCompilationUnit((IFile) doc.getUnderlyingResource());
        return DocumentSymbolHandler.getHierarchicalOutline(cu, null);
    }

    public List<TextEdit> formatting(Document doc) {
        final var cu            = JDTUtils.resolveCompilationUnit((IFile) doc.getUnderlyingResource());
        final var formatOptions = new FormattingOptions(4, true);
        return FormatterHandler.format(cu, formatOptions, null, new ProgressMonitor());
    }
    //endregion

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

    public ModelNode findModelNodeByModelFile(ModelFile file) {
        //TODO:暂简单处理路径
        var fileName = file.getName();
        fileName = fileName.substring(0, fileName.length() - 5); //去掉扩展名

        final var project = (ModelProject) file.getProject();
        if (project.getProjectType() == ModelProject.ModelProjectType.Models) {
            final var typeFolder = file.getParent();
            final var appFolder  = typeFolder.getParent();
            final var appNode    = hub.designTree.findApplicationNodeByName(appFolder.getName());
            final var modelType  = CodeHelper.getModelTypeFromLCC(typeFolder.getName());
            return hub.designTree.findModelNodeByName(appNode.model.id(), modelType, fileName);
        } else if (project.getProjectType() == ModelProject.ModelProjectType.DesigntimeService) {
            final var modelId = getModelIdFromServiceProjectName(project.getName());
            return hub.designTree.findModelNode(modelId);
        } else {
            throw new RuntimeException("Not supported");
        }
    }

    public ModelNode findModelNodeByUri(String uri) {
        //eg: file://Admin_models/sys/entities/Employee.java
        //or  file://Admin_[ServiceId]/OrderService.java
        final var segements = uri.split("/");
        if (segements.length == 6) { //from models project
            final var appNode   = hub.designTree.findApplicationNodeByName(segements[3]);
            final var modelType = ModelTypeUtil.fromLowercaseType(segements[4]);
            final var modelName = segements[5].replace(".java", "");
            return hub.designTree.findModelNodeByName(appNode.model.id(), modelType, modelName);
        } else if (segements.length == 4) { //from design service
            final var modelId = getModelIdFromServiceProjectName(segements[2]);
            return hub.designTree.findModelNode(modelId);
        } else {
            throw new RuntimeException("Unknown uri: " + uri);
        }
    }

    /** 找到服务模型对应的虚拟文件 */
    public ModelFile findFileForServiceModel(ModelNode serviceNode) {
        final var fileName    = String.format("%s.java", serviceNode.model().name());
        final var projectName = makeServiceProjectName(serviceNode);
        final var project     = jdtWorkspace.getRoot().getProject(projectName);
        return (ModelFile) project.findMember(fileName);
    }

    /** 从通用项目内查找模型对应的虚拟文件 */
    public ModelFile findFileFromModelsProject(ModelType type, String appName, String modelName) {
        final var typeName = ModelTypeUtil.toLowercaseTypeName(type);
        final var fileName = String.format("%s.java", modelName);
        return (ModelFile) modelsProject.getFolder(appName).getFolder(typeName).getFile(fileName);
    }
    //endregion
}
