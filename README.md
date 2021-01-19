
# 工程目录结构
```shell
core                基础类库
|----cache          缓存相关(对象池等)
|----channel        与主进程通讯的消息通道及消息接口等
|----data           基础数据结构
|----design         设计时相关
|----expressions    表达式定义
|----logging        日志相关
|----model          模型定义
|----runtime        运行时相关
|----serialization  序列化相关

design              设计时类库
|----common         设计时通用数据结构
|----handlers       与前端IDE交互的设计时命令
|----jdt            修改的JDT,用于虚拟工程代码
|----services       设计时的服务(代码分析转换、新建发布模型等)
|----tree           设计树结构

store               存储类库
|----channel        与主进程通讯的存储类消息定义
|----entities       系统内置的实体模型定义
|----store          各类存储的实现

host                子进程入口，由主进程启动
|----channel        与主进程通讯的共享内存通道及相关消息定义
|----server         运行时上下文及服务模型字节码管理
```
