# Supported Devices

## Tested configurations

| Elder's phone | Speaker | Status | Notes |
|---|---|---|---|
| Google Pixel 6 Pro | JBL Link 20 | ✅ Primary target | SCO works, excellent far-field pickup |
| Google Pixel 6 Pro | Phone speaker only | ✅ Tested | Works, shorter wake distance |

## Speaker requirements

The speaker must support **Bluetooth HFP (Hands-Free Profile)**.
This is the same profile used for phone calls — if you can take a phone call through the
speaker, it will work with OpenCompanion.

**HFP speakers (recommended)**:
- JBL Link series (Link 10, 20, 300, 500)
- JBL Flip 5 and newer
- Bose Home Portable
- Most "smart speakers" with phone-call support

**Will NOT work** (audio only, no microphone):
- Basic Bluetooth speakers without microphone
- Amazon Echo (uses its own wake word system, cannot act as BT microphone)
- Google Nest speakers (same issue)

## Minimum Android version

Android 8.0 (API 26) — required for ChatGPT voice mode.

## Add a device

If you test a combination that isn't listed, please open a PR adding a row to this table.
