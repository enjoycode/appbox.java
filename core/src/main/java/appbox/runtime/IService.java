package appbox.runtime;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface IService {

    /**
     * 异步调用服务方法
     * @param method eg: "SaveOrder"
     * @param args 参数列表
     * @return 异步结果
     */
    CompletableFuture<Object> invokeAsync(CharSequence method, InvokeArgs args);

}
