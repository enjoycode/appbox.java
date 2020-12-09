package appbox.design.tree;

import appbox.design.DesignHub;
import appbox.design.services.StagedService;
import appbox.model.DataStoreModel;
import appbox.serialization.IJsonWriter;

import java.util.concurrent.CompletableFuture;

public final class DataStoreNode extends DesignNode {

    private DataStoreModel _model;

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
    public int version() {
        return _model.version();
    }

    @Override
    public String checkoutInfoTargetID() {
        return id();
    }
    //endregion

    public CompletableFuture<Void> saveAsync() {
        if (!isCheckoutByMe())
            return CompletableFuture.failedFuture(new RuntimeException("DataStore hasn't checkout"));
        return StagedService.saveModelAsync(_model);
    }

    @Override
    protected void writeJsonMembers(IJsonWriter writer) {
        super.writeJsonMembers(writer);

        writer.writeKeyValue("Kind", _model.kind().value);
        writer.writeKeyValue("Provider", _model.provider());
        writer.writeKeyValue("Settings", _model.settings());
    }
}
