package appbox.data;

import appbox.model.PermissionModel;
import appbox.serialization.IJsonSerializable;
import appbox.serialization.IJsonWriter;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** 用于运行时权限分配时包装PermissionModel形成权限树 */
public final class PermissionNode implements IJsonSerializable {
    public final String               name;
    public final PermissionModel      model;
    private      List<PermissionNode> _children;

    public PermissionNode(String folder) {
        Objects.requireNonNull(folder);

        name  = folder;
        model = null;
    }

    public PermissionNode(PermissionModel model) {
        Objects.requireNonNull(model);

        name       = model.name();
        this.model = model;
    }

    public boolean isFolder() { return model == null; }

    public List<PermissionNode> children() {
        if (model != null)
            throw new RuntimeException("Can't has children");
        if (_children == null)
            _children = new ArrayList<>();
        return _children;
    }

    @Override
    public void writeToJson(IJsonWriter writer) {
        writer.startObject();

        writer.writeKeyValue("Id", model == null ?
                UUID.randomUUID().toString() : Long.toUnsignedString(model.id()));
        writer.writeKeyValue("Name", name);

        if (_children != null && _children.size() > 0) {
            writer.writeKey("Childs");
            writer.writeList(_children);
        }

        if (model != null) {
            //注意不管有无OrgUnits都需要序列化，因为前端用于判断是否权限节点
            writer.writeKey("OrgUnits");
            if (model.hasOrgUnits())
                writer.writeList(model.orgUnits());
            else
                writer.writeEmptyArray();
        }

        writer.endObject();
    }
}
