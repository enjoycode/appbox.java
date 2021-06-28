# 虚拟工程目录结构示例：

/ (Workspace)
/models (Project)
/models/DataStore.java
/models/基础虚拟.java
/models/sys/基础虚拟.java
/models/sys/Permissions.java
/models/sys/entities/Order.Java
/models/sys/enums/OrderStatus.java
/models/sys/services/TestService.java (服务代理)

每个服务模型(设计时)对应一个Project
/服务模型标识[eg:123456] (Project)
/服务模型标识[eg:123456]/OrderService.java
