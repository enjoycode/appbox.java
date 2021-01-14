package appbox.channel.messages;

import appbox.model.ModelFolder;
import appbox.serialization.IInputStream;

public final class KVScanFoldersResponse  extends KVScanResponse {

    public ModelFolder[] folders;

    @Override
    public void readFrom(IInputStream bs) {
        reqId     = bs.readInt();
        errorCode = bs.readInt();

        if (errorCode == 0) {
            skipped = bs.readInt();
            length  = bs.readInt();

            folders = new ModelFolder[length];
            for (int i = 0; i < length; i++) {
                bs.skip(bs.readNativeVariant()); //跳过Row's Key
                bs.readNativeVariant(); //跳过Row's Value size;
                folders[i] = new ModelFolder();
                folders[i].readFrom(bs);
            }
        }
    }

}
