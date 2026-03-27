## 输出语言
始终使用中文输出。

## 项目环境
- **Minecraft 版本**: 26.1
- **Java 版本**: 25（非 Path 环境变量版本，需手动配置，路径：`C:/Program Files/Zulu/zulu-25`）
- **Fabric Loader**: 0.18.4
- **Fabric API**: 0.144.3+26.1

## 代码生成要求
1. **严格遵循上述版本信息**，禁止生成过时版本的代码。
2. **禁止擅自修改版本信息**，否则将导致代码生成错误。
3. **避免依赖内置知识库**，因其可能已过时。
4. **禁止使用过时的 API**，避免运行时错误。
5. **禁止修改 Gradle 相关文件**。

## 参考文档
生成代码前，必须优先查阅以下资料：
- [Fabric 官方文档](https://docs.fabricmc.net/)
- [Fabric 社区文档](https://fabricmc.net/wiki/)
- Minecraft 反编译后的本地源码(gradle生成的即可,路径为`"F:\Development\Java\tuanzis_server_mod\.gradle\loom-cache\minecraftMaven\net\minecraft\minecraft-merged-9a7fd27717\26.1\minecraft-merged-9a7fd27717-26.1-sources.jar"`)

## 必须进行
- 进行联网搜索以获取最新 API 信息。
- 阅读 Minecraft 反编译后的本地源码以确认实现细节,具体路径见上参考文档。
- 使用 gradle build 与 gradle runClient 验证代码是否被成功构建。