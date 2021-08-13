# 虚拟工程目录结构示例：
DevName = 当前开发者的名称

/ (Workspace)
/DevName_models (Project)
/DevName_models/DataStore.java
/DevName_models/基础虚拟.java
/DevName_models/sys/基础虚拟.java
/DevName_models/sys/Permissions.java
/DevName_models/sys/entities/Order.Java
/DevName_models/sys/enums/OrderStatus.java
/DevName_models/sys/services/TestService.java (服务代理)

每个服务模型(设计时)对应一个Project
/DevName_服务模型标识[eg:123456] (Project)
/DevName_服务模型标识[eg:123456]/OrderService.java

运行时服务
/DevName_runtime_服务模型标识/OrderService.java
/DevName_runtime_服务模型标识/sys/entities/Employee.java

