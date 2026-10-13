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

GitHub Actions builds source APKs and runs the checks required by AGENTS.md. The
downloadable artifact has no runtime authentication assets and must not be
installed as a complete standalone receiver. The owner must select and validate
the runtime identity locally, package it into the APK and sign the result with a
persistent local Android signing key. Neither private keys nor device diagnostic
logs belong in this repository or its build artifacts.

The device instrumentation suite can be explicitly selected using:

```sh
adb shell am instrument -w -r -e class OkavangoDeviceRuntimeTest \
  com.shihab.diplay.okavango.test/com.shilapi.xcertplay.T3LegacyInstrumentation
```

This exercises on-device cryptography and H.264 decoding. It does not pair a phone
or start a wireless session. Keep upstream source and license notices intact.
