# DiPlay Okavango 0.3.0

面向 2023 菲律宾 Geely Okavango E01 / MT6735 / Android 5.1（API 22）。

- 保留已验证的有线 CarPlay，以及 ECARX/PopView 方向盘上一首、下一首通路。
- 保留独立的 Android MEDIA_BUTTON / MediaSession 通路和来电时的按键保护。
- 删除 BYD HUD、仪表显示、车辆数据探测、车辆控制输出和 Android 10 扫描暂停后台任务。
- 删除 API19 Multidex 引导、T3 专用设备测试和低版本媒体缓冲访问分支。
- 只提供英文、简体中文及固定的无字母 Geely 车标。
- 正式 APK 使用固定签名、版本码 72；后续同签名且更高版本可覆盖安装。
- 应用更新渠道改为 DreamHoa 本仓库；不再尝试安装上游 APK。
- 改进构建缓存，保留必要测试与 Lint。

旧的临时 debug 签名包可能需要先卸载。首次卸载会清除该包设置和配对数据。
无线 CarPlay 在此 Okavango 上仍未实车验证。
本次清理后的 USB、音频、触摸、方向盘和重连仍需实车回归。

认证包包含可提取材料，Release 默认保留为草稿，仅供授权范围内使用。
