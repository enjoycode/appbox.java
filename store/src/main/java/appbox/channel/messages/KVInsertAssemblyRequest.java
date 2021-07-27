package appbox.channel.messages;

import appbox.serialization.IOutputStream;
import appbox.store.KVTxnId;
import appbox.store.KVUtil;
import appbox.store.MetaAssemblyType;

public final class KVInsertAssemblyRequest extends KVInsertRequire {

    private final String           asmName;
    private final byte[]           asmData;
    private final MetaAssemblyType asmType;

    //TODO:支持传入BytesOutputStream，避免复制为byte[]

    public KVInsertAssemblyRequest(KVTxnId txnId, String asmName, byte[] asmData, MetaAssemblyType asmType) {
        super(txnId);

        this.asmName = asmName;
        this.asmData = asmData;
        this.asmType = asmType;

        raftGroupId    = KVUtil.META_RAFTGROUP_ID;
        schemaVersion  = 0;
        dataCF         = -1;
        overrideExists = true;
    }

    @Override
    public void writeTo(IOutputStream bs) {
        super.writeTo(bs);

        //key
        KVUtil.writeAssemblyKey(bs, asmType, asmName, true);
        //refs
        bs.writeVariant(0);
        //data
        bs.write(asmData, 0, asmData.length);
    }

}
