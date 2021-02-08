package appbox.channel;

import appbox.channel.messages.ForwardMessage;
import appbox.data.EntityId;
import appbox.data.TreeNodePath;
import appbox.design.DesignHub;
import appbox.design.IDeveloperSession;
import appbox.logging.Log;
import appbox.runtime.RuntimeContext;
import appbox.serialization.BytesInputStream;
import appbox.serialization.BytesOutputStream;
import appbox.server.runtime.HostRuntimeContext;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class WebSession implements IDeveloperSession {
    private final long         _id;
    private       DesignHub    _designHub;
    public final  EntityId     employeeId;
    public final  TreeNodePath treePath;

    public WebSession(long id, TreeNodePath path, EntityId employeeId) {
        _id             = id;
        treePath        = path;
        this.employeeId = employeeId;
    }

    //region ====IUserSession====
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
        return treePath.level();
    }

    @Override
    public TreeNodePath.TreeNodeInfo getAt(int level) {
        return treePath.getAt(level);
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
    //endregion

    //region ====IDeveloperSession====
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
    public CompletableFuture<Void> startDebugChannel(String service, byte[] invokeArgs) {
        return DebugSessionManager.startDebugChannel(this, service, invokeArgs);
    }

    @Override
    public void sendEvent(IClientMessage event) {
        var msg     = new ForwardMessage(_designHub.session.sessionId(), MessageType.Event, event);
        var channel = ((HostRuntimeContext) RuntimeContext.current()).channel;
        try {
            channel.sendMessage(channel.newMessageId(), msg);
        } catch (Exception ex) {
            Log.warn("Can't forward event message");
        }
    }

    @Override
    public byte[] getSerializedData() {
        var output = new BytesOutputStream(256);
        output.writeLong(_id);
        employeeId.writeTo(output);
        output.writeVariant(treePath.level());
        for (int i = 0; i < treePath.level(); i++) {
            var node = treePath.getAt(i);
            output.writeLong(node.id.getLeastSignificantBits());
            output.writeLong(node.id.getMostSignificantBits());
            output.writeString(node.text);
        }
        return output.toByteArray();
    }

    /** 仅用于调试时反序列化会话 */
    public static WebSession fromSerializedData(byte[] data) {
        var input      = new BytesInputStream(data);
        var id         = input.readLong();
        var employeeId = new EntityId();
        employeeId.readFrom(input);
        var levels = input.readVariant();
        var nodes  = new TreeNodePath.TreeNodeInfo[levels];
        for (int i = 0; i < levels; i++) {
            var lowbits  = input.readLong();
            var highbits = input.readLong();
            var uuid     = new UUID(highbits, lowbits);
            var text     = input.readString();
            nodes[i] = new TreeNodePath.TreeNodeInfo(uuid, text);
        }
        var treePath = new TreeNodePath(nodes);
        return new WebSession(id, treePath, employeeId);
    }
    //endregion

}
