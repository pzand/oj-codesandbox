**测试功能：**
- 通过判断容器的文件diff，来确认是否删除容器

**问题：**
- 通过docker容器的status来获取内存 不准确
- docker容器运行的错误信息不完整

**需要的改进**
- 通过控制台标准输入 或者 编写`Driver`驱动类，来准确获取内存，时间，错误信息