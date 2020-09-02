package appbox.channel;

import appbox.data.TreeNodePath;
import appbox.design.DesignHub;
import appbox.design.IDeveloperSession;

import java.util.UUID;

public final class WebSession implements IDeveloperSession {
    private final long      _id;
    private final String    _name;
    private       DesignHub _designHub;

    public WebSession(long id, String name) {
        _id   = id;
        _name = name;
    }

    @Override
    public String name() {
        return _name;
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
        return null;
    }

    @Override
    public UUID emploeeId() {
        return null;
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
        }
        return _designHub;
    }

    @Override
    public void sendEvent(int source, String body) {

    }
}
