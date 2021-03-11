package appbox.model;

import appbox.serialization.IBinSerializable;
import appbox.serialization.IInputStream;
import appbox.serialization.IOutputStream;
import appbox.utils.StringUtil;

public final class DataStoreModel implements IBinSerializable {
    private long          _id;
    private String        _name;
    private DataStoreKind _kind;
    private String        _provider;
    private String        _settings;
    //TODO:DataStoreNameRules

    public DataStoreModel() {}

    public DataStoreModel(DataStoreKind kind, String provider, String storeName) {
        _id       = StringUtil.getHashCode(storeName);
        _name     = storeName;
        _kind     = kind;
        _provider = provider;
    }

    public long id() {
        return _id;
    }

    public String name() {
        return _name;
    }

    public DataStoreKind kind() { return _kind; }

    public String provider() { return _provider; }

    public String settings() { return _settings; }

    /** 是否系统内置的BlobStore */
    public boolean isSystemBlobStore() {
        return _kind == DataStoreKind.Blob && (_provider == null || _provider.isEmpty());
    }

    public void updateSettings(String value) {
        _settings = value;
    }

    //region ====Serialization====
    //注意:provider及settings的编号与C#不同
    @Override
    public void writeTo(IOutputStream bs) {
        bs.writeLongField(_id, 1);
        bs.writeStringField(_name, 2);
        bs.writeByteField(_kind.value, 3);
        bs.writeStringField(_provider, 5);
        bs.writeStringField(_settings, 6);

        bs.finishWriteFields();
    }

    @Override
    public void readFrom(IInputStream bs) {
        int fieldId;
        do {
            fieldId = bs.readVariant();
            switch (fieldId) {
                case 1:
                    _id = bs.readLong(); break;
                case 2:
                    _name = bs.readString(); break;
                case 3:
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
