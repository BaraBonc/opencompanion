# OpenCompanion Setup Guide

This guide is written for family members setting up OpenCompanion for a loved one.
No technical knowledge required.

---

## What you need

| Item | Notes |
|---|---|
| Android phone (for elder) | Android 8 or newer. Any brand. |
| Bluetooth speaker (optional but recommended) | Must support phone calls. See supported-devices.md |
| Android phone (yours — the carer) | Any Android phone |
| ChatGPT Plus subscription | Required for voice conversation |
| Wi-Fi at elder's home | Required |

---

## Part 1 — Set up the companion phone

### 1.1 Install the companion app

Download **OpenCompanion** from the GitHub releases page and install it:
- Transfer the APK to the phone and tap it to install
- If asked, allow installation from unknown sources (Settings → Security)

### 1.2 Open the app and grant permissions

When you open OpenCompanion for the first time:
- Tap **Allow** when asked for microphone access
- Tap **Allow** for Bluetooth access

### 1.3 Connect your Bluetooth speaker

1. Put the speaker in pairing mode (usually hold the Bluetooth button for 3 seconds)
2. On the elder's phone: Settings → Bluetooth → pair the speaker
3. OpenCompanion will automatically use the speaker's microphone

### 1.4 Set ChatGPT as the default assistant

1. Settings → Apps → Default apps → Digital assistant app
2. Select **ChatGPT**

### 1.5 Configure the companion

Open OpenCompanion → tap **Settings**:
- **Companion name**: Type the name your family uses (e.g. "Zoli", "Nana")
- **Daily check-in**: Turn on. Set a time that suits your loved one (default: 10:00 AM)
- **Check-in greeting**: Edit this to sound like your family (e.g. "Szia! Minden rendben van?")

### 1.6 Set up your Custom GPT

Follow the guide in `custom-gpt-setup.md`. This takes about 20 minutes and makes the
AI companion sound like a real person rather than a generic assistant.

---

## Part 2 — Set up your carer phone

### 2.1 Install the carer app

Download **OpenCompanion Carer** from the GitHub releases page and install it.

### 2.2 Pair the two phones

1. On your carer phone: open **OpenCompanion Carer** → tap **Pair with companion**
2. A 6-digit code appears on your screen
3. On the elder's phone: open OpenCompanion → Settings → **Enter pairing code** → type the 6 digits
4. Both apps confirm pairing

You only need to do this once.

---

## Part 3 — Test the full system

1. Say **"Hello Zoli"** (or whatever name you configured) near the elder's phone
2. ChatGPT should open and start listening — have a short conversation
3. Check your carer app — you should see the conversation appear in the activity feed
4. At the configured check-in time, the phone should speak the greeting automatically

---

## What happens each day

| Time | What happens |
|---|---|
| 10:00 AM | Phone says greeting. ChatGPT opens. Your loved one can chat. |
| Any time | They say "Hello Zoli" → same thing |
| Any time | They say "Hívd Zolit" → you get an instant alert |
| 8:00 PM | You get a daily summary: conversations, last active, any concerns |

---

## What you see in the carer app

- **Last active**: when the elder last talked to Zoli
- **Conversations today**: how many times they activated the companion
- **Check-in**: did they complete the morning check-in?
- **Alerts**: any emergency phrases detected

Red header = elder hasn't been active for more than 24 hours.

---

## If something stops working

**No response to wake word**: Check the companion app is showing "Listening…" on screen.
If not, tap the app to reopen it — it will restart the listener automatically.

**Speaker not working**: Make sure Bluetooth is connected in phone settings.
OpenCompanion switches to phone speaker automatically if Bluetooth disconnects.

**Carer app not receiving updates**: Check both phones have internet. Re-pair if needed.

---

*For technical issues, open a GitHub issue at github.com/BaraBonc/opencompanion*
