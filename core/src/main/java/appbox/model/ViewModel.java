package appbox.model;

import appbox.serialization.IBinSerializable;
import appbox.serialization.IInputStream;
import appbox.serialization.IJsonWriter;
import appbox.serialization.IOutputStream;

public class ViewModel extends ModelBase implements IBinSerializable {

    //region ====Fields & Properties====
    private ViewModelFlag flag = ViewModelFlag.None;
    private ViewModelType type = ViewModelType.Vue;

    /** 自定义路由的上级，指向视图名称eg: sys.Home */
    private String routeParent;
    /**
     * 自定义路由的路径，未定义则采用默认路径如: /erp/CustomerList
     * 如设置RouteParent但不定义，则为/Parent/thisViewName
     */
    private String routePath;
    /** 列入路由或菜单所对应的权限模型标识 */
    private long   permissionID;

    @Override
    public ModelType modelType() {
        return ModelType.View;
    }

    public ViewModelFlag getFlag() {
        return flag;
    }

    public void setFlag(ViewModelFlag flag) {
        this.flag = flag;
    }

    public String getRouteParent() {
        return routeParent;
    }

    public void setRouteParent(String routeParent) {
        this.routeParent = routeParent;
    }

    public String getRoutePath() {
        return routePath;
    }

    public void setRoutePath(String routePath) {
        this.routePath = routePath;
    }

    public long getPermissionID() {
        return permissionID;
    }

    public void setPermissionID(long permissionID) {
        this.permissionID = permissionID;
    }

    /** 仅用于模型存储 */
    public String getRouteStoredPath() {
        return routeParent == null || routeParent.equals("") ? routePath : String.format("%s;%s", routeParent, routePath);
    }
    //endregion

    public ViewModel() {}

    public ViewModel(long id, String name) {
        super(id, name);
    }

    //region ====Serialization====
    @Override
    public void writeTo(IOutputStream bs) {
        super.writeTo(bs);

        bs.writeByteField(flag.value, 1);
        bs.writeByteField(type.value, 2);
        bs.writeLongField(permissionID, 3);
        if (routePath != null && !routePath.isEmpty())
            bs.writeStringField(routePath, 4);
        if (routeParent != null && !routeParent.isEmpty())
            bs.writeStringField(routeParent, 5);

        bs.finishWriteFields();
    }

    @Override
    public void readFrom(IInputStream bs) {
        super.readFrom(bs);

        int propIndex;
        do {
            propIndex = bs.readVariant();
            switch (propIndex) {
                case 1:
                    flag = ViewModelFlag.fromValue(bs.readByte()); break;
                case 2:
                    type = ViewModelType.fromValue(bs.readByte()); break;
                case 3:
                    permissionID = bs.readLong(); break;
                case 4:
                    routePath = bs.readString(); break;
                case 5:
                    routeParent = bs.readString(); break;
                case 0:
                    break;
                default:
                    throw new RuntimeException("Unknown field id: " + propIndex);
            }
        } while (propIndex != 0);
    }

    public void writeToJson(IJsonWriter writer) {
        writer.writeKeyValue("Route", (flag.value & ViewModelFlag.ListInRouter.value) == ViewModelFlag.ListInRouter.value);
        writer.writeKeyValue("RouteParent", routeParent);
        writer.writeKeyValue("RoutePath", routePath);
    }
    //endregion

    //region ====Enums====
    public enum ViewModelFlag {
        None(0), ListInRouter(1);
        public final byte value;

        ViewModelFlag(int v) {
            value = (byte) v;
        }

        public static ViewModelFlag fromValue(byte v) {
            if (v == 0) return None;
            else if (v == 1) return ListInRouter;
            throw new RuntimeException("Unknown value: " + v);
        }
    }

    public enum ViewModelType {
        Vue(0), VisualVue(1);
        public final byte value;

        ViewModelType(int v) { value = (byte) v;}

        public static ViewModelType fromValue(byte v) {
            if (v == 0) return Vue;
            else if (v == 1) return VisualVue;
            throw new RuntimeException("Unknown value: " + v);
        }
    }
    //endregion

}
