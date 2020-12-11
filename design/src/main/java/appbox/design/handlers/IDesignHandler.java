package appbox.design.handlers;

import appbox.design.DesignHub;
import appbox.runtime.InvokeArgs;

import java.util.concurrent.CompletableFuture;

public interface IDesignHandler {

    CompletableFuture<Object> handle(DesignHub hub, InvokeArgs args);

}
