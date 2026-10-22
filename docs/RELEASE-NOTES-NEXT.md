# DiPlay Okavango v0.3.2 (versionCode 74) — signed car-test candidate

Target: 2023 Philippines Geely Okavango, MT6735, Android 5.1 / API 22.

## Integrated changes

- Retain v0.3.1 wired session state fix (do not reset established AirPlay on duplicate iAP2 availability).
- Port MarioZero99 Geely E01 bounded touch dispatch: coalesce only pending MOVE events; keep DOWN, UP, POINTER and CANCEL in order. Keep Okavango letterbox mapping, gesture interception and media buttons.
- Record touch event-origin time from Android MotionEvent for touch-to-frame statistics.
- Port direct Annex-B writing into MediaCodec input ByteBuffer, avoiding one full per-frame video allocation. Keep existing Okavango presentation timestamps, render pacing, keyframe and recovery logic.
- Reuse MediaCodec.BufferInfo objects for the video and audio decoder threads.
- On audio decoder initialization failure, retry creation up to five times, 1 second apart; for Opus first try Android's software OMX decoder (if present). This is not an external libopus implementation.
- Port upstream DiPlay #962's wired late-NCM recovery: track first successful outbound USB packet and iPhone TCP SYN to the AirPlay port; resend StartSession at most once if the USB network came up after the first request and no connection attempt followed. Preserve the 30-second first-TCP startup watchdog for each attempt.
- Add JVM tests covering touch queue, Annex-B write and reference detection, late USB decision and Ethernet TCP SYN tracking.

## Deliberately not included

- Root/usbfs direct access fallback from Legacy v0.2.83: Okavango has a proven working USB/NCM transport. Switching the kernel access path is a separate, unverified and privileged hardware experiment.
- Upstream MediaClockTrim change from DiPlay #993: primarily addresses bursty Wi-Fi audio; this Android 5.1 wired build has no corresponding MediaClockTrim code. Do not introduce an unrelated playback-rate controller.
- Vendor Bluetooth patches for Geely E01 wireless: Okavango's Bluetooth firmware and vendor service are different, and wireless is not validated.
- Instrument cluster patches for Android 11 geely_fx11: not applicable to the API22 Okavango.
- Replacing the existing current advertisement/charging policy without electrical measurements.

## Real-car validation (required before accepting as stable)

1. Install the authenticated **draft Release** APK using the existing fixed signer (same appId).
2. Plug iPhone into the vehicle USB and wait for CarPlay UI; verify taps, rapid horizontal scroll, multitouch, drag/release and three-finger menu gesture.
3. Leave CarPlay connected **without audio or touch for 3–5 minutes**. Verify no repeated disconnect, first-TCP timeout or UI relaunch.
4. Play music continuously for 10+ minutes, trigger Siri, navigation prompts and a call, and inspect audio decoder failures and dropouts.
5. Confirm Geely ECARX/PopView next/previous steering keys, Android MEDIA_BUTTON path, unplug/replug and app restart.
6. If idle reconnection remains, save a diagnostic report and inspect iAP2 StartSession, NCM bulk-out readiness, TCP SYN and AirPlay RECORD before changing the USB stack.

**Code and CI checks do not establish physical success on this head unit.**

The authenticated APK contains extractable credential material. Never upload it to a public Actions artifact or publish this draft Release.
