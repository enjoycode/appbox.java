package appbox.core.serialization;

public interface IInputStream {

    byte readByte() throws Exception;

    void read(byte[] dest, int offset, int count) throws Exception;

    default short readShort() throws Exception {
        return (short) (readByte() << 8 | readByte());
    }

    default int readVariant() throws Exception {
        int data = readByte();
        if ((data & 0x80) == 0) {
            return -(data & 1) ^ ((data >> 1) & 0x7fffffff);
        }
        data &= 0x7F;
        int num2 = readByte();
        data |= (num2 & 0x7F) << 7;
        if ((num2 & 0x80) == 0) {
            return -(data & 1) ^ ((data >> 1) & 0x7fffffff);
        }
        num2 = readByte();
        data |= (num2 & 0x7F) << 14;
        if ((num2 & 0x80) == 0) {
            return -(data & 1) ^ ((data >> 1) & 0x7fffffff);
        }
        num2 = readByte();
        data |= (num2 & 0x7F) << 0x15;
        if ((num2 & 0x80) == 0) {
            return -(data & 1) ^ ((data >> 1) & 0x7fffffff);
        }
        num2 = readByte();
        data |= num2 << 0x1C;
        if ((num2 & 240) != 0) {
            throw new Exception("out of range");
        }
        return -(data & 1) ^ ((data >> 1) & 0x7fffffff);
    }

    default String readString() throws Exception {
        int len = readVariant();
        if (len == -1) {
            return null;
        } else if (len == 0) {
            return "";
        } else {
            //TODO:暂简单实现，待优化
            var utf8Bytes = new byte[len];
            read(utf8Bytes, 0, len);
            return new String(utf8Bytes, 0, len, "UTF-8");
        }
    }
}
