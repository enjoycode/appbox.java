package appbox.design;

import appbox.design.lang.dart.DartLanguageServer;
import appbox.design.lang.java.debug.DebugService;
import appbox.design.lang.TypeSystem;
import appbox.design.tree.DesignTree;
import appbox.model.ApplicationModel;
import appbox.model.EntityModel;
import appbox.model.ModelType;

import java.util.concurrent.*;

/** 每个在线开发者对应一个DesignHub实例 */
public final class DesignHub implements IDesignContext { //TODO: rename to DesignContext
    public final DesignTree        designTree;
    public final TypeSystem        typeSystem;
    public final IDeveloperSession session;
    private      DebugService      _debugService;
    private      boolean           _isFlutterIDE = false; //是否新版基于Flutter的IDE

    /** 用于发布时暂存挂起的修改 */
    public Object[] pendingChanges;

    //TODO:临时方案用于顺序执行代码编辑相关任务，如ChangeBuffer, GetCompletion等，待实现支持取消的任务队列
    public final ExecutorService codeEditorTaskPool = new ThreadPoolExecutor(1, 1,
            0L, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<Runnable>());

    public DesignHub(IDeveloperSession session) {
        this.session = session;
        typeSystem   = new TypeSystem(this);
        designTree   = new DesignTree(this);
    }

    public void setIDE(boolean isFlutterIDE) {
        _isFlutterIDE = isFlutterIDE;

        if (_isFlutterIDE && typeSystem.dartLanguageServer == null) {
            //TODO:考虑延迟dartLanguageServer初始化与启动
            typeSystem.dartLanguageServer = new DartLanguageServer(this, session instanceof MockDeveloperSession);
        }
    }

    public boolean isFlutterIDE() {
        return _isFlutterIDE;
    }

    public void dispose() {
        //TODO:清理调试服务

        typeSystem.javaLanguageServer.dispose();

        if (_isFlutterIDE && typeSystem.dartLanguageServer != null)
            typeSystem.dartLanguageServer.stop();
    }

    public synchronized DebugService debugService() {
        if (_debugService == null)
            _debugService = new DebugService(this);
        return _debugService;
    }

    //region ====IDesignContext====
    @Override
    public ApplicationModel getApplicationModel(int appId) {
        return designTree.findApplicationNode(appId).model;
    }

    @Override
    public EntityModel getEntityModel(long modelId) {
        var modelNode = designTree.findModelNode(ModelType.Entity, modelId);
        if (modelNode == null)
            throw new RuntimeException("Can't find EntityModel");
        return (EntityModel) modelNode.model();
    }
    //endregion

}
