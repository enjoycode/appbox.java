package appbox.core.serialization;

public interface IBinSerializable {
    void writeTo(BinSerializer bs) throws Exception;

    void readFrom(BinDeserializer bs) throws Exception;
}
