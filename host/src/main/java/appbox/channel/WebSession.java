package appbox.channel;

import appbox.data.EntityId;
import appbox.data.TreeNodePath;
import appbox.design.DesignHub;
import appbox.design.IDeveloperSession;

import java.util.UUID;

public final class WebSession implements IDeveloperSession {
    private final long         _id;
    private       DesignHub    _designHub;
    public final EntityId employeeId;
    public final  TreeNodePath treePath;

    public WebSession(long id, TreeNodePath path, EntityId employeeId) {
        _id      = id;
        treePath = path;
        this.employeeId = employeeId;
    }

    @Override
    public String name() {
        return treePath.getAt(0).text;
    }

    @Override
    public boolean isExternal() {
        return false;
    }

    @Override
    public String tag() {
        return null;
    }

    @Override
    public long sessionId() {
        return _id;
    }

    @Override
    public int levels() {
        return 0;
    }

    @Override
    public TreeNodePath.TreeNodeInfo getAt(int level) {
        return null;
    }

    @Override
    public UUID leafOrgUnitId() {
        return this.employeeId == null ? treePath.getAt(1).id : treePath.getAt(0).id;
    }

    @Override
    public UUID emploeeId() {
        return employeeId.toUUID();
    }

    @Override
    public UUID externalId() {
        return null;
    }

    @Override
    public synchronized DesignHub getDesignHub() {
        if (_designHub == null) {
            //TODO:创建DesignHub实例前，判断当前用户是否具备开发者权限
            _designHub = new DesignHub(this);
            _designHub.typeSystem.init(); //注意创建完后初始化
        }
        return _designHub;
    }

    @Override
    public void sendEvent(int source, String body) {

    }
}
