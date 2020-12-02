package appbox.design.handlers;

import appbox.data.JsonResult;
import appbox.design.DesignHub;
import appbox.design.services.StagedService;
import appbox.model.ModelBase;
import appbox.model.ModelFolder;
import appbox.model.ModelType;
import appbox.runtime.InvokeArg;
import com.alibaba.fastjson.annotation.JSONField;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/** 发布时获取所有变更 */
public final class GetPendingChanges implements IRequestHandler {

    @Override
    public CompletableFuture<Object> handle(DesignHub hub, List<InvokeArg> args) {
        //暂重新加载
        return StagedService.loadStagedAsync(false)
                .thenApply(staged -> {
                    hub.pendingChanges = staged.getItems();
                    if (hub.pendingChanges == null || hub.pendingChanges.length == 0)
                        return null;
                    var list = new ArrayList<ChangedInfo>();
                    for (var item : hub.pendingChanges) {
                        //TODO:其他类型处理
                        if (item instanceof ModelBase) {
                            var model  = (ModelBase) item;
                            var change = new ChangedInfo();
                            change.modelType = model.modelType().name();
                            change.modelId   = model.name();
                            list.add(change);
                        } else if (item instanceof ModelFolder) {
                            var folder = (ModelFolder) item;
                            var change = new ChangedInfo();
                            change.modelType = ModelType.Folder.name();
                            change.modelId   = folder.getTargetModelType().name();
                            list.add(change);
                        }
                    }
                    return new JsonResult(list);
                });
    }

    static final class ChangedInfo {
        @JSONField(name = "ModelType")
        public String modelType;
        @JSONField(name = "ModelID")
        public String modelId;
    }

}
