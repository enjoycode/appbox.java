package appbox.server.services;

import appbox.channel.SessionManager;
import appbox.channel.WebSession;
import appbox.data.JsonResult;
import appbox.entities.Employee;
import appbox.entities.OrgUnit;
import appbox.logging.Log;
import appbox.model.EntityModelInfo;
import appbox.runtime.IService;
import appbox.runtime.InvokeArgs;
import appbox.runtime.RuntimeContext;
import appbox.store.EntityStore;
import appbox.store.StoreInitiator;
import appbox.store.query.IndexGet;
import appbox.store.query.TableScan;
import appbox.utils.IdUtil;
import com.alibaba.fastjson.JSON;

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
        return q.toIndexRowAsync().thenCompose(row -> {
            if (row == null) {
                res.succeed = false;
                res.error   = "User not exists";
                return CompletableFuture.completedFuture(new JsonResult(res));
            }

            //验证密码
            if (!RuntimeContext.current().passwordHasher().verifyHashedPassword(row.getPassword(), req.p)) {
                res.succeed = false;
                res.error   = "Password not valid";
                return CompletableFuture.completedFuture(new JsonResult(res));
            }

            //TODO:****暂全表扫描获取Emploee对应的OrgUnits，待用Include EntitySet实现
            var q1 = new TableScan<>(IdUtil.SYS_ORGUNIT_MODEL_ID, OrgUnit.class);
            q1.where(OrgUnit.BASEID.eq(row.getTargetId()));
            return q1.toListAsync().thenCompose(ous -> {
                if (ous == null || ous.size() == 0) {
                    res.succeed = false;
                    res.error   = "User must assign to OrgUnit";
                    return CompletableFuture.completedFuture(new JsonResult(res));
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
    public CompletableFuture<Object> invokeAsync(CharSequence method, InvokeArgs args) {
        if (method.equals("InitStore")) {
            return StoreInitiator.initAsync().thenApply(ok -> ok);
        } else if (method.equals("Login")) {
            return login(args.getLong(), args.getString());
        } else if (method.equals("GetModelInfo")) {
            var res = new EntityModelInfo(RuntimeContext.current().getModel(args.getLong()));
            return CompletableFuture.completedFuture(res);
        } else {
            var ex = new NoSuchMethodException("SystemService can't find method: " + method.toString());
            return CompletableFuture.failedFuture(ex);
        }
    }
}
