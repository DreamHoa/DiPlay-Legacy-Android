# DiPlay · Geely Okavango (Android 5.1)

A vehicle-specific CarPlay receiver for the **2023 Philippine-market Geely Okavango**. This work is maintained on the **`okavango-api22`** branch, not the generic upstream `main` branch.

## Target head unit

| | |
|---|---|
| Model | VF12LQJ (Geely / ECARX head unit) |
| Hardware | MediaTek MT6735 |
| Android | 5.1, API 22 |
| Display | 1920 × 720 |
| Firmware observed | `SWVX11A0126H5173.00036` |

This fork prioritizes predictable operation and minimal overhead on **this head unit**, not compatibility with BYD or other Android vehicles.

## Working and experimental features

- **Wired CarPlay:** verified on the reference Okavango. Requires a proper USB data connection and the separately provisioned accessory authentication assets.
- **Steering-wheel previous/next:** verified by the owner. The factory PopView service exposes presses through an ECARX Binder/AIDL listener; `GeelyWheelKeys.kt` interprets `163` (next) and `165` (previous), suppresses repeats, and forwards the commands as CarPlay media HID messages.
- **Android media controls:** existing `MEDIA_BUTTON` / MediaSession forwarding is retained independently. The `413812c` steering-wheel change was additive, and simulated Android `87/88` input also worked. These paths must not be removed without a replacement test.
- **Wireless CarPlay:** **not verified working** on this Okavango. The Bluetooth vendor transport is not equivalent to the proprietary E01 adapter used by other Geely forks. Do not assume a Wi-Fi connection alone establishes wireless CarPlay.

## Language and platform scope

The application packages **English and Simplified Chinese** translations only. The language picker offers these two languages and a system-default choice, with English as the resource fallback for other system languages. Brand presets and bundled brand images are restricted to **Geely**.

This is a fork of an older multi-vehicle DiPlay tree. Some inherited BYD-specific Java/Kotlin features are still interleaved with the settings and session implementations and have **not** all been removed. Obsolete BYD HUD assets, demos, and reference documents were removed first; remaining interdependent source must be removed only with compile and vehicle-regression checks. No BYD code is required for the PopView steering-wheel listener.

## Build and diagnostics

GitHub Actions on `okavango-api22` runs unit tests and lint, then builds an authenticated test APK from repository secrets:

[Android build workflow](.github/workflows/android.yml)

- Package: `com.shihab.diplay.okavango`
- Android minSdk: **22**
- Supported native ABIs: ARMv7 / ARM64
- CarPlay authentication inputs are never committed to this repository.
- An APK containing provisioned authentication assets is sensitive; do not distribute the test artifact publicly.

Use ADB logs tagged `DiPlay-GeelyWheel` and `DiPlay-MediaKeys` when checking the factory callback and the CarPlay forwarding pipeline. Never modify vendor Bluetooth services, system partitions, or root settings simply to install the application.

## Attribution

Based on [programmerguohuajing/DiPlay-Legacy-Android](https://github.com/programmerguohuajing/DiPlay-Legacy-Android) and earlier [DiPlay](https://github.com/shihabal3amri/DiPlay). Existing copyright notices, open-source license obligations, and retained third-party source notices apply. This is not an Apple-certified accessory.
