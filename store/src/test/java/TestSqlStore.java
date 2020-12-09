import appbox.design.IDesignContext;
import appbox.model.ApplicationModel;
import appbox.model.EntityModel;
import appbox.model.entity.DataFieldModel;
import appbox.runtime.MockRuntimeContext;
import appbox.runtime.RuntimeContext;
import appbox.store.PgSqlStore;
import appbox.store.SqlStore;
import appbox.store.query.SqlQuery;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestSqlStore {

    private static final String      connection  = "jdbc:postgresql://10.211.55.2:54321/ABStore?user=lushuaijun&password=123456";
    private static final long        testStoreId = 1;
    private static       EntityModel model;

    @BeforeAll
    public static void init() throws Exception {
        model = new EntityModel(ELog.MODEL_ID, "ELog2");
        model.bindToSqlStore(testStoreId);
        var idMember   = new DataFieldModel(model, "Id", DataFieldModel.DataFieldType.Int, false);
        var nameMember = new DataFieldModel(model, "Name", DataFieldModel.DataFieldType.String, true);
        var addrMember = new DataFieldModel(model, "Address", DataFieldModel.DataFieldType.String, true);
        model.addSysMember(idMember, ELog.ID_ID);
        model.addSysMember(nameMember, ELog.NAME_ID);
        model.addSysMember(addrMember, ELog.ADDR_ID);

        var ctx = new MockRuntimeContext();
        ctx.injectApplicationModel(new ApplicationModel("appbox", "sys"));
        ctx.injectEntityModel(model);
        RuntimeContext.init(ctx, (short) 10410);

        var db = new PgSqlStore(connection);
        SqlStore.inject(testStoreId, db);
    }


    @Test
    @Order(1)
    public void testCreateTable() throws Exception {
        var db = SqlStore.get(testStoreId);
        var txn = db.beginTransaction().get();
        db.createTableAsync(model, txn, (IDesignContext) RuntimeContext.current()).get();
        txn.commitAsync().get();
    }

    @Test
    @Order(10)
    public void testInsert() throws Exception {
        var log = new ELog();
        log.setId(100);
        log.setName("Future");

        var db = SqlStore.get(testStoreId);
        db.insertAsync(log, null).get();
    }

    @Test
    @Order(20)
    public void testQueryToList() throws Exception {
        var q    = new SqlQuery<>(ELog.MODEL_ID, ELog.class);
        var list = q.toListAsync().get();
        assertNotNull(list);
    }

    @Test
    @Order(30)
    public void testQueryToDynamic() throws Exception {
        var q = new SqlQuery<>(ELog.MODEL_ID, ELog.class);
        q.where(q.t.m("Id").eq(200)); //t->t.Id == 200

        var list = q.toListAsync(r -> new Object() {
            final int eid = r.getInt(0);
            final String ename = r.getString(1);
        }, q.t.m("Id"), q.t.m("Name")).get();

        assertNotNull(list);
        for (var item : list) {
            System.out.println("id: " + item.eid + " name: " + item.ename);
        }
    }

    @Test
    @Order(40)
    public void testQueryToExpand() throws Exception {
        var q = new SqlQuery<>(ELog.MODEL_ID, ELog.class);
        var list = q.toListAsync(r -> new ELog() {
            final String extName = "Ext" + r.getString(1); //扩展的字段
        }, q.t.m("Id"), q.t.m("Name"), q.t.m("Address")).get();
        assertNotNull(list);
    }

}
