package appbox.server.services;

import appbox.channel.SessionManager;
import appbox.channel.WebSession;
import appbox.data.JsonResult;
import appbox.entities.Employee;
import appbox.entities.OrgUnit;
import appbox.logging.Log;
import appbox.runtime.IService;
import appbox.runtime.InvokeArg;
import appbox.store.EntityStore;
import appbox.store.StoreInitiator;
import appbox.store.query.IndexGet;
import appbox.store.query.TableScan;
import appbox.utils.IdUtil;
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
        public String  name;
        public UUID    id; //对应的组织单元的标识号，非会话标识
        public String  error;
        public boolean succeed;
    }


    public CompletableFuture<Object> login(long sessionId, String loginJson) {
        var req = JSON.parseObject(loginJson, LoginRequest.class);
        var res = new LoginResponse();

        //根据账号索引查询
        var q = new IndexGet<>(Employee.UI_Account.class);
        q.where(Employee.ACCOUNT, req.u);
        return q.toIndexRowAsync().thenApply(row -> {
            if (row == null) {
                res.succeed = false;
                res.error   = "User account not exists";
                return new JsonResult(res);
            }

            //TODO:验证密码

            //TODO:****暂全表扫描获取Emploee对应的OrgUnits，待用Include EntitySet实现
            var q1 = new TableScan<>(IdUtil.SYS_ORGUNIT_MODEL_ID, OrgUnit.class);
            q1.where(OrgUnit.BASEID.eq(row.getTargetId()));
            return q1.toListAsync().thenApply(ous -> {
                if (ous == null || ous.size() == 0) {
                    res.succeed = false;
                    res.error   = "User must assign to OrgUnit";
                    return new JsonResult(res);
                }

                return EntityStore.loadTreePathAsync(OrgUnit.class, ous.get(0).id(), OrgUnit::getParentId, OrgUnit::getName)
                        .thenApply(path -> {
                            //注册会话
                            //long sessionId = ((long) peerId) << 32 | ((long) StringUtil.getHashCode(user));
                            var session = new WebSession(sessionId, path, row.getTargetId());
                            SessionManager.register(session);

                            Log.debug("User login: " + req.u);
                            res.succeed = true;
                            res.name    = req.u;
                            res.id      = ous.get(0).id().toUUID();
                            return new JsonResult(res);
                        });
            });
        });
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
