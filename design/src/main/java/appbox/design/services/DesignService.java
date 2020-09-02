package appbox.design.services;

import appbox.design.IDeveloperSession;
import appbox.design.handlers.IRequestHandler;
import appbox.design.handlers.LoadDesignTree;
import appbox.runtime.IService;
import appbox.runtime.InvokeArg;
import appbox.runtime.RuntimeContext;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class DesignService implements IService {

    private final HashMap<CharSequence, IRequestHandler> handlers = new HashMap<>() {{
        put("LoadDesignTree", new LoadDesignTree());
    }};

    @Override
    public CompletableFuture<Object> invokeAsync(CharSequence method, List<InvokeArg> args) {
        if (!(RuntimeContext.current().currentSession() instanceof IDeveloperSession)) {
            return CompletableFuture.failedFuture(new Exception("Must login as a developer"));
        }

        var session   = (IDeveloperSession) RuntimeContext.current().currentSession();
        var designHub = session.getDesignHub();
        if (designHub == null) {
            return CompletableFuture.failedFuture(new Exception("Can't get DesignContext"));
        }

        var handler = handlers.get(method);
        if (handler == null) {
            return CompletableFuture.failedFuture(new Exception("Unknown design request: " + method));
        }

        return handler.handle(designHub, args);
    }
}
