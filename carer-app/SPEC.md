# Carer App — Technical Spec

**Package**: `com.opencompanion.carer`
**Min SDK**: 26 (Android 8.0) | **Target SDK**: 34
**Language**: Kotlin

## Build status
- [x] Project scaffolded
- [ ] PairingActivity — generate 6-digit code, write to Firebase
- [ ] MainActivity — activity feed, daily summary display
- [ ] AlertReceiver — FCM handler, high-priority notifications
- [ ] RemoteCheckInActivity — one-tap "start conversation"
- [ ] FirebaseReader — read activity log for elder's device

---

## Components

### PairingActivity
`ui/PairingActivity.kt`

**Flow**:
1. Carer opens app, taps "Pair with companion"
2. App generates 6-digit numeric code
3. Writes to Firebase: `pairing/codes/{code} = {carerFcmToken, expires: now+10min}`
4. Shows code on screen with countdown timer
5. User enters same code on companion app
6. Companion app reads carer FCM token, stores under `pairing/{deviceId}/carerFcmToken`
7. Companion app writes `pairing/{deviceId}/companionFcmToken` for reverse comms
8. Carer app polls Firebase for `pairing/codes/{code}/companionFcmToken`
9. On receipt: store companionFcmToken locally, navigate to MainActivity
10. Code document auto-deleted after use or expiry

**Security**: 6 digits = 1M combinations, 10-minute TTL → brute force infeasible.
No user-identifiable data stored anywhere.

---

### MainActivity
`ui/MainActivity.kt`

Two-tab layout:
- **Feed tab**: RecyclerView of today's activity events (wake words, check-ins, alerts)
  streamed in real-time from Firebase Realtime Database
- **Summary tab**: Latest daily summary card + 7-day history

**Header**: Shows elder's last-active time. Red if > 24h ago.

**Remote check-in button**: Floating action button. Sends FCM message to companion
app's FCM token. Companion app plays TTS greeting and fires ACTION_ASSIST.

---

### AlertReceiver (FCM handler)
`firebase/AlertReceiver.kt` extends `FirebaseMessagingService`

Handles incoming FCM messages. Message types (via `data.type`):

| Type | Priority | Action |
|---|---|---|
| `emergency` | HIGH | Full-screen alert, sound alarm, vibrate |
| `red_flag` | HIGH | Heads-up notification |
| `daily_summary` | NORMAL | Silent notification, update MainActivity |
| `check_in_missed` | NORMAL | Notification |

**Emergency alert**: Uses `NotificationManager.IMPORTANCE_HIGH` + full-screen intent.
No dismissal until carer explicitly acknowledges.

---

### RemoteCheckIn
`ui/MainActivity.kt` (FAB handler)

Sends FCM message to stored companion FCM token:
```json
{
  "to": "{companionFcmToken}",
  "data": {
    "type": "carer_initiated",
    "greeting": "Zoli szeretne beszélni veled"
  }
}
```

Uses Firebase Realtime Database to queue the message (companion app listens on its
own node). Avoids needing server-side FCM send (which requires service account key).

---

## Permissions (AndroidManifest.xml)
```xml
INTERNET                   <!-- Firebase -->
POST_NOTIFICATIONS         <!-- FCM alerts (API 33+) -->
RECEIVE_BOOT_COMPLETED     <!-- restart FCM listener on boot -->
VIBRATE                    <!-- emergency alerts -->
USE_FULL_SCREEN_INTENT     <!-- emergency full-screen overlay -->
```

---

## Dependencies (app/build.gradle.kts)
```kotlin
implementation("com.google.firebase:firebase-database-ktx:20.3.0")
implementation("com.google.firebase:firebase-messaging-ktx:23.4.0")
implementation("com.google.firebase:firebase-auth-ktx:22.3.0")
implementation("androidx.recyclerview:recyclerview:1.3.2")
implementation("com.google.android.material:material:1.11.0")
```
