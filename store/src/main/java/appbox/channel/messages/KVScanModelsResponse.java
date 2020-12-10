package appbox.channel.messages;

import appbox.data.PersistentState;
import appbox.model.ModelBase;
import appbox.serialization.IInputStream;

public final class KVScanModelsResponse extends KVScanResponse {
    public ModelBase[] models;

    @Override
    public void readFrom(IInputStream bs) {
        reqId     = bs.readInt();
        errorCode = bs.readInt();

        if (errorCode == 0) {
            skipped = bs.readInt();
            length  = bs.readInt();

            models = new ModelBase[length];
            for (int i = 0; i < length; i++) {
                bs.skip(bs.readNativeVariant()); //跳过Row's Key
                bs.readNativeVariant(); //跳过Row's Value size;
                models[i] = ModelBase.makeModelByType(bs.readByte());
                models[i].readFrom(bs);
                //处理变更状态
                if (models[i].persistentState() != PersistentState.Unchnaged) {
                    models[i].acceptChanges();
                }
            }
        }
    }
}
