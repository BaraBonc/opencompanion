# OpenCompanion — Session Memory

**Last updated**: 2026-05-05
**Current phase**: Phase 1 — Companion app MVP

## Status
- [x] Repo structure created
- [x] MEMORY.md, PROJECT.md, companion-app/SPEC.md, carer-app/SPEC.md written
- [x] Companion app scaffolded (package: com.opencompanion.companion, minSdk 26)
- [x] Carer app scaffolded (package: com.opencompanion.carer, minSdk 26)
- [x] GitHub Actions build workflow written
- [ ] Wake word detection service (TFLite + openWakeWord)
- [ ] Bluetooth SCO audio routing (auto-switch on connect/disconnect)
- [ ] ACTION_ASSIST handoff
- [ ] Daily check-in scheduler (AlarmManager + TTS)
- [ ] Emergency phrase detection
- [ ] Firebase Realtime Database activity logging
- [ ] Basic settings UI
- [ ] FCM notifications (carer app)
- [ ] Pairing system (6-digit code)
- [ ] GitHub repo created and pushed (needs gh auth login)

## Active decisions log
2026-05-05 — Using TFLite + openWakeWord for wake word detection
2026-05-05 — Firebase Spark tier for all backend needs
2026-05-05 — ACTION_ASSIST intent for AI handoff (no API cost)
2026-05-05 — Bluetooth SCO for external speakers, phone mic/speaker fallback
2026-05-05 — Anonymous Firebase auth + 6-digit pairing code, no user accounts
2026-05-05 — Min SDK 26 (ChatGPT requirement), Target SDK 34

## Reference files
Full context: PROJECT.md
Companion spec: companion-app/SPEC.md
Carer spec: carer-app/SPEC.md

## Next session priorities
1. Implement WakeWordService (TFLite inference loop on AudioRecord)
2. Implement BluetoothScoManager (HFP connect/disconnect receiver)
3. Wire ACTION_ASSIST handoff on wake word detection
4. Add BootReceiver to restart service after reboot
