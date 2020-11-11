import appbox.model.ApplicationModel;
import appbox.model.EntityModel;
import appbox.model.entity.DataFieldModel;
import appbox.runtime.RuntimeContext;
import appbox.store.PgSqlStore;
import appbox.store.SqlStore;
import appbox.store.query.SqlQuery;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestSqlStore {

    private static final String connection  = "jdbc:postgresql://10.211.55.2:54321/ABStore?user=lushuaijun&password=123456";
    private static final long   testStoreId = 1;

    @BeforeAll
    public static void init() throws Exception {
        var model = new EntityModel(ELog.MODEL_ID, "ELog");
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
    public void testInsert() throws Exception {
        var log = new ELog();
        log.setId(100);
        log.setName("Future");

        var db = SqlStore.get(testStoreId);
        db.insertAsync(log, null).get();
    }

    @Test
    public void testQuery() throws Exception {
        var q = new SqlQuery<>(ELog.MODEL_ID, ELog.class);
        var list = q.toListAsync().get();
        assertNotNull(list);
    }

}
