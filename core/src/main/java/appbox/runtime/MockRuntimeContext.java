package appbox.runtime;

import appbox.design.IDesignContext;
import appbox.model.ApplicationModel;
import appbox.model.EntityModel;
import appbox.model.ModelBase;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class MockRuntimeContext implements IRuntimeContext, IDesignContext {
    private       ApplicationModel         applicationModel;
    private final HashMap<Long, ModelBase> models = new HashMap<>();
    private       IUserSession             session;

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
    public IUserSession currentSession() {
        return session;
    }

    @Override
    public void setCurrentSession(IUserSession session) {
        this.session = session;
    }

    @Override
    public IPasswordHasher passwordHasher() {
        throw new RuntimeException("不支持");
    }

    @Override
    public CompletableFuture<Object> invokeAsync(String method, InvokeArgs args) {
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

    @Override
    public EntityModel getEntityModel(long modelId) {
        return getModel(modelId);
    }
}
