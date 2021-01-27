import java.util.concurrent.CompletableFuture;

import sys.entities.*;
import static sys.Async.await;

import appbox.data.EntityId;
import appbox.runtime.RuntimeContext;

public class OrgUnitService {

    public CompletableFuture<?> loadTree() {
        var q = new TableScan<OrgUnit>();
        return q.toTreeAsync(t -> t.Childs);
    }

    public CompletableFuture<?> loadEnterprise(EntityId id) {
        return Enterprise.fetchAsync(id);
    }

    public CompletableFuture<?> loadWorkgroup(EntityId id) {
        return Workgroup.fetchAsync(id);
    }

    public CompletableFuture<?> loadEmployee(EntityId id) {
        return Emploee.fetchAsync(id);
    }

    public CompletableFuture<Void> SaveEmployee(Emploee emp, EntityId ouid) {
        //TODO:同步关联至相同员工的组织单元的名称
        var ou = await(OrgUnit.fetchAsync(ouid));
        boolean nameChanged = !ou.Name.equals(emp.Name);
        if (nameChanged)
            ou.Name = emp.Name;

        var txn = await(sys.KVTransaction.beginAsync());
        await(emp.saveAsync(txn));
        if (nameChanged)
            await(ou.saveAsync(txn));

        return txn.commitAsync();
    }

    public CompletableFuture<Void> ResetPassword(Emploee emp, String password) {
        if (password == null || password.isEmpty())
            throw new RuntimeException("密码不能为空");

        var hashed = RuntimeContext.current().passwordHasher().hashPassword(password);
        emp.Password = hashed;
        return emp.saveAsync();
    }

}
