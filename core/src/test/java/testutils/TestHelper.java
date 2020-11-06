package testutils;

import appbox.model.EntityModel;
import appbox.model.entity.DataFieldModel;
import appbox.model.entity.FieldWithOrder;
import appbox.model.entity.SysIndexModel;
import appbox.serialization.BinDeserializer;
import appbox.serialization.BinSerializer;
import appbox.serialization.BytesInputStream;
import appbox.serialization.BytesOutputStream;
import appbox.utils.IdUtil;

public final class TestHelper {

    public static EntityModel makeEntityModel() {
        try {
            var nameId = (short) (1 << IdUtil.MEMBERID_SEQ_OFFSET);
            var model  = new EntityModel(IdUtil.SYS_EMPLOEE_MODEL_ID, "Emploee", true, false);
            var name   = new DataFieldModel(model, "Name", DataFieldModel.DataFieldType.String, false, false);
            model.addSysMember(name, nameId);
            var ui_name = new SysIndexModel(model, "UI_Name", true,
                    new FieldWithOrder[]{new FieldWithOrder(nameId)}, null);
            model.sysStoreOptions().addSysIndex(model, ui_name, (byte) ((1 << IdUtil.INDEXID_UNIQUE_OFFSET) | (1 << 2)));
            return model;
        } catch (Exception e) {
            return null;
        }
    }

    public static void serializeTo(Object obj, BytesOutputStream output) throws Exception {
        var bs = BinSerializer.rentFromPool(output);
        bs.serialize(obj);
        BinSerializer.backToPool(bs);
    }

    public static Object deserializeFrom(BytesInputStream input) throws Exception {
        var bs  = BinDeserializer.rentFromPool(input);
        var res = bs.deserialize();
        BinDeserializer.backToPool(bs);
        return res;
    }

}
