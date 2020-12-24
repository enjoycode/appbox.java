package appbox.channel.messages;

import appbox.serialization.IOutputStream;
import appbox.store.KVTxnId;
import appbox.store.KeyUtil;

public final class KVDeleteViewRouteRequest extends KVDeleteRequest {

    private final String viewName;

    public KVDeleteViewRouteRequest(KVTxnId txnId, String viewName) {
        super(txnId);

        this.viewName = viewName;

        raftGroupId   = KeyUtil.META_RAFTGROUP_ID;
        schemaVersion = 0;
        returnExists  = false;
        dataCF        = -1;
    }

    @Override
    public void writeTo(IOutputStream bs) {
        super.writeTo(bs);

        //refs always 0
        bs.writeVariant(0);
        //key(不带长度)
        bs.writeByte(KeyUtil.METACF_VIEW_ROUTE_PREFIX);
        bs.writeUtf8(viewName);
    }
}
