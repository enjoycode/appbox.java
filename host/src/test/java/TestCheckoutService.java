import appbox.channel.SharedMemoryChannel;
import appbox.design.common.CheckoutInfo;
import appbox.design.common.CheckoutResult;
import appbox.design.services.CheckoutService;
import appbox.design.tree.DesignNodeType;
import appbox.runtime.RuntimeContext;
import appbox.server.runtime.HostRuntimeContext;
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
    public void checkoutAsync(){
        UUID uuid=new UUID(-6038453433195871438l,-7082168417221633763l);
        List<CheckoutInfo> checkoutInfos =new ArrayList<>();
        CheckoutInfo checkoutInfo=new CheckoutInfo(DesignNodeType.ApplicationNode,"1",1,"测试员",uuid);
        checkoutInfos.add(checkoutInfo);
        CompletableFuture<CheckoutResult> res = null;
        try {
            res = CheckoutService.checkoutAsync(checkoutInfos);
            var b=res.get();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Test
    public void loadAllAsync() throws Exception{
        var map = CheckoutService.loadAllAsync().get();
        assertNotNull(map);
    }

    @Test
    public void checkInAsync(){
        var fu=CheckoutService.checkInAsync();
        try{
            var res=fu.get();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public static void main(String args[]){
        UUID uuid=UUID.randomUUID();
        long m=uuid.getMostSignificantBits();
        long l=uuid.getLeastSignificantBits();
        System.out.println(m);
        System.out.println(l);
    }


}
