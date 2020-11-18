package appbox.model;

import appbox.serialization.BinDeserializer;
import appbox.serialization.BinSerializer;

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

    private Language _language;

    /**
     * Only for serialization
     */
    public ServiceModel() {
    }

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

    //region ====Serialization====
    @Override
    public void writeTo(BinSerializer bs) {
        super.writeTo(bs);

        bs.writeByte(_language.value, 2);

        bs.finishWriteFields();
    }

    @Override
    public void readFrom(BinDeserializer bs) {
        super.readFrom(bs);

        int propIndex;
        do {
            propIndex = bs.readVariant();
            switch (propIndex) {
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
