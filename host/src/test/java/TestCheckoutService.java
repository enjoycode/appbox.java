import appbox.channel.SharedMemoryChannel;
import appbox.design.common.CheckoutInfo;
import appbox.design.services.CheckoutService;
import appbox.design.tree.DesignNodeType;
import appbox.logging.Log;
import appbox.runtime.RuntimeContext;
import appbox.server.runtime.HostRuntimeContext;
import appbox.store.KVTransaction;
import appbox.store.SysStoreApi;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class TestCheckoutService {

    private static SharedMemoryChannel channel;

    @BeforeAll
    public static void init() {
        RuntimeContext.init(new HostRuntimeContext(), (short) 1/*TODO: fix peerId*/);
        channel = new SharedMemoryChannel("AppChannel");
        SysStoreApi.init(channel);

        CompletableFuture.runAsync(() -> {
            channel.startReceive();
        });
    }

    @AfterAll
    public static void clean() {
        //TODO: storeReceive
    }

    @Test
    public void checkoutAsync() throws Exception {
        UUID               uuid          = new UUID(-6038453433195871438L, -7082168417221633763L);
        List<CheckoutInfo> checkoutInfos = new ArrayList<>();
        CheckoutInfo       checkoutInfo  = new CheckoutInfo(DesignNodeType.ApplicationNode, "2", 1, "测试员", uuid);
        checkoutInfos.add(checkoutInfo);
        var res = CheckoutService.checkoutAsync(checkoutInfos).get();
        assertNotNull(res);
    }

    @Test
    public void loadAllAsync() throws Exception {
        var map = CheckoutService.loadAllAsync().get();
        Log.debug("size:" + map.size());
        assertNotNull(map);
    }

    @Test
    public void checkInAsync() throws Exception {
        var txn = KVTransaction.beginAsync().get();
        var fu  = CheckoutService.checkInAsync(txn).get();
        txn.commitAsync().get();
    }

}
