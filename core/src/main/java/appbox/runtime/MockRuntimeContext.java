package appbox.runtime;

import appbox.model.ApplicationModel;
import appbox.model.EntityModel;
import appbox.model.ModelBase;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class MockRuntimeContext implements IRuntimeContext {
    private       ApplicationModel         applicationModel;
    private final HashMap<Long, ModelBase> models = new HashMap<>();
    private       ISessionInfo             session;

    public void injectApplicationModel(ApplicationModel app) {
        applicationModel = app;
    }

    public void injectEntityModel(EntityModel model) {
        models.putIfAbsent(model.id(), model);
    }

    public void injectModels(List<ModelBase> models) {
        for (var m : models) {
            this.models.putIfAbsent(m.id(), m);
        }
    }

    @Override
    public ISessionInfo currentSession() {
        return session;
    }

    @Override
    public void setCurrentSession(ISessionInfo session) {
        this.session = session;
    }

    @Override
    public CompletableFuture<Object> invokeAsync(String method, List<InvokeArg> args) {
        return null;
    }

    @Override
    public ApplicationModel getApplicationModel(int appId) {
        return applicationModel;
    }

    @Override
    public <T extends ModelBase> T getModel(long modelId) {
        return (T) models.get(modelId);
    }

    @Override
    public void invalidModelsCache(String[] services, long[] others, boolean byPublish) {

    }
}
