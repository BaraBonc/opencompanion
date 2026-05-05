# Companion App — Technical Spec

**Package**: `com.opencompanion.companion`
**Min SDK**: 26 (Android 8.0) | **Target SDK**: 34
**Language**: Kotlin

## Build status
- [x] Project scaffolded
- [ ] WakeWordService — TFLite inference loop
- [ ] BluetoothScoManager — HFP connect/disconnect
- [ ] ACTION_ASSIST handoff
- [ ] BootReceiver — restart service on reboot
- [ ] ProactiveCheckInScheduler — AlarmManager + TTS
- [ ] EmergencyPhraseDetector — secondary TFLite model
- [ ] FirebaseLogger — activity log writes
- [ ] SettingsActivity — check-in time, wake word, companion name
- [ ] DailySummaryWorker — evening summary push to carer

---

## Components

### WakeWordService (foreground service)
`service/WakeWordService.kt`

Runs permanently in the foreground (persistent notification required API 26+).
Started by `BootReceiver` on device boot and by `MainActivity` on first launch.

**Audio capture loop**:
```
AudioRecord (MIC or SCO source, 16kHz, mono, 16-bit PCM)
  → 20ms chunks (320 samples)
  → TFLite Interpreter (openWakeWord model)
  → score > threshold → onWakeWordDetected()
```

**Model loading**: reads selected .tflite from `assets/wakewords/`. Default:
`hello_zoli.tflite`. Falls back to `hello_nana.tflite` if not found.

**onWakeWordDetected()**:
1. Stop AudioRecord briefly to release mic for assistant app
2. Fire `Intent(Intent.ACTION_ASSIST)` with `FLAG_ACTIVITY_NEW_TASK`
3. Log wake detection to Firebase
4. Resume AudioRecord after 500ms delay

**Sensitivity**: `WakeWordPreferences.sensitivity` (0.0–1.0). Maps to TFLite score
threshold: `threshold = 1.0 - sensitivity` (default sensitivity 0.5 → threshold 0.5).

---

### BluetoothScoManager
`audio/BluetoothScoManager.kt`

Manages the Bluetooth HFP/SCO channel for external speaker support.

**BroadcastReceiver** registered for:
- `BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED`
- `AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED`

**On BT device connected**:
1. Check device supports HFP (`BluetoothProfile.HEADSET`)
2. Call `audioManager.startBluetoothSco()`
3. Wait for `SCO_AUDIO_STATE_CONNECTED` broadcast
4. Update `WakeWordService` audio source to `AudioSource.VOICE_COMMUNICATION`

**On BT device disconnected**:
1. Call `audioManager.stopBluetoothSco()`
2. Update `WakeWordService` audio source back to `AudioSource.MIC`

**Switching must be seamless** — `WakeWordService` pauses capture, switches source,
resumes. Target: < 200ms gap.

---

### ProactiveCheckInScheduler
`scheduler/ProactiveCheckInScheduler.kt`

Uses `AlarmManager.setExactAndAllowWhileIdle()` (required for background alarms API 23+).

**Daily at configured time (default 10:00)**:
1. Acquire `WakeLock` (release after 30s max)
2. `TextToSpeech.speak()` the configured greeting through active audio output
3. After TTS completes (`onUtteranceProgressListener`), fire `ACTION_ASSIST`
4. Log check-in to Firebase
5. Schedule next alarm for tomorrow same time

**Greeting** (stored in `CompanionPreferences`):
Default: `"Szia! Minden rendben van?"` (Hungarian)
Configurable per family in settings.

**TTS engine**: Android built-in. Language set to match `CompanionPreferences.locale`.

---

### EmergencyPhraseDetector
`wakeword/EmergencyPhraseDetector.kt`

Runs in same `WakeWordService` thread as wake word detector, using a second TFLite
interpreter instance with `call_zoli.tflite` (emergency trigger model).

**On detection**:
1. Play confirmation tone (AudioTrack, short beep)
2. Write emergency event to Firebase: `{timestamp, phrase, deviceId}`
3. Firebase → FCM to carer app (handled server-side via Firebase Functions or
   directly via Firebase Realtime Database triggers)

**Default trigger phrase**: "Hívd Zolit" (Hungarian) — fully configurable.

---

### FirebaseLogger
`firebase/FirebaseLogger.kt`

Writes to Firebase Realtime Database path: `activity/{deviceId}/{date}/{eventId}`

**Event schema**:
```json
{
  "type": "wake_word" | "check_in" | "emergency" | "carer_initiated",
  "timestamp": 1234567890000,
  "details": {}
}
```

Uses Firebase Anonymous Auth. Device ID is the anonymous UID, generated once and
stored in `FirebasePreferences`. Pairing writes the carer FCM token to
`pairing/{deviceId}/carerFcmToken`.

---

### SettingsActivity
`ui/SettingsActivity.kt`

Uses `PreferenceFragmentCompat` with `res/xml/preferences.xml`.

**Settings**:
- Companion name (string, default "Zoli")
- Wake word model (dropdown: hello_zoli / hello_nana / hey_helper / custom)
- Sensitivity (slider 0–100, default 50)
- Check-in enabled (switch)
- Check-in time (TimePicker, default 10:00)
- Check-in greeting (string, default Hungarian greeting)
- Locale/language (for TTS engine)
- Emergency phrase (string, default "Hívd Zolit")
- Emergency phrase enabled (switch)

---

### DailySummaryWorker
`service/DailySummaryWorker.kt`

`WorkManager` `PeriodicWorkRequest`, fires at 20:00 daily.

Reads today's Firebase activity log, formats summary:
```
Conversations today: N
Last active: HH:MM
Check-in: completed/missed (HH:MM)
Red flags: N
Emergency alerts: N
```

Posts summary via FCM to carer FCM token stored in Firebase under `pairing/{deviceId}`.

---

## Permissions (AndroidManifest.xml)
```xml
RECORD_AUDIO               <!-- wake word detection -->
BLUETOOTH / BLUETOOTH_CONNECT / BLUETOOTH_SCAN  <!-- SCO audio -->
RECEIVE_BOOT_COMPLETED     <!-- start service on boot -->
FOREGROUND_SERVICE         <!-- persistent wake word service -->
FOREGROUND_SERVICE_MICROPHONE <!-- API 34 foreground service type -->
INTERNET                   <!-- Firebase -->
WAKE_LOCK                  <!-- check-in alarm -->
SCHEDULE_EXACT_ALARM       <!-- AlarmManager exact alarms -->
```

---

## Bundled wake word models
`assets/wakewords/`
- `hello_zoli.tflite` — Hungarian male name (primary default)
- `hello_nana.tflite` — English/universal
- `hey_helper.tflite` — English neutral
- `call_zoli.tflite` — emergency phrase "Hívd Zolit"

**Note**: Placeholder .tflite files committed for scaffolding. Real openWakeWord-trained
models to be trained and replaced before first deployment.

---

## Dependencies (app/build.gradle.kts)
```kotlin
implementation("org.tensorflow:tensorflow-lite:2.14.0")
implementation("org.tensorflow:tensorflow-lite-support:0.4.4")
implementation("com.google.firebase:firebase-database-ktx:20.3.0")
implementation("com.google.firebase:firebase-messaging-ktx:23.4.0")
implementation("com.google.firebase:firebase-auth-ktx:22.3.0")
implementation("androidx.work:work-runtime-ktx:2.9.0")
implementation("androidx.preference:preference-ktx:1.2.1")
```
