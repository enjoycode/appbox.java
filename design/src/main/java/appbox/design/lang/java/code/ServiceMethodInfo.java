package appbox.design.lang.java.code;

import java.util.ArrayList;

/** 服务方法信息,用于前端调试 */
public final class ServiceMethodInfo {
    //名称跟前端一致
    public       String                   Name;
    public final ArrayList<ParameterInfo> Args = new ArrayList<>();

    public void addParameter(String name, String type) {
        var para = new ParameterInfo();
        para.Name = name;
        para.Type = type;
        Args.add(para);
    }

    public static class ParameterInfo {
        public String Name;
        public String Type;
    }
}
