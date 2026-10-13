# Okavango development branch

Base: programmerguohuajing/DiPlay-Legacy-Android, commit
`d386e54a9369a0721943871bb7c79b4163905530` (0.2.46).

This development build installs separately as `com.shihab.diplay.okavango`,
requires Android API 22+, and packages ARM64 and ARMv7 libraries.

The application profile initializes missing display preferences once. Defaults
select H.264, 30 fps, 67% resolution and direct SurfaceView output. Saved user
settings are retained. The default wireless backend is Existing Wi-Fi; no default
password is provisioned. The profile does not enable radios, change routing,
switch vendor services, obtain root or modify firmware.

Existing Wi-Fi still requires a functioning Bluetooth iAP2 transport. A successful
Wi-Fi attachment or a passing build is not proof of a working CarPlay session.

Ordinary push CI runs the shared/common unit tests, the mobile app lint and
the mobile debug build. It skips the home/maphost samples and device-test APK on
ordinary pushes to shorten the feedback loop; the complete project checks remain
documented in AGENTS.md. Ordinary source artifacts deliberately have no MFi identity.

For an authenticated car-test APK, the manual `Okavango MFi car-test APK` workflow
is defined on the default `main` branch and checks out `okavango-api22`. It
requires two repository Actions Secrets, `DIPLAY_MFI_PK8_B64` and
`DIPLAY_MFI_P7B_B64`, containing Base64-encoded authorized local identity
files. Gradle validates that the key and certificate match, then builds a debug
APK and verifies its Android signing and packaged MFi files. Download the
`okavango-mfi-test` artifact. No secret is committed to Git. However, the
identity *is extractable* from any APK containing it, so do not distribute
the car-test build beyond its authorized audience. GitHub-hosted debug signing
does not guarantee update compatibility between runs: plan to retain a stable
signing key for repeat installations. Neither private keys nor device diagnostic
logs belong in source control. Physical iPhone pairing still needs car testing.

The device instrumentation suite can be explicitly selected using:

```sh
adb shell am instrument -w -r -e class OkavangoDeviceRuntimeTest \
  com.shihab.diplay.okavango.test/com.shilapi.xcertplay.T3LegacyInstrumentation
```

This exercises on-device cryptography and H.264 decoding. It does not pair a phone
or start a wireless session. Keep upstream source and license notices intact.
