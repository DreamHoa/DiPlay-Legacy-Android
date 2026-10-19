# DiPlay · Geely Okavango

专门适配 **2023 菲律宾版 Geely Okavango** 车机：E01 / VF12LQJ、MT6735、Android 5.1 / API 22、1920×720。
当前版本 **0.3.0 / versionCode 72**，包名 `com.shihab.diplay.okavango`。

有线 CarPlay 和方向盘上一首/下一首已在此前版本实车验证；无线仍未验证。
ECARX/PopView 回调（163 下一首、165 上一首）以及独立的 Android MEDIA_BUTTON / MediaSession 均保留。
构建通过不代替本次修改后的实车回归。

已删除：BYD HUD/仪表输出、车辆数据探测及专用后台线程、旧版 Multidex 引导、T3 专用设备测试和部分低于 API22 的兼容分支。
只保留英文、简体中文和固定的无字母 Geely 车标。USB、认证、加密及 API22 车机所需的兼容逻辑保留。

正式包改用固定签名，可生成 GitHub 草稿 Release；后续同签名、更高版本可覆盖安装。
此前的临时 debug 签名包可能需要先卸载，卸载会清除该包设置和配对数据。
应用检查更新已指向本仓库，不再指向上游。

- [构建命令与耗时分析](docs/BUILD.md)
- [一次性签名配置与发布](docs/RELEASE_SIGNING.md)
- [本次变更](docs/RELEASE-NOTES-NEXT.md)

基于 [programmerguohuajing/DiPlay-Legacy-Android](https://github.com/programmerguohuajing/DiPlay-Legacy-Android)
及更早的 DiPlay；保留原有版权及第三方许可说明。认证材料和签名私钥不进入源码。
