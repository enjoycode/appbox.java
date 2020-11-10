package appbox.design.handlers.entity;

import appbox.data.JsonResult;
import appbox.design.DesignHub;
import appbox.design.handlers.IRequestHandler;
import appbox.model.entity.EntityRefModel;
import appbox.runtime.InvokeArg;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class GetEntityRefModels implements IRequestHandler {

    @Override
    public CompletableFuture<Object> handle(DesignHub hub, List<InvokeArg> args) {
        var targetEntityModelID = args.get(0).getString();
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
