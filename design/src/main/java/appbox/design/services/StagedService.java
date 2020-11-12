package appbox.design.services;

import appbox.model.EntityModel;
import appbox.runtime.RuntimeContext;
import appbox.utils.IdUtil;

import java.util.concurrent.CompletableFuture;

public final class StagedService {
    enum StagedType {
        Model(0),           //模型序列化数据
        Folder(1),          //文件夹
        SourceCode(2),      //服务模型或视图模型的源代码 //TODO:考虑按类型分开
        ViewRuntimeCode(3); //仅用于视图模型前端编译好的运行时脚本代码

        public final byte value;

        StagedType(int value) {
            this.value = (byte)value;
        }
    }

    private static CompletableFuture<Void> saveAsync(StagedType type, long modelId, byte[] data) {
        var         developerId = RuntimeContext.current().currentSession().leafOrgUnitId();
        EntityModel model       = RuntimeContext.current().getModel(IdUtil.SYS_STAGED_MODEL_ID);

        throw new RuntimeException("未实现");
    }

}
