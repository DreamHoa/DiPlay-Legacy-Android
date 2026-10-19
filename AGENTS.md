# DiPlay — Geely Okavango (Philippines, 2023)

This repository's **`main`** branch is for one verified Android head unit, not a general BYD/DiLink or multi-car project. See `README.md` / `README.zh-CN.md` for current status. Keep changes minimal and explain them to the user in Chinese.

## Reference vehicle

- Geely Okavango E01: Android **5.1 / API 22**, MT6735, device `VF12LQJ`.
- Observed firmware: `SWVX11A0126H5173.00036`. Check `OkavangoProfile.matches` before invoking any vehicle-specific service.
- Only **English** and **Simplified Chinese** app resources; the brand preset is **Geely** only.

## Protect the proven paths

- Wired CarPlay / USB / iAP2 / authorization already works on this vehicle. Do not rewrite the transport/authentication chain while cleaning unrelated code.
- Steering-wheel next and previous were physically confirmed after commit `413812c`. The factory `com.neusoft.optimus.wheeljack.popview` service exposes the ECARX Binder callback; `GeelyWheelKeys.kt` maps **163 to NEXT** and **165 to PREVIOUS**, then forwards CarPlay HID. Preserve binder registration stability, duplicate suppression, and call/audio-focus/session checks.
- Android standard `MEDIA_BUTTON` + `MediaSession` (including 87/88 input) is a **separate, retained path**. `413812c` was additive; do not remove the receiver, manifest entry, or API 22 callback based only on the AIDL path working.
- Wireless CarPlay is **not verified on this Okavango**. Never treat another Geely E01's private Bluetooth daemon replacement, a Wi-Fi toggle, or a simulated test as on-car proof.

## Cleanup boundaries

- Android 4.4 `RemoteControlClient`, non-Geely brand images, non-English/Chinese translations, legacy demo apps and obsolete BYD-only call-key broadcast handling have been removed.
- BYD HUD, instrument-display routing and vehicle-data probes/writers have been deleted. Generic phone navigation/now-playing decoding and call state live in `shared/.../navigation`; do not reintroduce vendor workers.
- Do not access root/system partitions, overwrite Bluetooth daemons, or send unverified vehicle commands as a side effect of cleanup.
- Keep original upstream copyright notices and third-party license obligations; never commit MFi keys, certificates or signing identities.

## Required checks

GitHub Actions on `main` runs:

```sh
./gradlew :shared:testDebugUnitTest :common:testDebugUnitTest :mobile:lintDebug --no-daemon
```

Do **not** invoke `:home` or `:maphost`: those modules were deleted. Also compile `:mobile:assembleDebug` for source verification. Push and PR checks must not read private inputs. Only a manual release from `main`, after checks pass, builds `:mobile:assembleStandaloneRelease` in the `okavango-release` environment with explicit strong signing passwords and authentication Secrets. It verifies the actual API22 APK and uploads it directly to a draft Release. Never use public Actions artifacts or shared caches for authenticated packages. No random-signed car APK is distributed. See `docs/RELEASE_SIGNING.md`. Releases are created as drafts because authenticated APKs contain extractable credentials.

A source review, unit-test pass or green CI does **not** replace final in-car USB, audio, touch, previous/next and reconnection tests. Report what passed and what remains unverified; avoid accumulating device-specific hacks.
