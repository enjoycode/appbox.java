package appbox.design.tree;

import appbox.design.DesignHub;
import appbox.model.DataStoreModel;
import appbox.serialization.IJsonWriter;
import appbox.store.ModelStore;

import java.util.concurrent.CompletableFuture;

public final class DataStoreNode extends DesignNode {

    private final DataStoreModel _model;

    DataStoreNode(DataStoreModel model, DesignHub hub) {
        _model = model;
    }

    //region ====Properties====
    public DataStoreModel model() { return _model; }

    @Override
    public DesignNodeType nodeType() {
        return DesignNodeType.DataStoreNode;
    }

    @Override
    public String id() {
        return Long.toUnsignedString(_model.id());
    }

    @Override
    public String text() {
        return _model.name();
    }

    @Override
    public String checkoutInfoTargetID() {
        return id();
    }
    //endregion

    public CompletableFuture<Void> saveAsync(boolean isNew) {
        //暂忽略系统BlobStore的保存
        if (_model.isSystemBlobStore())
            return CompletableFuture.completedFuture(null);

        if (!isCheckoutByMe())
            return CompletableFuture.failedFuture(new RuntimeException("DataStore hasn't checkout"));

        if (isNew)
            return ModelStore.createDataStoreAsync(_model);
        else
            return ModelStore.updateDataStoreAsync(_model);
    }

    @Override
    protected void writeJsonMembers(IJsonWriter writer) {
        super.writeJsonMembers(writer);

        writer.writeKeyValue("Kind", _model.kind().value);
        writer.writeKeyValue("Provider", _model.provider());
        writer.writeKeyValue("Settings", _model.settings());
    }
}
