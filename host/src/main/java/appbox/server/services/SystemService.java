package appbox.server.services;

import appbox.channel.SessionManager;
import appbox.channel.WebSession;
import appbox.data.JsonResult;
import appbox.logging.Log;
import appbox.runtime.IService;
import appbox.runtime.InvokeArg;
import appbox.store.StoreInitiator;
import com.alibaba.fastjson.JSON;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * 系统内置的一些服务，如初始化存储、密码Hash等
 */
public final class SystemService implements IService {
    static final class LoginRequest {
        public String  u;
        public String  p;
        public boolean e;
    }

    static final class LoginResponse {
        public String name;
        public UUID   id; //对应的组织单元的标识号，非会话标识
    }


    public CompletableFuture<Object> login(long sessionId, String loginJson) {
        var req = JSON.parseObject(loginJson, LoginRequest.class);
        //TODO:验证逻辑

        //注册会话
        //long sessionId = ((long) peerId) << 32 | ((long) StringUtil.getHashCode(user));
        var session = new WebSession(sessionId, req.u);
        SessionManager.register(session);

        Log.debug("用户登入: " + req.u);
        var res = new LoginResponse();
        res.name = req.u;
        res.id = UUID.randomUUID();
        return CompletableFuture.completedFuture(new JsonResult(res));
    }

    @Override
    public CompletableFuture<Object> invokeAsync(CharSequence method, List<InvokeArg> args) {
        if (method.equals("InitStore")) {
            return StoreInitiator.initAsync().thenApply(ok -> ok);
        } else if (method.equals("Login")) {
            return login(args.get(0).getLong(), args.get(1).getString());
        } else {
            var ex = new NoSuchMethodException("SystemService can't find method: " + method.toString());
            return CompletableFuture.failedFuture(ex);
        }
    }
}
