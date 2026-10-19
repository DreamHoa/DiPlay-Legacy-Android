# 构建

目标：2023 菲律宾 Geely Okavango，Android 5.1 / API 22，ARMv7 + ARM64。

使用 JDK 25、Gradle Wrapper 9.5.0、Android SDK 37.0、Build Tools 36.0.0、NDK 25.2.9519653。
保留现有编译工具链，避免把代码清理和依赖升级混在一起。

```sh
./gradlew :shared:testDebugUnitTest :common:testDebugUnitTest :mobile:lintDebug :mobile:assembleDebug --build-cache
```

可选设备测试包：`./gradlew :mobile:assembleDebugAndroidTest`。
本次移除了 KitKat/T3 专用 instrumentation 引导，改用标准 AndroidJUnitRunner。
实车运行 `com.shihab.diplay.okavango.test/androidx.test.runner.AndroidJUnitRunner`，
用 `-e class com.shilapi.xcertplay.OkavangoDeviceRuntimeTest` 选择只读能力检测。
设备测试 APK 与被测应用必须使用匹配的签名；普通 debug 测试包不用于检测固定签名的已安装 Release。

签名、认证材料及发布流程见 [RELEASE_SIGNING.md](RELEASE_SIGNING.md)。

## 为什么以前慢

基准：Actions run `39184208336`，job 约 9 分 20 秒。

| 阶段 | 耗时 |
| --- | --- |
| SDK 准备 | 23 秒 |
| 测试、Lint 及它们依赖的编译 | 7 分 58 秒 |
| 最后的认证 debug APK 打包 | 42 秒 |

不能把全部 8 分钟算成测试本身：这个阶段也包含依赖解析、源码及测试编译、资源处理和 Lint。
原 setup-gradle 默认只允许默认分支 main 写缓存，而实际开发 push 在 okavango-api22；
Gradle task-output cache 原来也没有启用。每次都重新做了很多工作。

改进：

- 显式允许可信分支写 Gradle 缓存，并启用 task-output cache。
- 单独缓存 Robolectric 的 Android 运行库（当前测试集约 770 MB），它们不在 Gradle 默认缓存目录内。
- 源码检查 job 内复用 Gradle daemon；保留单元测试和 Lint。
- 所有模块最低 API 统一为 22；原生代码仅编译 ARMv7/ARM64。
- 删除 BYD HUD/仪表/车辆数据源码及其专用测试、API19 的 Multidex 引导。
- Google Maven 只查询 Android/AndroidX 相关依赖，避免向它逐个查询 Maven Central 库。
- 源码检查 job 不接触 Secrets；认证打包使用独立 runner，不恢复/保存缓存，并禁用配置缓存和输出缓存。
- 日常 push 不再重复构建认证 APK；仅手动从 main 创建正式草稿 Release 时构建。

首次冷构建仍需下载工具和依赖。固定签名 Release 会比最后的 debug 打包多编译一个变体；
实际提速应以新 Actions 的冷/热缓存记录为准，不承诺每次固定分钟数。
