package appbox.model;

import appbox.serialization.BinDeserializer;
import appbox.serialization.IBinSerializable;
import appbox.serialization.IOutputStream;
import appbox.utils.StringUtil;

public final class ApplicationModel implements IBinSerializable {
    private int    _id;
    private String _owner;
    private String _name;
    private byte   _storeId;        //映射至系统存储的编号，由EntityStore生成
    private int    _devModelIdSeq;  //仅用于导入导出，注意导出前需要从存储刷新

    /**
     * Only for serialization
     */
    public ApplicationModel() {
    }

    public ApplicationModel(String owner, String name) {
        _owner = owner;
        _name  = name;
        _id    = StringUtil.getHashCode(owner) ^ StringUtil.getHashCode(name);
    }

    public int id() {
        return _id;
    }

    public String name() {
        return _name;
    }

    public byte getAppStoreId() {
        return _storeId;
    }

    public void setAppStoreId(byte id) {
        _storeId = id;
    }

    public void setDevModelIdSeq(int seq) {
        _devModelIdSeq = seq;
    }

    //region ====IBinSerializable====
    @Override
    public void writeTo(IOutputStream bs) {
        bs.writeIntField(_id, 1);
        bs.writeVariantField(_devModelIdSeq, 2);
        bs.writeStringField(_owner, 3);
        bs.writeStringField(_name, 4);
        bs.finishWriteFields();
    }

    @Override
    public void readFrom(BinDeserializer bs) {
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
                    throw new RuntimeException("Unknown field id: " + fieldId);
            }
        } while (fieldId != 0);
    }
    //endregion
}
