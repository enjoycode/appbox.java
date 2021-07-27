package appbox.channel.messages;

import appbox.serialization.IOutputStream;
import appbox.store.KVTxnId;
import appbox.store.KVUtil;
import appbox.store.MetaAssemblyType;

public final class KVDeleteAssemblyRequest extends KVDeleteRequest {

    private final String           _asmName;
    private final MetaAssemblyType _type;

    public KVDeleteAssemblyRequest(KVTxnId txnId, MetaAssemblyType type, String asmName) {
        super(txnId);

        _asmName = asmName;
        _type    = type;

        raftGroupId   = KVUtil.META_RAFTGROUP_ID;
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
        KVUtil.writeAssemblyKey(bs, _type, _asmName, false);
    }
}
