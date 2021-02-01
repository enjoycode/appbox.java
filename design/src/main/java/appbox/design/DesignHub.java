package appbox.design;

import appbox.design.services.debug.DebugService;
import appbox.design.services.code.TypeSystem;
import appbox.design.tree.DesignTree;
import appbox.model.ApplicationModel;
import appbox.model.EntityModel;
import appbox.model.ModelType;
import appbox.runtime.ISessionInfo;

import java.util.concurrent.*;

/**
 * 每个在线开发者对应一个DesignHub实例
 */
public final class DesignHub implements IDesignContext { //TODO: rename to DesignContext
    public final DesignTree   designTree;
    public final TypeSystem   typeSystem;
    public final ISessionInfo session;
    private      DebugService _debugService;

    /** 用于发布时暂存挂起的修改 */
    public Object[] pendingChanges;

    //TODO:临时方案用于顺序执行代码编辑相关任务，如ChangeBuffer, GetCompletion等，待实现支持取消的任务队列
    public final ExecutorService codeEditorTaskPool = new ThreadPoolExecutor(1, 1,
            0L, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<Runnable>());

    public DesignHub(ISessionInfo session) {
        this.session = session;
        typeSystem   = new TypeSystem(this);
        designTree   = new DesignTree(this);
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
