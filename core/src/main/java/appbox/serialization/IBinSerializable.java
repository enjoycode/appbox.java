package appbox.serialization;

public interface IBinSerializable {
    void writeTo(BinSerializer bs) throws Exception;

    void readFrom(BinDeserializer bs) throws Exception;
}
