package appbox.channel.messages;

import appbox.data.PersistentState;
import appbox.model.ApplicationModel;
import appbox.model.DataStoreModel;
import appbox.model.ModelBase;
import appbox.model.ModelFolder;
import appbox.serialization.IInputStream;
import appbox.store.KVUtil;

import java.util.ArrayList;
import java.util.List;

public final class KVScanModelResponse extends KVScanResponse {
    public List<ApplicationModel> apps;
    public List<DataStoreModel> stores;
    public List<ModelFolder> folders;
    public List<ModelBase> models;

    @Override
    public void readFrom(IInputStream bs) {
        reqId     = bs.readInt();
        errorCode = bs.readInt();

        if (errorCode != 0)
            return;

        skipped = bs.readInt();
        length  = bs.readInt();
        int keySize, valueSize = 0;
        byte[] key = new byte[128]; //TODO:最长的

        for (int i = 0; i < length; i++) {
            keySize = bs.readNativeVariant(); //Row's KeySize
            bs.readBytes(key, 0, keySize); //Row's Key
            valueSize = bs.readNativeVariant(); //Row's ValueSize
            switch (key[0]) {
                case KVUtil.METACF_APP_PREFIX:
                    readApp(bs);
                    break;
                case KVUtil.METACF_FOLDER_PREFIX:
                    readFolder(bs);
                    break;
                case KVUtil.METACF_MODEL_PREFIX:
                    readModel(bs);
                    break;
                default:
                    throw new RuntimeException("Unknown model type");
            }
        }
    }

    private void readApp(IInputStream bs) {
        var app = new ApplicationModel();
        var appStoreId = bs.readByte();
        var devIdSeq   = bs.readInt();
        var usrIdSeq   = bs.readInt();
        app.readFrom(bs);
        app.setAppStoreId(appStoreId);
        app.setDevModelIdSeq(devIdSeq);

        if (apps == null)
            apps = new ArrayList<>();
        apps.add(app);
    }

    private void readFolder(IInputStream bs) {
        var folder = new ModelFolder();
        folder.readFrom(bs);

        if (folders == null)
            folders = new ArrayList<>();
        folders.add(folder);
    }

    private void readModel(IInputStream bs) {
        var model = ModelBase.makeModelByType(bs.readByte());
        model.readFrom(bs);
        //处理变更状态
        if (model.persistentState() != PersistentState.Unchnaged) {
            model.acceptChanges();
        }

        if (models == null)
            models = new ArrayList<>();
        models.add(model);
    }

}
