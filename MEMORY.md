# OpenCompanion — Session Memory

**Last updated**: 2026-05-05
**Current phase**: Phase 1 — Companion app MVP

## Status
- [x] Repo structure + all spec/doc files
- [x] GitHub repo live — https://github.com/BaraBonc/opencompanion
- [x] Companion app scaffolded (com.opencompanion.companion, minSdk 26)
- [x] Carer app scaffolded (com.opencompanion.carer, minSdk 26)
- [x] GitHub Actions build workflow
- [x] Gradle 8.6 wrappers, gradle.properties, proguard rules
- [x] Launcher icons — adaptive icons (companion: purple/mic, carer: teal/heart)
- [x] Firebase google-services.json in both app/ dirs (not committed — .gitignored)
- [x] All Kotlin service classes written:
      WakeWordService, BluetoothScoManager, WakeWordDetector (3-stage pipeline),
      EmergencyPhraseDetector, BootReceiver, ProactiveCheckInScheduler,
      CheckInReceiver, DailySummaryWorker, FirebaseLogger,
      CompanionMessagingService, CarerMessagingService, RemoteCheckIn,
      PairingActivity, MainActivity (both apps), SettingsActivity
- [x] SCHEDULE_EXACT_ALARM crash fixed (falls back to inexact on API 31+)
- [x] build/ dirs removed from git, .gitignore fixed
- [x] 3-stage openWakeWord pipeline implemented in WakeWordDetector:
      raw PCM → melspectrogram.tflite → embedding_model.tflite → wakeword.tflite
- [x] Both apps build and install successfully
- [x] Race condition crash fixed — accumulation moved into coroutine local scope
- [ ] End-to-end test: wake word → ChatGPT voice
- [ ] "Hello Zoli" TFLite model trained (before September delivery)

## Next session priorities
1. Open app, grant mic + BT permissions, confirm it stays running (crash fixed)
2. Set ChatGPT as default assistant: Settings → Apps → Default apps → Digital assistant app
3. Say **"Alexa"** (stand-in model) → confirm ChatGPT opens in voice mode
4. If working: begin training real "Hello Zoli" openWakeWord model

## Active decisions log
2026-05-05 — TFLite + openWakeWord, 3-stage pipeline confirmed
2026-05-05 — Firebase Spark tier, anonymous auth, 6-digit pairing
2026-05-05 — ACTION_ASSIST intent for AI handoff (no API cost)
2026-05-05 — Bluetooth SCO for external speakers, phone mic fallback
2026-05-05 — openWakeWord alexa_v0.1.tflite as stand-in (trigger: "Alexa")
             Real "Hello Zoli" model to be trained before September delivery
2026-05-05 — Digital assistant app setting: Settings → Apps → Default apps
             → Digital assistant app (or long-press Home → tap corner icon)

## TFLite model shapes (confirmed)
melspectrogram.tflite:  IN [1, N] float32 → OUT [1, 8, 1, 32]  (N=1280 samples)
embedding_model.tflite: IN [1, 76, 32, 1]  → OUT [1, 1, 1, 96]
wakeword model:         IN [1, 16, 96]      → OUT [1, 1]
Warmup: ~2s (fills both sliding windows before first score)

## Asset files (companion-app/app/src/main/assets/wakewords/)
melspectrogram.tflite, embedding_model.tflite — pipeline models
hello_zoli.tflite, hello_nana.tflite, hey_helper.tflite, call_zoli.tflite
  — all currently alexa_v0.1 stand-in (836KB each)

## Environment
ANDROID_HOME=/home/barabonc/android-sdk (in .bashrc)
JDK 17: /usr/lib/jvm/java-17-openjdk-amd64
Device: Pixel 6 Pro on 192.168.0.69 (ADB wireless, reconnect if port changes)
Build: cd companion-app && ANDROID_HOME=~/android-sdk ./gradlew assembleDebug
Install: adb -s 192.168.0.69:39049 install -r app/build/outputs/apk/debug/app-debug.apk

## Reference files
Full context: PROJECT.md | Companion spec: companion-app/SPEC.md | Carer spec: carer-app/SPEC.md
