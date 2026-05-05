# OpenCompanion

An open source Android companion for elderly people living alone, with a safety
monitoring layer for their remote carers.

**Say "Hello Zoli" → ChatGPT opens in voice mode. No typing. No screens. Just talk.**

---

## What it is

Two Android apps:

- **Companion app** — runs permanently on a phone near the elder's speaker. Listens
  always-on for a custom wake word. Fires the standard Android assistant intent on
  detection, handing off to ChatGPT (or Gemini, or Claude) in full voice mode.
  Sends a daily activity summary and instant alerts to the carer.

- **Carer app** — runs on the family member's phone. Receives real-time alerts
  (emergency phrases, red flags), shows a daily activity feed, and lets the carer
  trigger a check-in conversation remotely.

## Why

Viktor's elderly mother lives alone in Hungary. Viktor lives in the UK. She speaks
Hungarian. She is not technically literate. This is not a unique situation.

OpenCompanion is what Alexa Together would be if it were **free, open source,
multilingual, and built by people who actually have elderly relatives.**

## Status

Phase 1 in progress — companion app MVP.

- [x] Project scaffolded
- [ ] Wake word detection (TFLite + openWakeWord)
- [ ] Bluetooth SCO audio routing
- [ ] ACTION_ASSIST handoff
- [ ] Daily check-in scheduler
- [ ] Firebase activity logging
- [ ] Carer app (Phase 2)

**Target**: September 2026 — pre-configured phone + speaker delivered to Viktor's mother.

## Technical highlights

- Wake word runs **on-device** via TensorFlow Lite — no API key, no cloud, no cost
- AI conversation via **ACTION_ASSIST intent** — works with any app the user has set
  as their Android assistant (ChatGPT, Gemini, Claude)
- **Bluetooth HFP/SCO** support for external speakers with far-field microphones
- **Firebase Spark (free tier)** for all notifications and data sync
- **No user accounts** — anonymous auth + 6-digit pairing code only
- MIT licensed

## Setup

See [docs/setup-guide.md](docs/setup-guide.md) for the full end-user setup guide.

See [docs/custom-gpt-setup.md](docs/custom-gpt-setup.md) for configuring a Custom GPT
companion named "Zoli" (or any name).

## Building

```bash
cd companion-app && ./gradlew assembleDebug
cd carer-app && ./gradlew assembleDebug
```

Requires: JDK 17, Android SDK. Firebase `google-services.json` not included —
create your own Firebase project (free Spark tier) and drop the file in `app/`.

## Contributing

Issues and PRs welcome. Key areas:

- **Wake word models**: Train and contribute models for new names and languages
- **Device testing**: Add rows to [docs/supported-devices.md](docs/supported-devices.md)
- **Translations**: Settings UI, setup guide, notification strings

## License

MIT
