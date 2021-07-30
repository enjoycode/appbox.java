package appbox.model;

import appbox.serialization.IInputStream;
import appbox.serialization.IOutputStream;

import java.util.List;

public final class ServiceModel extends ModelBase {
    //region ====Enum Language====
    public enum Language {
        Java(1), CSharp(0);

        public final byte value;

        Language(int v) {
            value = (byte) v;
        }

        public static Language fromValue(byte v) {
            return v == 1 ? Java : CSharp;
        }
    }
    //endregion

    private Language     _language;   //开发语言
    private List<String> _references; //第三方包

    /** Only for serialization */
    public ServiceModel() {}

    public ServiceModel(long id, String name) {
        super(id, name);

        _language = Language.Java;
    }

    @Override
    public ModelType modelType() {
        return ModelType.Service;
    }

    public Language language() {
        return _language;
    }

    public boolean hasReference() {
        return _references != null && _references.size() > 0;
    }

    public List<String> getReferences() {
        return _references;
    }

    public void setReferences(List<String> newRefs) {
        _references = newRefs;
    }

    //region ====Serialization====
    @Override
    public void writeTo(IOutputStream bs) {
        super.writeTo(bs);

        if (hasReference()) {
            bs.writeVariant(1);
            bs.writeListString(_references);
        }

        bs.writeByteField(_language.value, 2);

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
                    _references = bs.readListString();
                    break;
                case 2:
                    _language = Language.fromValue(bs.readByte());
                    break;
                case 0:
                    break;
                default:
                    throw new RuntimeException("Unknown field id: " + propIndex);
            }
        } while (propIndex != 0);
    }
    //endregion

}
