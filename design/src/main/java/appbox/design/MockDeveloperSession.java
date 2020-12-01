package appbox.design;

import appbox.data.TreeNodePath;

import java.util.UUID;

/**仅用于测试*/
public final class MockDeveloperSession implements IDeveloperSession {

    private final TreeNodePath _path;
    private final UUID         _emploeeId;
    private final DesignHub    _hub;

    public MockDeveloperSession() {
        var nodes = new TreeNodePath.TreeNodeInfo[] {
          new TreeNodePath.TreeNodeInfo(UUID.fromString("11111111-1111-1111-1111-111111111111"), "Admin")
        };
        _path = new TreeNodePath(nodes);
        _emploeeId = UUID.fromString("11111111-1111-1111-1111-222222222222");
        _hub = new DesignHub(this);
    }

    @Override
    public DesignHub getDesignHub() {
        return _hub;
    }

    @Override
    public void sendEvent(int source, String body) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String name() {
        return _path.getAt(0).text;
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
        return 12345678L;
    }

    @Override
    public int levels() {
        return 0;
    }

    @Override
    public TreeNodePath.TreeNodeInfo getAt(int level) {
        return _path.getAt(level);
    }

    @Override
    public UUID leafOrgUnitId() {
        return _path.getAt(0).id;
    }

    @Override
    public UUID emploeeId() {
        return _emploeeId;
    }

    @Override
    public UUID externalId() {
        return null;
    }
}

