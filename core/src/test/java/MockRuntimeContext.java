import appbox.model.ApplicationModel;
import appbox.model.EntityModel;
import appbox.model.ModelBase;
import appbox.runtime.IRuntimeContext;
import appbox.runtime.ISessionInfo;
import appbox.runtime.InvokeArg;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class MockRuntimeContext implements IRuntimeContext {
    private       ApplicationModel           applicationModel;
    private final HashMap<Long, EntityModel> models = new HashMap<>();

    public void injectApplicationModel(ApplicationModel app) {
        applicationModel = app;
    }

    public void injectEntityModel(EntityModel model) {
        models.putIfAbsent(model.id(), model);
    }

    @Override
    public ISessionInfo currentSession() {
        return null;
    }

    @Override
    public void setCurrentSession(ISessionInfo session) {

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
}
