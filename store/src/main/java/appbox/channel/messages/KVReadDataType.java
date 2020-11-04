package appbox.channel.messages;

public enum KVReadDataType {
    ApplicationModel(1),
    Model(2),
    ModelCode(3),
    Partition(4); //RaftGroupId

    public final byte value;

    KVReadDataType(int value) {
        this.value = (byte) value;
    }
}
