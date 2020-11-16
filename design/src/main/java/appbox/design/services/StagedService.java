package appbox.design.services;

import appbox.entities.StagedModel;
import appbox.logging.Log;
import appbox.model.EntityModel;
import appbox.runtime.RuntimeContext;
import appbox.store.EntityStore;
import appbox.store.query.TableScan;
import appbox.store.utils.ModelCodeUtil;
import appbox.utils.IdUtil;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

public final class StagedService {
    enum StagedType {
        Model(0),           //模型序列化数据
        Folder(1),          //文件夹
        SourceCode(2),      //服务模型或视图模型的源代码 //TODO:考虑按类型分开
        ViewRuntimeCode(3); //仅用于视图模型前端编译好的运行时脚本代码

        public final byte value;

        StagedType(int value) {
            this.value = (byte) value;
        }
    }

    private static CompletableFuture<Void> saveAsync(StagedType type, long modelId, byte[] data) {
        var         developerId = RuntimeContext.current().currentSession().leafOrgUnitId();
        EntityModel model       = RuntimeContext.current().getModel(IdUtil.SYS_STAGED_MODEL_ID);

        String modelIdString = Long.toUnsignedString(modelId); //转换为字符串

        //TODO:暂采用先读取再插入的方式，待实现KVUpsert后改写
        var q = new TableScan<>(IdUtil.SYS_STAGED_MODEL_ID, StagedModel.class);
        q.where(StagedModel.TYPE.eq(type.value)
                .and(StagedModel.MODEL.eq(modelIdString))
                .and(StagedModel.DEVELOPER.eq(developerId))
        );
        return q.toListAsync().thenApply(list -> {
            if (list.size() > 1) Log.warn("Detected multi row");
            StagedModel stagedItem = list.size() > 0 ? list.get(0) : new StagedModel();
            stagedItem.setType(type.value);
            stagedItem.setModelId(modelIdString);
            stagedItem.setDeveloperId(developerId);
            stagedItem.setData(data);
            return stagedItem;
        }).thenCompose(item -> EntityStore.insertEntityAsync(item, true));
    }

    private static CompletableFuture<String> loadServiceCode(long serviceModelId) {
        var developerID = RuntimeContext.current().currentSession().leafOrgUnitId();

        var q = new TableScan<>(IdUtil.SYS_STAGED_MODEL_ID,StagedModel.class);
        q.where(StagedModel.TYPE.eq(StagedType.SourceCode.value)
                        .and(StagedModel.MODEL.eq(Long.toUnsignedString(serviceModelId)))
                        .and(StagedModel.DEVELOPER.eq(developerID)));

        return q.toListAsync().thenApply(res -> {
            if (res == null || res.size() <= 0)
                return null;

            try {
                return ModelCodeUtil.decodeServiceCode(res.get(0).getData()).sourceCode;
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        });
    }
}
