import appbox.channel.SharedMemoryChannel;
import appbox.design.common.CheckoutInfo;
import appbox.design.common.CheckoutResult;
import appbox.design.services.CheckoutService;
import appbox.design.tree.DesignNodeType;
import appbox.model.EntityModel;
import appbox.runtime.RuntimeContext;
import appbox.server.runtime.HostRuntimeContext;
import appbox.store.KVTransaction;
import appbox.store.ModelStore;
import appbox.store.StoreInitiator;
import appbox.store.SysStoreApi;
import appbox.utils.IdUtil;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class TestCheckout {

    private static SharedMemoryChannel channel;

    @BeforeAll
    public static void init() {
        RuntimeContext.init(new HostRuntimeContext(), (short) 1/*TODO: fix peerId*/);
        channel = new SharedMemoryChannel("AppChannel");
        SysStoreApi.init(channel);

        StoreInitiator.initAsync();
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

        List<CheckoutInfo> checkoutInfos =new ArrayList<>();
        CheckoutInfo checkoutInfo=new CheckoutInfo(DesignNodeType.ApplicationNode,"1",1,"测试员",UUID.randomUUID());
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
    public void loadModelAsync(){
        var res=ModelStore.loadModelAsync(Long.parseLong("1"));
        try{
            res.get();
        }catch (Exception e){
            e.printStackTrace();
        }
    }


    @Test
    public void loadAllAsync(){
        var res=CheckoutService.loadAllAsync();
        try{
            res.get();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    @Test
    public void checkInAsync(){
        var res=CheckoutService.checkInAsync();
        try{
            res.get();
        }catch (Exception e){
            e.printStackTrace();
        }
    }


}
