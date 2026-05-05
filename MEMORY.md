# OpenCompanion — Session Memory

**Last updated**: 2026-05-05
**Current phase**: Phase 1 — Companion app MVP

## Status
- [x] Repo structure created
- [x] MEMORY.md, PROJECT.md, companion-app/SPEC.md, carer-app/SPEC.md written
- [x] Companion app scaffolded (package: com.opencompanion.companion, minSdk 26)
- [x] Carer app scaffolded (package: com.opencompanion.carer, minSdk 26)
- [x] GitHub Actions build workflow written
- [x] GitHub repo live — https://github.com/BaraBonc/opencompanion
- [x] WakeWordService — TFLite inference loop on AudioRecord (foreground service)
- [x] BluetoothScoManager — HFP connect/disconnect receiver, SCO ↔ phone mic
- [x] ACTION_ASSIST handoff on wake word detection
- [x] BootReceiver — restarts WakeWordService after device reboot
- [x] ProactiveCheckInScheduler — AlarmManager exact alarm + TTS + ACTION_ASSIST
- [x] EmergencyPhraseDetector — second TFLite interpreter, 880Hz tone, Firebase log
- [x] FirebaseLogger — anonymous auth, timestamped activity events
- [x] DailySummaryWorker — WorkManager, reads Firebase, pushes summary to carer
- [x] CompanionMessagingService — FCM handler (carer-initiated check-in)
- [x] SettingsActivity — wake word, sensitivity, check-in time/greeting, emergency
- [x] Gradle wrappers added (Gradle 8.6)
- [x] Launcher icons — adaptive icons for both apps
- [x] Wake word models — real openWakeWord alexa_v0.1 model as stand-in (836KB each)
- [ ] Firebase google-services.json — Viktor setting up Firebase project now
- [ ] First successful build (./gradlew assembleDebug)
- [ ] Tested on Pixel 6 Pro + JBL Link speaker
- [ ] "Hello Zoli" TFLite model trained (Phase 1b — before September delivery)

## Active decisions log
2026-05-05 — Using TFLite + openWakeWord for wake word detection
2026-05-05 — Firebase Spark tier for all backend needs
2026-05-05 — ACTION_ASSIST intent for AI handoff (no API cost)
2026-05-05 — Bluetooth SCO for external speakers, phone mic/speaker fallback
2026-05-05 — Anonymous Firebase auth + 6-digit pairing code, no user accounts
2026-05-05 — Using openWakeWord alexa_v0.1.tflite as stand-in for all four model
            slots until "Hello Zoli" model is trained

## Blocking: what Viktor needs to do
1. Firebase console: create project → enable Realtime DB + Auth (anonymous) + FCM
2. Add Android app (com.opencompanion.companion) → download google-services.json
   → drop into companion-app/app/google-services.json
3. Add Android app (com.opencompanion.carer) → download google-services.json
   → drop into carer-app/app/google-services.json
4. Run: cd ~/opencompanion/companion-app && ./gradlew assembleDebug

## Next session priorities (after Firebase JSON lands)
1. First build verification — fix any remaining compile errors
2. Sideload companion APK to Pixel 6 Pro, set ChatGPT as default assistant
3. End-to-end test: wake word detection → ChatGPT voice mode
4. Begin training real "Hello Zoli" openWakeWord model

## Reference files
Full context: PROJECT.md
Companion spec: companion-app/SPEC.md
Carer spec: carer-app/SPEC.md
