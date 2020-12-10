package testutils;

import appbox.model.EntityModel;
import appbox.model.entity.DataFieldModel;
import appbox.model.entity.FieldWithOrder;
import appbox.model.entity.SysIndexModel;
import appbox.utils.IdUtil;

public final class TestHelper {

    public static EntityModel makeEntityModel() {
        try {
            var nameId = (short) (1 << IdUtil.MEMBERID_SEQ_OFFSET);
            var model  = new EntityModel(IdUtil.SYS_EMPLOYEE_MODEL_ID, "Emploee");
            model.bindToSysStore(true, false);
            var name = new DataFieldModel(model, "Name", DataFieldModel.DataFieldType.String, false, false);
            model.addSysMember(name, nameId);
            var ui_name = new SysIndexModel(model, "UI_Name", true,
                    new FieldWithOrder[]{new FieldWithOrder(nameId)}, null);
            model.sysStoreOptions().addSysIndex(model, ui_name, (byte) ((1 << IdUtil.INDEXID_UNIQUE_OFFSET) | (1 << 2)));
            return model;
        } catch (Exception e) {
            return null;
        }
    }

}
