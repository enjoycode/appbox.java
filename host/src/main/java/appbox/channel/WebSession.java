package appbox.channel;

import appbox.data.TreeNodePath;
import appbox.runtime.ISessionInfo;

import java.util.UUID;

public final class WebSession implements ISessionInfo {
    private final long   _id;
    private final String _name;

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
}
