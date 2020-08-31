import appbox.utils.ReflectUtil;
import com.alibaba.ttl.TransmittableThreadLocal;
import org.junit.jupiter.api.Test;

import java.lang.reflect.*;
import java.util.Arrays;
import java.util.concurrent.*;

public class TestThreadLocal {

    private static InheritableThreadLocal<Integer>   itl = new InheritableThreadLocal<>();
    private static TransmittableThreadLocal<Integer> ttl = new TransmittableThreadLocal<>();

    private static void showITL(String task) {
        System.out.printf("%s %s : %s\n", task, Thread.currentThread().getName(), itl.get());
    }

    private static void showTTL(String task) {
        System.out.printf("%s %s : %s\n", task, Thread.currentThread().getName(), ttl.get());
    }

    @Test
    public void TestITL() throws ExecutionException, InterruptedException {
        itl.set(10);

        //只有一个线程的线程池
        //ExecutorService executorService = Executors.newFixedThreadPool(1);

        var fut1 = CompletableFuture.supplyAsync(() -> {
            showITL("1");
            //sessionId.remove();
            return CompletableFuture.runAsync(() -> {
                showITL("1.1");
            }/*, executorService*/);
        }/*, executorService*/);
        fut1.get().get();

        itl.set(20);
        showITL("0");

        var fut2 = CompletableFuture.supplyAsync(() -> {
            itl.set(20);
            showITL("2");
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return CompletableFuture.runAsync(() -> {
                showITL("2.1");
            }/*, executorService*/);
        }/*, executorService*/);
        fut2.get().get();
    }

    @Test
    public void TestTTL() throws Exception {
        var async_pool      = CompletableFuture.completedFuture(true).defaultExecutor();
        var wrap_async_pool = com.alibaba.ttl.threadpool.TtlExecutors.getTtlExecutor(async_pool);

        ReflectUtil.setFinalStatic(CompletableFuture.class.getDeclaredField("ASYNC_POOL"), wrap_async_pool);

        ttl.set(10);

        //ExecutorService executorService = Executors.newFixedThreadPool(8);
        //var wrap = com.alibaba.ttl.threadpool.TtlExecutors.getTtlExecutorService(executorService);

        var fut1 = CompletableFuture.runAsync(() -> {
            showTTL("1");
        }).thenRunAsync(() -> {
            showTTL("1.1");
        });
        fut1.get();

        //ttl.set(20);
        showTTL("0");

        var fut2 = CompletableFuture.supplyAsync(() -> {
            ttl.set(20);
            showTTL("2");
            return CompletableFuture.runAsync(() -> {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                showTTL("2.2");
            });
        }).thenAcceptAsync((r) -> {
            try {
                r.get();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
            showTTL("2.1");
        });

        //fut1.get();
        fut2.get();
    }

}
