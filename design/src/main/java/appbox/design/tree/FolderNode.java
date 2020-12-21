package appbox.design.tree;

import appbox.model.ModelFolder;

public class FolderNode extends DesignNode{

    private ModelFolder folder;

    private int version;

    @Override
    public DesignNodeType nodeType() {
        return DesignNodeType.FolderNode;
    }

    public String id(){
        return folder.getId().toString();
    }

    public int getVersion() {
        return folder.getRoot().getVersion();
    }

    public void setVersion(int version) {
        folder.getRoot().setVersion(version);
    }

    public ModelFolder getFolder() {
        return folder;
    }

    public void setFolder(ModelFolder folder) {
        this.folder = folder;
    }
}
