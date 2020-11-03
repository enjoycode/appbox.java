package appbox.design.handlers.entity;

import appbox.data.JsonResult;
import appbox.design.DesignHub;
import appbox.design.handlers.IRequestHandler;
import appbox.runtime.InvokeArg;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class GetEntityRefModels implements IRequestHandler {

    @Override
    public CompletableFuture<Object> handle(DesignHub hub, List<InvokeArg> args) {
        var targetEntityModelID = args.get(0).getString();
        //var refModels = hub.designTree.findEntityRefModels(Long.parseUnsignedLong(targetEntityModelID));
        //object res = refModels.Select(t => new
        //{
        //    Path = $"{hub.DesignTree.FindApplicationNode(t.Owner.AppId).Model.Name}.{t.Owner.Name}.{t.Name}",
        //        EntityID = t.Owner.Id.ToString(), //ulong转换为string
        //        MemberID = t.MemberId,
        //}).ToArray();
        return CompletableFuture.supplyAsync(() -> {
            return new JsonResult(null);
        });
    }
}
