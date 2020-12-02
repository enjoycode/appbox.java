package appbox.channel.messages;

import appbox.serialization.BinSerializer;
import appbox.store.KVTxnId;
import appbox.store.KeyUtil;

public final class KVInsertAssemblyRequest extends KVInsertRequire {

    private final String  asmName;
    private final byte[]  asmData;
    private final boolean isService;

    public KVInsertAssemblyRequest(KVTxnId txnId, String asmName, byte[] asmData, boolean isService) {
        super(txnId);

        this.asmName   = asmName;
        this.asmData   = asmData;
        this.isService = isService;

        raftGroupId    = KeyUtil.META_RAFTGROUP_ID;
        schemaVersion  = 0;
        dataCF         = -1;
        overrideExists = true;
    }

    @Override
    public void writeTo(BinSerializer bs) {
        super.writeTo(bs);

        //key
        KeyUtil.writeAssemblyKey(bs, isService, asmName, true);
        //refs
        bs.writeVariant(0);
        //data
        bs.write(asmData, 0, asmData.length);
    }

}
