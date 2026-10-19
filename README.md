# DiPlay · Geely Okavango (Android 5.1)

A vehicle-specific CarPlay receiver for the **2023 Philippine-market Geely Okavango**. The Okavango code is maintained on **`main`**; `okavango-api22` retains the same migration checkpoint.

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

BYD vehicle-data probes, HUD/cluster writers, instrument-display routing and their dedicated tests have been removed. The generic phone metadata decoder remains for Android navigation widgets and call-safe wheel handling. API19 multidex bootstrap and pre-21 codec buffer access have also been removed. USB transport compatibility and cryptography used by this API22 unit remain.

## Build and release

Current release line: **0.3.0 / versionCode 72**. Package: `com.shihab.diplay.okavango`.
All modules target minimum API22; native libraries are ARMv7 and ARM64 only.

The workflow runs unit tests, lint and source compilation, then produces a non-debuggable,
fixed-signer release APK when the signing and authentication Secrets are configured.
Manual `create_release` creates a draft GitHub Release with APK, checksum and signing metadata.
The in-app updater points to this fork. It only discovers publicly published stable releases.

See [build and timing](docs/BUILD.md), [signing and releases](docs/RELEASE_SIGNING.md),
and [changes](docs/RELEASE-NOTES-NEXT.md). Previous runner-generated debug APKs may require
one uninstall before switching to the fixed signer; subsequent same-key releases update in place.
Never publish the signing backup. Authenticated APKs carry extractable accessory credentials;
release drafts remain private to repository collaborators unless explicitly published.

## Attribution

Based on [programmerguohuajing/DiPlay-Legacy-Android](https://github.com/programmerguohuajing/DiPlay-Legacy-Android) and earlier [DiPlay](https://github.com/shihabal3amri/DiPlay). Existing copyright notices, open-source license obligations, and retained third-party source notices apply. This is not an Apple-certified accessory.
