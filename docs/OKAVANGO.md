# Okavango profile

Base: programmerguohuajing/DiPlay-Legacy-Android `d386e54a9369a0721943871bb7c79b4163905530` (0.2.46).

The reference unit is E01 / VF12LQJ / MT6735, Android 5.1 API22.
The profile initializes missing preferences once: H.264, 30 fps, 67% resolution,
direct SurfaceView, Existing Wi-Fi. It preserves saved choices and never toggles radios or vendor daemons.

Wired CarPlay and previous/next were verified before this cleanup. Wireless is unverified.
Bluetooth transport support is required even with a working Wi-Fi connection.

See README, BUILD.md and RELEASE_SIGNING.md for the maintained build, signing and release process.
