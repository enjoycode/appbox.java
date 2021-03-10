package appbox.model;

import appbox.serialization.IInputStream;
import appbox.serialization.IOutputStream;
import appbox.utils.StringUtil;

public final class DataStoreModel extends ModelBase {

    private DataStoreKind _kind;
    private String        _provider;
    private String        _settings;
    //TODO:DataStoreNameRules

    public DataStoreModel() {}

    public DataStoreModel(DataStoreKind kind, String provider, String storeName) {
        super(StringUtil.getHashCode(storeName), storeName);
        _kind     = kind;
        _provider = provider;
    }

    public DataStoreKind kind() { return _kind; }

    public String provider() { return _provider; }

    public String settings() { return _settings; }

    public void updateSettings(String value) {
        _settings = value;
    }

    @Override
    public ModelType modelType() {
        return ModelType.DataStore;
    }

    //region ====Serialization====
    //注意:provider及settings的编号与C#不同
    @Override
    public void writeTo(IOutputStream bs) {
        super.writeTo(bs);

        bs.writeByteField(_kind.value, 1);
        bs.writeStringField(_provider, 5);
        bs.writeStringField(_settings, 6);

        bs.finishWriteFields();
    }

    @Override
    public void readFrom(IInputStream bs) {
        super.readFrom(bs);

        int fieldId;
        do {
            fieldId = bs.readVariant();
            switch (fieldId) {
                case 1:
                    _kind = DataStoreKind.fromValue(bs.readByte()); break;
                case 5:
                    _provider = bs.readString(); break;
                case 6:
                    _settings = bs.readString(); break;
                case 0:
                    break;
                default:
                    throw new RuntimeException("Unknown field id: " + fieldId);
            }
        } while (fieldId != 0);
    }

    //endregion

    public enum DataStoreKind {
        Sql(0), Cql(1), Blob(2), Future(3);

        public final byte value;

        DataStoreKind(int value) {
            this.value = (byte) value;
        }

        public static DataStoreKind fromValue(byte value) {
            switch (value) {
                case 0:
                    return Sql;
                case 1:
                    return Cql;
                case 2:
                    return Blob;
                case 3:
                    return Future;
                default:
                    throw new RuntimeException();
            }
        }
    }
}
