package appbox.design.services.code;

import appbox.design.DesignHub;
import appbox.design.IDeveloperSession;
import appbox.design.jdt.DefaultVMType;
import appbox.design.jdt.Document;
import appbox.design.jdt.ModelFile;
import appbox.design.jdt.ModelWorkspace;
import appbox.design.tree.ModelNode;
import appbox.design.utils.ReflectUtil;
import appbox.logging.Log;
import appbox.model.ModelType;
import appbox.runtime.RuntimeContext;
import org.eclipse.core.internal.resources.ProjectPreferences;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.*;
import org.eclipse.jdt.internal.core.JavaModelCache;
import org.eclipse.jdt.internal.core.JavaModelManager;
import org.eclipse.jdt.internal.core.JavaModelStatus;
import org.eclipse.jdt.internal.core.search.indexing.IndexManager;
import org.eclipse.jdt.internal.launching.JREContainerInitializer;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.ls.core.internal.DocumentAdapter;
import org.eclipse.jdt.ls.core.internal.JDTUtils;
import org.eclipse.jdt.ls.core.internal.JVMConfigurator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * 一个TypeSystem对应一个实例，管理JavaProject及相应的虚拟文件
 */
public final class LanguageServer {
    private static final JREContainerInitializer jreContainerInitializer   = new JREContainerInitializer();
    private static final ProjectPreferences      defaultProjectPreferences = new ProjectPreferences();

    static {
        try {
            //init defaut java runtime
            DefaultVMType.init();
            //hack JAVA_LIKE_EXTENSIONS
            char[][] extensions = new char[1][];
            extensions[0] = "java".toCharArray();
            ReflectUtil.setField(org.eclipse.jdt.internal.core.util.Util.class, "JAVA_LIKE_EXTENSIONS", null, extensions);
            //hack JavaModelManager //TODO:*** 暂共用JavaModelManager
            var indexPath    = new Path(System.getProperty("java.io.tmpdir")).append("appbox_index_data");
            var indexManager = new IndexManager(indexPath);
            ReflectUtil.setField(JavaModelManager.class, "indexManager", JavaModelManager.getJavaModelManager(), indexManager);
            ReflectUtil.setField(JavaModelManager.class, "cache", JavaModelManager.getJavaModelManager(), new JavaModelCache());
            //JavaModelManager.getJavaModelManager().initializePreferences(); //需要ResourcesPlugin.getPlugin()
            JavaModelManager.getJavaModelManager().containerInitializersCache.put(JavaRuntime.JRE_CONTAINER, jreContainerInitializer);
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
    }

    public final  ModelWorkspace          jdtWorkspace;
    private final HashMap<Long, Document> openedFiles = new HashMap<>();

    public LanguageServer() {
        jdtWorkspace = new ModelWorkspace();
        //TODO:如果不能共用JavaModelManager,在这里初始化
    }

    //region ====create XXX====
    protected IProject createProject(String name) throws Exception {
        //TODO:check exists
        var project = jdtWorkspace.getRoot().getProject(name);
        project.create(null);
        //project.open(null);

        var perProjectInfo = JavaModelManager.getJavaModelManager().getPerProjectInfo(project, true);
        perProjectInfo.preferences = defaultProjectPreferences;

        var javaProject = JavaCore.create(project);
        JVMConfigurator.configureJVMSettings(javaProject, JavaRuntime.getDefaultVMInstall());

        IClasspathEntry[] buildPath = {
                JavaCore.newSourceEntry(project.getFullPath()),
                JavaRuntime.getDefaultJREContainerEntry()
        };

        var outPath = project.getFullPath().append("bin");
        perProjectInfo.setRawClasspath(buildPath, outPath, JavaModelStatus.VERIFIED_OK);

        return project;
    }
    //endregion

    //region ====open/close/edit Document====
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

    public List<CompletionProposal> completion(Document doc, int line, int column, String wordToComplete) {
        //TODO:暂简单实现
        var offset = doc.getOffset(line, column);
        var cu     = JDTUtils.resolveCompilationUnit((IFile) doc.getUnderlyingResource());

        var list = new ArrayList<CompletionProposal>();
        try {
            cu.codeComplete(offset, new CompletionRequestor() {
                @Override
                public void accept(CompletionProposal completionProposal) {
                    list.add(completionProposal);
                }
            });
        } catch (Exception ex) {
            Log.warn("代码完成错误: " + ex.getMessage());
        }
        return list;
    }

}
