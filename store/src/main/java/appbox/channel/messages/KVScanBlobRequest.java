package appbox.channel.messages;

import appbox.serialization.IOutputStream;
import appbox.utils.StringUtil;

/** 扫描系统BlobStore指定目录下的对象列表 */
public final class KVScanBlobRequest extends KVScanRequest {

    private static final byte BLOB_SCAN_META_BY_PATH = 2; //BlobCommand.hh BlobReadType::kScanMetaByPath

    private final long   blobMetaRaftGroupId;
    private final String path;

    public KVScanBlobRequest(long blobMetaRaftGroupId, String path) {
        this.blobMetaRaftGroupId = blobMetaRaftGroupId;
        this.path                = path;
    }

    @Override
    public void writeTo(IOutputStream bs) {
        int pathSize = StringUtil.getUtf8Size(path);

        bs.writeInt(0); //ReqId占位
        bs.writeLong(blobMetaRaftGroupId); //raftGroupId
        bs.writeNativeVariant(1 + pathSize); //BeginKeySize
        bs.writeByte(BLOB_SCAN_META_BY_PATH); //BeginKey ScanMetaByPath
        bs.writeUtf8(path); //BeginKey Path
        bs.writeNativeVariant(0); //EndKeySize
        bs.writeInt(0); //Skip
        bs.writeInt(Integer.MAX_VALUE); //Take
        bs.writeLong(0); //Timestamp
        bs.writeByte((byte) -1); //DataCF
        bs.writeBool(false); //IsMVCC TODO: remove it
        bs.writeBool(false); //ToIndexTarget
    }

}
