# OpenCompanion — Project Context

## What it is
Two Android apps that turn any Android phone + Bluetooth speaker into a named AI
companion for an elderly person, with a safety monitoring layer for their remote carer.

**Use case**: Viktor's elderly mother lives alone in Hungary. Viktor lives in the UK.
She speaks Hungarian, is not tech-literate. OpenCompanion lets her talk naturally to
"Zoli" (a warm Hungarian AI companion), while Viktor gets daily summaries and alerts.

## Two apps

**Companion app** (`com.opencompanion.companion`) — runs on a phone near the elder's
speaker. Listens always-on for a wake word ("Hello Zoli"). On detection, fires
`ACTION_ASSIST` to hand off to the user's AI app (ChatGPT/Gemini/Claude) in voice
mode. No API cost. Sends daily summary + alerts to carer via Firebase FCM.

**Carer app** (`com.opencompanion.carer`) — runs on the family member's phone.
Receives FCM alerts (red flags, emergency phrases, daily summary). Can trigger a
remote check-in conversation. Paired to companion app via 6-digit code.

## Architecture decisions (final — do not re-open)
| Decision | Choice | Reason |
|---|---|---|
| Language | Kotlin, native Android | No cross-platform complexity |
| AI handoff | `ACTION_ASSIST` intent | No API cost, works with any assistant app |
| Wake word | TFLite + openWakeWord | On-device, no API key, open source |
| Audio | Android AudioManager + Bluetooth SCO | Native, supports external speaker mic |
| Notifications | Firebase FCM | Free, direct device-to-device |
| Data | Firebase Realtime Database | Free tier, real-time |
| Auth | Firebase Anonymous Auth + pairing code | No accounts needed |
| Min SDK | 26 (Android 8.0) | ChatGPT minimum requirement |

## Audio routing (critical detail)
- **Mode A**: Phone mic/speaker — always available fallback
- **Mode B**: Bluetooth HFP/SCO — preferred when BT device connected
- Auto-switch via `BroadcastReceiver` on `BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED`
- `AudioManager.startBluetoothSco()` on connect, `stopBluetoothSco()` on disconnect

## Wake word pipeline
1. Background service (survives screen off, started on boot)
2. `AudioRecord` captures mic audio in 20ms chunks
3. TFLite model (openWakeWord) runs inference on each chunk
4. Score > threshold → fire `ACTION_ASSIST` → default assistant app opens

## Firebase architecture
- Realtime Database: activity log (wake detections, check-ins, alerts)
- FCM: push to carer app (alerts + daily summary)
- Anonymous Auth: device tokens, no user accounts
- Pairing: 6-digit code, 10-minute TTL, written by carer app, read by companion app

## Build phases
1. **Phase 1** (current): Companion app MVP — wake word → ChatGPT voice
2. **Phase 2**: Carer app — alerts, daily summary, pairing, remote check-in
3. **Phase 3**: Custom GPT webhook integration, red flag detection pipeline
4. **Phase 4**: In-app wake word trainer, community model library

**Deadline**: September 2026 — Viktor delivers pre-configured phone + JBL speaker to
his mother in Hungary.

## Repository structure
```
opencompanion/
├── MEMORY.md                 ← Session brief (update every session)
├── PROJECT.md                ← This file
├── companion-app/            ← Android project, com.opencompanion.companion
│   ├── SPEC.md
│   └── app/src/main/
│       ├── assets/wakewords/ ← Bundled .tflite models
│       └── java/com/opencompanion/companion/
├── carer-app/                ← Android project, com.opencompanion.carer
│   ├── SPEC.md
│   └── app/src/main/
├── training-backend/         ← Phase 4: Python wake word trainer
│   └── SPEC.md
├── docs/
│   ├── custom-gpt-setup.md
│   ├── setup-guide.md
│   └── supported-devices.md
└── .github/workflows/
    └── build.yml
```
