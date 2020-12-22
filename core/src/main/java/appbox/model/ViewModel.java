package appbox.model;

import appbox.serialization.IBinSerializable;
import appbox.serialization.IJsonWriter;

public class ViewModel extends ModelBase implements IBinSerializable {

    //region ====Fields & Properties====
    private ViewModelFlag flag = ViewModelFlag.None;
    /** 自定义路由的上级，指向视图名称eg: sys.Home */
    private String        routeParent;
    /**
     * 自定义路由的路径，未定义则采用默认路径如: /erp/CustomerList
     * 如设置RouteParent但不定义，则为/Parent/thisViewName
     */
    private String        routePath;
    /** 列入路由或菜单所对应的权限模型标识 */
    private long          permissionID;

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

    public void writeToJson(IJsonWriter writer) {
        writer.writeKeyValue("Route", (flag.value & ViewModelFlag.ListInRouter.value) == ViewModelFlag.ListInRouter.value);
        writer.writeKeyValue("RouteParent", routeParent);
        writer.writeKeyValue("RoutePath", routePath);
    }

    public enum ViewModelFlag {
        None(0), ListInRouter(1);
        public final byte value;

        ViewModelFlag(int v) {
            value = (byte) v;
        }
    }
}
