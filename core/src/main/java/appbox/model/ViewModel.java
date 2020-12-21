package appbox.model;

import appbox.serialization.IBinSerializable;
import appbox.serialization.IJsonSerializable;
import appbox.serialization.IJsonWriter;

public class ViewModel extends ModelBase implements IJsonSerializable, IBinSerializable {

    //region ====Fields & Properties====
    private ModelType modelType=ModelType.View;

    private ViewModelFlag flag;

    /**
     * 自定义路由的上级，指向视图名称eg: sys.Home
     */
    private String routeParent;

    /**
     * 自定义路由的路径，未定义则采用默认路径如: /erp/CustomerList
     * 如设置RouteParent但不定义，则为/Parent/thisViewName
     */
    private String routePath;

    /**
     * 列入路由或菜单所对应的权限模型标识
     */
    private long permissionID;

    /**
     * 仅用于模型存储
     */
    private String routeStoredPath;
//            string.IsNullOrEmpty(RouteParent) ? RoutePath : $"{RouteParent};{RoutePath}";

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

    public String getRouteStoredPath() {
        return routeParent==null||routeParent.equals("") ? routePath : String.format("%s;%s",routeParent,routePath);
    }
    //endregion

    public ViewModel() {}

    public ViewModel(long id, String name) {
        super(id, name);
    }

    @Override
    public ModelType modelType() {
        return modelType;
    }

    @Override
    public void writeToJson(IJsonWriter writer) {
        //TODO
//        writer.WriteBoolean("Route", (Flag & ViewModelFlag.ListInRouter) == ViewModelFlag.ListInRouter);
//        writer.WriteString("RouteParent", RouteParent);
//        writer.WriteString("RoutePath", RoutePath);
    }

    public enum ViewModelFlag
    {
        None(0), ListInRouter(1);
        public final byte value;

        ViewModelFlag(int v) {
            value = (byte) v;
        }
    }
}
