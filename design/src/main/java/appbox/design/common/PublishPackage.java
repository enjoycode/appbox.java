package appbox.design.common;

import appbox.data.PersistentState;
import appbox.model.EntityModel;
import appbox.model.ModelBase;
import appbox.model.ModelFolder;
import appbox.model.ModelType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 用于发布的模型包，支持依赖排序 */
public final class PublishPackage {
    public final List<ModelBase>     models            = new ArrayList<>();
    /** 需要保存或删除的模型根文件夹 */
    public final List<ModelFolder>   folders           = new ArrayList<>();
    /** 新建或更新的模型的虚拟代码，Key=ModelId */
    public final Map<Long, byte[]>   sourceCodes       = new HashMap<>();
    /** 新建或更新的编译好的服务组件, Key=xxx.XXXX */
    public final Map<String, byte[]> serviceAssemblies = new HashMap<>();
    /** 新建或更新的视图组件, Key=xxx.XXXX */
    public final Map<String, byte[]> viewAssemblies    = new HashMap<>();

    /** 根据引用依赖关系排序 */
    public void sortAllModels() {
        models.sort((a, b) -> {
            //先将标为删除的排在前面
            if (a.persistentState() == PersistentState.Deleted
                    && b.persistentState() != PersistentState.Deleted)
                return -1;
            if (a.persistentState() != PersistentState.Deleted
                    && b.persistentState() == PersistentState.Deleted)
                return 1;
            //后面根据类型及依赖关系排序
            if (a.modelType() != b.modelType())
                return a.modelType().compareTo(b.modelType());
            if (a.modelType() == ModelType.Entity) {
                //注意如果都标为删除需要倒序
                if (a.persistentState() == PersistentState.Deleted)
                    return ((EntityModel) b).compareTo((EntityModel) a);
                return ((EntityModel) a).compareTo((EntityModel) b);
            }
            return a.name().compareTo(b.name());
        });
    }

}
