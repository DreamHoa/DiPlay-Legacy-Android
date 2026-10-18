# DiPlay · 吉利 Okavango 专用版

本分支 **`okavango-api22`** 只针对 **2023 菲律宾版 Geely Okavango** 的车机开展适配。它不是面向其他车型的通用 CarPlay 软件。

## 目标车机

- Android **5.1 / API 22**
- 芯片：MediaTek MT6735
- 设备：Geely E01 / VF12LQJ
- 实测固件：`SWVX11A0126H5173.00036`
- 主要屏幕：1920 × 720

## 实车验证范围

- **有线 CarPlay：已验证工作。** 仍需通过私有的、经授权配置的 MFi 认证资源构建实车包；仓库内不包含这些凭据。
- **方向盘上一首／下一首：已验证工作。** 原厂 PopView Ecarx Binder/AIDL 返回按键码 **163（下一首）／165（上一首）**；`GeelyWheelKeys.kt` 负责防止连按和调用 CarPlay HID。来自 `413812c` 的此链路保留不动。
- **Android 标准媒体键：保留。** 原来的 `MEDIA_BUTTON`、`MediaSession`、按键码 87／88 路径是独立兼容入口，不能凭猜测删除。
- **无线 CarPlay：本车尚未确认成功。** 其他吉利 E01 项目的无线方案依赖不同固件及专有蓝牙模块，不可直接当成此车已验证。

## 精简范围

- 语言：**English / 简体中文**（另外提供跟随系统）；其他翻译已移除。
- CarPlay 返回车机按钮固定为 **Geely 六分格盾形标志（无 GEELY 字母）**；每次连接直接使用内置图标生成的 PNG，不再提供品牌选择、上传自定义图片及其缓存设置。
- 仅支持目标 APK **API 22**；移除了无用的 Android 4.4 旧媒体会话实现。
- 移除了其他品牌图片、比亚迪 HUD 图标／专用调试器、旧样例 App。
- 原项目仍有部分比亚迪设置、仪表和车辆状态相关源码与通用播放逻辑交织。**这些文件尚未全部清理完成**；在切断依赖并通过 CI／实车验证前，不作盲删。此车不会启动比亚迪专有服务。

## 编译和验证

推送本分支时，GitHub Actions 自动执行测试、Lint 及经授权的实车 APK 构建：

[查看 Actions 工作流](.github/workflows/android.yml)

包名 `com.shihab.diplay.okavango`，Android 最低版本 22，ARMv7/ARM64。带认证资源的 APK 不应公开分享。

源代码来自 [programmerguohuajing/DiPlay-Legacy-Android](https://github.com/programmerguohuajing/DiPlay-Legacy-Android) 及 DiPlay 上游，遵守已有开源许可。Geely 商标属于权利人，本项目非汽车厂商官方软件。
