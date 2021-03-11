package appbox.channel.messages;

import appbox.serialization.IInputStream;
import appbox.data.BlobObject;
import appbox.utils.DateTimeUtil;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

public final class KVScanBlobResponse extends KVScanResponse {

    public BlobObject[] objects;

    @Override
    public void readFrom(IInputStream bs) {
        reqId     = bs.readInt();
        errorCode = bs.readInt();

        if (errorCode != 0)
            return;

        skipped = bs.readInt();
        length  = bs.readInt();

        int    keySize, valueSize = 0;
        byte[] key                = new byte[512]; //TODO:最长的
        objects = new BlobObject[length];
        for (int i = 0; i < length; i++) {
            keySize = bs.readNativeVariant(); //Row's KeySize
            bs.readBytes(key, 0, keySize); //Row's Key
            //从Key中读取类型及名称
            boolean isFile = key[10] == 1;
            String  name   = new String(key, 11, keySize - 11, StandardCharsets.UTF_8);

            valueSize  = bs.readNativeVariant(); //Row's ValueSize
            objects[i] = isFile ? readFile(bs, name) : readFolder(bs, name);
        }
    }

    private static LocalDateTime fromTimestamp(long timestamp) {
        var micros = timestamp >> 12;
        return DateTimeUtil.fromEpochMilli(micros / 1000);
    }

    private BlobObject readFolder(IInputStream bs, String name) {
        var folderId  = bs.readLong();
        var size      = bs.readLong();
        var timestamp = bs.readLong();
        return new BlobObject(name, size, fromTimestamp(timestamp), false);
    }

    private BlobObject readFile(IInputStream bs, String name) {
        var raftGroupId = bs.readLong();
        var fileId      = bs.readLong();
        var size        = bs.readInt();
        var version     = bs.readInt();
        var timestamp   = bs.readLong();
        return new BlobObject(name, size, fromTimestamp(timestamp), true);
    }

}
