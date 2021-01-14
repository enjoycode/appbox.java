package appbox.channel.messages;

import appbox.model.ModelFolder;
import appbox.serialization.IOutputStream;
import appbox.store.KVTxnId;
import appbox.store.KeyUtil;

public final class KVInsertFolderRequest extends KVInsertRequire {

    private final ModelFolder folder;

    public KVInsertFolderRequest(ModelFolder folder, KVTxnId txnId) {
        super(txnId);

        this.folder = folder;

        raftGroupId    = KeyUtil.META_RAFTGROUP_ID;
        schemaVersion  = 0;
        dataCF         = -1;
        overrideExists = true;
    }

    @Override
    public void writeTo(IOutputStream bs) {
        super.writeTo(bs);

        //key
        KeyUtil.writeFolderKey(bs, folder.appId(), folder.targetModelType(), false);
        //refs
        bs.writeVariant(0);
        //data
        folder.writeTo(bs);
    }

}
