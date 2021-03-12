package appbox.store;

import appbox.data.BlobObject;
import appbox.logging.Log;

import java.util.HashMap;
import java.util.concurrent.CompletableFuture;

public abstract class BlobStore {

    //region ====static====
    private static final HashMap<String, SysBlobStore> sysBlobs = new HashMap<>();

    public static SysBlobStore get(String storeName) {
        var store = sysBlobs.get(storeName);
        if (store == null) {
            synchronized (sysBlobs) {
                store = sysBlobs.get(storeName);
                if (store != null)
                    return store;

                try {
                    var model = ModelStore.loadBlobStoreAsync(storeName).get();
                    if (model == null)
                        throw new RuntimeException("System blob store not exists");
                    store = new SysBlobStore(storeName, model.blobStoreId);
                    sysBlobs.put(storeName, store);
                } catch (Exception ex) {
                    Log.error("Load blob store error: " + ex.getMessage());
                }
            }
        }
        return store;
    }
    //endregion

    public abstract CompletableFuture<BlobObject[]> listAsync(String path);

    public abstract CompletableFuture<Void> deleteFileAsync(String path);

}
