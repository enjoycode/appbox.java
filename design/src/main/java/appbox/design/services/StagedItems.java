package appbox.design.services;

import appbox.data.PersistentState;
import appbox.design.services.StagedService.StagedType;
import appbox.entities.StagedModel;
import appbox.model.ModelBase;
import appbox.model.ModelFolder;
import appbox.serialization.IBinSerializable;

import java.util.ArrayList;
import java.util.List;

public final class StagedItems {
    private Object[] items;

    public StagedItems(List<StagedModel> staged) {
        if (staged != null && staged.size() > 0) {
            items = new Object[staged.size()];
            for (int i = 0; i < staged.size(); i++) {
                var data = staged.get(i).getData();
                var type = StagedType.from(staged.get(i).getType());
                switch (type) {
                    case Model:
                    case Folder:
                        items[i] = IBinSerializable.deserialize(data);
                        break;
                    case SourceCode:
                        var modelId1 = Long.parseUnsignedLong(staged.get(i).getModelId());
                        items[i] = new StagedSourceCode(modelId1, data);
                        break;
                    case ViewRuntimeCode:
                        var modelId2 = Long.parseUnsignedLong(staged.get(i).getModelId());
                        items[i] = new StagedViewRuntimeCode(modelId2, data);
                        break;
                    default:
                        throw new RuntimeException("未实现");
                }
            }
        }
    }

    public boolean isEmpty() {
        return items == null || items.length == 0;
    }

    public Object[] getItems() {
        return items;
    }

    public ModelBase[] findNewModels() {
        var list = new ArrayList<ModelBase>();
        if (items != null) {
            for (Object item : items) {
                if (item instanceof ModelBase
                        && ((ModelBase) item).persistentState() == PersistentState.Detached) {
                    list.add((ModelBase) item);
                }
            }
        }
        var res = new ModelBase[list.size()];
        return list.toArray(res);
    }

    public ModelBase findModel(long modelId) {
        if (items != null) {
            for (int i = 0; i < items.length; i++) {
                if (items[i] instanceof ModelBase
                        && ((ModelBase) items[i]).id() == modelId) {
                    return (ModelBase) items[i];
                }
            }
        }
        return null;
    }

    /** 用挂起的文件夹更新从存储加载的文件夹 */
    public void updateFolders(List<ModelFolder> storedFolders) {
        if (items == null) return;
        for (Object item : items) {
            if (item instanceof ModelFolder) {
                ModelFolder folder = (ModelFolder) item;
                int         index  = -1;
                for (ModelFolder t : storedFolders) {
                    if (t.getAppId() == folder.getAppId() && t.getTargetModelType() == folder.getTargetModelType()) {
                        index = 1;
                    }
                }
                if (index < 0) {
                    storedFolders.add(folder);
                } else {
                    storedFolders.add(index, folder);
                }
            }
        }
    }

    /** 从存储加载的模型中移除已删除的 */
    public void removeDeletedModels(List<ModelBase> storedModels) {
        if (items == null || items.length == 0)
            return;

        for (Object item : items) {
            if (item instanceof ModelBase
                    && ((ModelBase) item).persistentState() == PersistentState.Deleted) {
                var m = (ModelBase) item;
                storedModels.removeIf(t -> t.id() == m.id());
            }
        }
    }

    public static final class StagedSourceCode {
        public final long   ModelId;
        public final byte[] CodeData;

        public StagedSourceCode(long modelId, byte[] codeData) {
            this.ModelId  = modelId;
            this.CodeData = codeData;
        }
    }

    public static final class StagedViewRuntimeCode {
        public final long   ModelId;
        public final byte[] CodeData;

        public StagedViewRuntimeCode(long modelId, byte[] codeData) {
            this.ModelId  = modelId;
            this.CodeData = codeData;
        }
    }
}
