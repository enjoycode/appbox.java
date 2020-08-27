package appbox.server.services;

import appbox.runtime.IService;
import appbox.runtime.InvokeArg;
import appbox.utils.StringUtil;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 仅用于测试，待移除
 */
public final class TestLoginService implements IService {
    @Override
    public CompletableFuture<Object> invokeAsync(CharSequence method, List<InvokeArg> args) {
        var peerId = args.get(0).getShort();
        var user   = args.get(1).getString();
        var pass   = args.get(2).getString();

        long sessionId = ((long) peerId) << 32 | ((long) StringUtil.getHashCode(user));
        var  json      = String.format("{\"s\":1,\"i\":%d,\"n\":\"%s\"}", sessionId, user);
        return CompletableFuture.completedFuture(json);
    }
}
