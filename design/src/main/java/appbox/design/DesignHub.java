package appbox.design;

import appbox.design.services.code.TypeSystem;
import appbox.design.tree.DesignTree;
import appbox.runtime.ISessionInfo;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class DesignHub {
    public final DesignTree designTree;
    public final TypeSystem typeSystem;

    //TODO:临时方案用于顺序执行代码编辑相关任务，如ChangeBuffer, GetCompletion等，待实现支持取消的任务队列
    public final ExecutorService codeEditorTaskPool = Executors.newSingleThreadExecutor(); //Executors.newFixedThreadPool(1);

    public DesignHub(ISessionInfo session) {
        designTree = new DesignTree(this);
        typeSystem = new TypeSystem();
    }

}
