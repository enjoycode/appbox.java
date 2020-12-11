package appbox.design.handlers.entity;

import appbox.data.JsonResult;
import appbox.design.DesignHub;
import appbox.design.handlers.IDesignHandler;
import appbox.model.entity.EntityRefModel;
import appbox.runtime.InvokeArgs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public final class GetEntityRefModels implements IDesignHandler {

    @Override
    public CompletableFuture<Object> handle(DesignHub hub, InvokeArgs args) {
        var targetEntityModelID = args.getString();
        var refModels = hub.designTree.findEntityRefModels(Long.parseUnsignedLong(targetEntityModelID));
        List<Map> res=new ArrayList<>();
        for(EntityRefModel refModel:refModels){
            Map map=new HashMap();
            map.put("Path",hub.designTree.findApplicationNode(refModel.owner.appId()).model.name()+"."+refModel.owner.name()+"."+refModel.name());
            map.put("EntityID",String.valueOf(refModel.owner.id()));
            map.put("MemberID",refModel.memberId());
            res.add(map);
        }
        return CompletableFuture.completedFuture(new JsonResult(res));
    }

}
