package appbox.design;

import appbox.design.services.code.TypeSystem;
import appbox.design.tree.DesignTree;
import appbox.runtime.ISessionInfo;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 每个在线开发者对应一个DesignHub实例
 */
public final class DesignHub { //TODO: rename to DesignContext
    public final DesignTree designTree;
    public final TypeSystem typeSystem;
    public final ISessionInfo session;

    //TODO:临时方案用于顺序执行代码编辑相关任务，如ChangeBuffer, GetCompletion等，待实现支持取消的任务队列
    public final ExecutorService codeEditorTaskPool = Executors.newSingleThreadExecutor(); //Executors.newFixedThreadPool(1);

    public DesignHub(ISessionInfo session) {
        this.session = session;
        typeSystem = new TypeSystem(this);
        designTree = new DesignTree(this);
    }

}
