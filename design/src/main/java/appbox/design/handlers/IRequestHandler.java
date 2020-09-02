package appbox.design.handlers;

import appbox.design.DesignHub;
import appbox.runtime.InvokeArg;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface IRequestHandler {

    CompletableFuture<Object> handle(DesignHub hub, List<InvokeArg> args);

}
