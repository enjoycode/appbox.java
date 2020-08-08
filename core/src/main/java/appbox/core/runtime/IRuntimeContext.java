package appbox.core.runtime;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 运行时上下文，用于提供模型容器及服务调用
 */
public interface IRuntimeContext {

    CompletableFuture<Object> invokeAsync(CharSequence method, List<InvokeArg> args);

}
