package appbox.model;

import appbox.serialization.IBinSerializable;
import appbox.serialization.IInputStream;
import appbox.serialization.IJsonWriter;
import appbox.serialization.IOutputStream;

public class ViewModel extends ModelBase implements IBinSerializable {
    public static final byte FLAG_NONE   = 0;
    public static final byte FLAG_ROUTE  = 1; //加入路由表
    public static final byte FLAG_WIDGET = 2; //作为Widget

    public static final byte TYPE_VUE        = 0;          //普通Vue视图
    public static final byte TYPE_VISUAL_VUE = 1;   //可视化Vue视图

    //region ====Fields & Properties====
    private byte flag = FLAG_NONE;
    private byte type = TYPE_VUE;

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

    public byte getFlag() {
        return flag;
    }

    public void setFlag(byte flag) {
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

    public ViewModel(long id, String name, byte type) {
        super(id, name);
        this.type = type;
    }

    //region ====Serialization====
    @Override
    public void writeTo(IOutputStream bs) {
        super.writeTo(bs);

        bs.writeByteField(flag, 1);
        bs.writeByteField(type, 2);
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
                    flag = bs.readByte(); break;
                case 2:
                    type = bs.readByte(); break;
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
        writer.writeKeyValue("Type", type);
        writer.writeKeyValue("Route", (flag & FLAG_ROUTE) == FLAG_ROUTE);
        writer.writeKeyValue("RouteParent", routeParent);
        writer.writeKeyValue("RoutePath", routePath);
        writer.writeKeyValue("Widget", (flag & FLAG_WIDGET) == FLAG_WIDGET);
    }
    //endregion

}
