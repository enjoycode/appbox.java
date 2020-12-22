package appbox.model;

import java.util.ArrayList;
import java.util.List;

public class EnumModel extends ModelBase{

    //region ====Fields & Properties====
    public ModelType modelType = ModelType.Enum;

    public boolean isFlag;

    public String comment;

    public List<EnumModelItem> items = new ArrayList<EnumModelItem>();

    public EnumModel() {
    }

    public EnumModel(long modelId, String name) {
        super(modelId, name);
    }
    //endregion

    //region ====Ctor====
    @Override
    public ModelType modelType() {
        return modelType;
    }
    //endregion

    public ModelType getModelType() {
        return modelType;
    }

    public void setModelType(ModelType modelType) {
        this.modelType = modelType;
    }

    public boolean isFlag() {
        return isFlag;
    }

    public void setFlag(boolean flag) {
        isFlag = flag;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public List<EnumModelItem> getItems() {
        return items;
    }

    public void setItems(List<EnumModelItem> items) {
        this.items = items;
    }
}
