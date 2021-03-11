package appbox.store;

import appbox.channel.messages.KVScanBlobRequest;
import appbox.channel.messages.KVScanBlobResponse;
import appbox.data.BlobObject;
import appbox.utils.IdUtil;

import java.util.concurrent.CompletableFuture;

/** 系统内置的BlobStore */
public final class SysBlobStore extends BlobStore {

    public final  byte   id;
    public final  String name;
    private final long   metaRaftGroupId;

    public SysBlobStore(String name, byte id) {
        this.name            = name;
        this.id              = id;
        this.metaRaftGroupId = IdUtil.makeBlobMetaRaftGroupId(id);
    }

    @Override
    public CompletableFuture<BlobObject[]> listAsync(String path) {
        var req = new KVScanBlobRequest(metaRaftGroupId, path);
        return SysStoreApi.execKVScanAsync(req, new KVScanBlobResponse())
                .thenApply(res -> res.objects);
    }

}
