package appbox.model;

import appbox.data.EntityId;
import appbox.serialization.IInputStream;
import appbox.serialization.IOutputStream;

import java.util.ArrayList;
import java.util.List;

/** 权限模型用于关联受控的资源与授权的组织单元 */
public final class PermissionModel extends ModelBase {

    private List<EntityId> _orgUnits; //授权的组织单元
    private int            _sortNum;  //用于排序
    private String         _remark;   //备注

    public PermissionModel() {}

    public PermissionModel(long id, String name) {
        super(id, name);
    }

    @Override
    public ModelType modelType() {
        return ModelType.Permission;
    }

    public List<EntityId> orgUnits() {
        if (_orgUnits == null)
            _orgUnits = new ArrayList<>();
        return _orgUnits;
    }

    public int sortNum() { return _sortNum; }

    public void setSortNum(int sort) { _sortNum = sort; }

    public String remark() { return _remark; }

    public void setRemark(String remark) { _remark = remark; }

    //region ====Serialization====
    @Override
    public void writeTo(IOutputStream bs) {
        super.writeTo(bs);

        bs.writeIntField(_sortNum, 1);
        if (_orgUnits != null && _orgUnits.size() > 0)
            bs.writeList(_orgUnits, 2, false);
        bs.writeStringField(_remark, 3);

        bs.finishWriteFields();
    }

    @Override
    public void readFrom(IInputStream bs) {
        super.readFrom(bs);

        int propIndex;
        do {
            propIndex = bs.readVariant();
            switch (propIndex) {
                case 1:
                    _sortNum = bs.readInt(); break;
                case 2:
                    _orgUnits = bs.readList(() -> new EntityId(), false); break;
                case 3:
                    _remark = bs.readString(); break;
                case 0:
                    break;
                default:
                    throw new RuntimeException("Unknown field id: " + propIndex);
            }
        } while (propIndex != 0);
    }
    //endregion

}
