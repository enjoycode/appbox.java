package appbox.core.model;

import appbox.core.serialization.BinDeserializer;
import appbox.core.serialization.BinSerializer;
import appbox.core.serialization.IBinSerializable;
import appbox.core.utils.StringUtil;

public final class ApplicationModel implements IBinSerializable {
    private int    _id;
    private String _owner;
    private String _name;
    private byte   _storeId;        //映射至系统存储的编号，由EntityStore生成
    private int    _devModelIdSeq;  //仅用于导入导出，注意导出前需要从存储刷新

    public ApplicationModel(String owner, String name) {
        _owner = owner;
        _name  = name;
        _id    = StringUtil.getHashCode(owner) ^ StringUtil.getHashCode(name);
    }

    //region ====IBinSerializable====
    @Override
    public void writeTo(BinSerializer bs) throws Exception {
        bs.writeInt(_id, 1);
        bs.writeVariant(_devModelIdSeq, 2);
        bs.writeString(_owner, 3);
        bs.writeString(_name, 4);
        bs.finishWriteFields();
    }

    @Override
    public void readFrom(BinDeserializer bs) throws Exception {
        int fieldId;
        do {
            fieldId = bs.readVariant();
            switch (fieldId) {
                case 1:
                    _id = bs.readInt();
                    break;
                case 2:
                    _devModelIdSeq = bs.readVariant();
                    break;
                case 3:
                    _owner = bs.readString();
                    break;
                case 4:
                    _name = bs.readString();
                    break;
                case 0:
                    break;
                default:
                    throw new Exception("Unknown field id: " + fieldId);
            }
        } while (fieldId != 0);
    }
    //endregion
}
