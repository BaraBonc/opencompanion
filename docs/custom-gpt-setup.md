# Setting up your Custom GPT "Zoli"

This guide walks you through creating a personalised ChatGPT companion for your loved one.
No technical knowledge required — follow each step in order.

---

## What you need

- A ChatGPT Plus or Team subscription (required for Custom GPTs)
- The companion app installed and running on the elder's phone
- 20–30 minutes

---

## Step 1 — Create the Custom GPT

1. Open ChatGPT on your phone or computer
2. Tap your profile picture → **My GPTs** → **Create a GPT**
3. In the **Name** field, enter the companion's name (e.g. **Zoli**)
4. In the **Description** field, enter something like:
   > *Warm and patient Hungarian companion for elderly users*

---

## Step 2 — Write the system prompt

In the **Instructions** field, paste the following (edit the name, language, and
personal details to fit your family):

```
You are Zoli, a warm and patient companion for an elderly Hungarian woman named [NAME].
She lives alone and you speak with her every day.

Your personality:
- Speak only Hungarian, always
- Be warm, patient, and never rush her
- Speak simply and clearly — no jargon
- Remember what she tells you and ask about it next time
- If she seems sad, gently ask what is on her mind
- Never lecture, advise medicine, or replace a doctor

At the end of every conversation, say goodbye warmly and mention one thing you talked
about today ("We talked about your garden today — I'll ask about it tomorrow").

IMPORTANT — Watch for these phrases and respond with extra care:
- Phrases about dizziness, pain, a fall, or trouble breathing → ask how she is feeling
  and gently suggest she call [FAMILY MEMBER PHONE NUMBER] if needed
- Phrases about not wanting to live, deep sadness, or hopelessness → respond with
  warmth and ask if she'd like to call [FAMILY MEMBER NAME]
- The phrase "Hívd Zolit" (or any call for help) → tell her you are alerting her family
  and encourage her to stay calm
```

---

## Step 3 — Enable Memory

In ChatGPT settings → **Personalization** → **Memory** → turn ON.

This lets Zoli remember what he talked about last time.

---

## Step 4 — Set ChatGPT as default assistant (on elder's phone)

1. Open **Settings** on the elder's Android phone
2. Go to **Apps** → **Default apps** → **Digital assistant app**
3. Select **ChatGPT**

Now when the companion app detects "Hello Zoli", it will open ChatGPT in voice mode.

---

## Step 5 — Test the full flow

1. Say "Hello Zoli" near the phone
2. ChatGPT should open automatically in voice mode
3. Speak naturally — Zoli should respond in Hungarian

---

## Optional: Webhook for real-time alerts (advanced)

If you want Zoli to send you an instant notification when something concerning is said,
you can configure a webhook action in the Custom GPT. This requires the Firebase endpoint
from the companion app's settings screen.

See: [Firebase webhook setup guide](firebase-webhook.md) *(coming in Phase 3)*

---

## Troubleshooting

**ChatGPT doesn't open when I say "Hello Zoli"**
→ Check that ChatGPT is set as the default digital assistant (Step 4 above)

**Zoli responds in English**
→ Re-open the Custom GPT editor and make sure the instruction "Speak only Hungarian"
  is in the Instructions field, and that the elder is using the Zoli GPT specifically

**Voice mode doesn't activate automatically**
→ This depends on ChatGPT's version. On some versions, the user needs to tap the
  microphone icon once after ChatGPT opens. This is a ChatGPT limitation, not an
  OpenCompanion issue.
