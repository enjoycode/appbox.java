package appbox.serialization;

public interface IBinSerializable {
    void writeTo(BinSerializer bs);

    void readFrom(BinDeserializer bs);
}
