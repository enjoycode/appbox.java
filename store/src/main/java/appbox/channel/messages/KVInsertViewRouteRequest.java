package appbox.channel.messages;

import appbox.serialization.IOutputStream;
import appbox.store.KVTxnId;
import appbox.store.KeyUtil;

public final class KVInsertViewRouteRequest extends KVInsertRequire {

    private final String viewName; //eg: sys.CustomerList
    private final String path; //无自定义路由为空，有上级路由;分隔

    public KVInsertViewRouteRequest(String viewName, String path, KVTxnId txnId) {
        super(txnId);

        this.viewName = viewName;
        this.path     = path;

        raftGroupId    = KeyUtil.META_RAFTGROUP_ID;
        schemaVersion  = 0;
        dataCF         = -1;
        overrideExists = true;
    }

    @Override
    public void writeTo(IOutputStream bs) {
        super.writeTo(bs);

        //key
        bs.writeByte(KeyUtil.METACF_VIEW_ROUTE_PREFIX);
        bs.writeUtf8(viewName);
        //refs
        bs.writeVariant(0);
        //data
        if (path != null && path.length() > 0) {
            bs.writeUtf8(path);
        }
    }
}
