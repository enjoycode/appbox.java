package appbox.store;

import appbox.channel.messages.*;
import appbox.data.BlobObject;
import appbox.utils.IdUtil;

import java.util.concurrent.CompletableFuture;

/** 系统内置的BlobStore */
public final class SysBlobStore extends BlobStore {

    public static final byte ACTION_DELETE_FILE = 5;

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

    @Override
    public CompletableFuture<Void> deleteFileAsync(String path) {
        var req = new BlobCommandRequest(metaRaftGroupId, ACTION_DELETE_FILE,
                0, 0, 0, 0, path);
        return SysStoreApi.execCommandAsync(req)
                .thenAccept(StoreResponse::checkStoreError);
    }
}
