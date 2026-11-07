<div align="center">

# Doreman AI

[![Kotlin](https://img.shields.io/badge/KOTLIN-1.9.22-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![Jetpack Compose](https://img.shields.io/badge/JETPACK%20COMPOSE-MATERIAL%203-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Custom Gateway](https://img.shields.io/badge/CUSTOM%20GATEWAY-CLAUDE%20%7C%20GPT-10B981?style=for-the-badge)](https://github.com/)
[![Google Gemini](https://img.shields.io/badge/GOOGLE%20GEMINI-API-8E75B2?style=for-the-badge)](https://ai.google.dev/)

**A Siri-style voice-controlled personal assistant for Android.**

</div>

Speak a command — in Bengali or English — and your phone does the work: it opens apps, calls contacts, installs games, sends WhatsApp messages, plays YouTube, and controls your device. Your API keys stay on your device; there is no Doreman server.

---

## Download

**Recommended — signed release v2.1.1:**

👉 **[https://github.com/asmitsr/DoremanAI/releases/tag/latest-release-apk](https://github.com/asmitsr/DoremanAI/releases/tag/latest-release-apk)**

Inside that release, download **`app-release.apk`** (about 38.3 MB) and install it on your phone.

**Latest debug build (for testing the newest code):**

👉 **[https://github.com/asmitsr/DoremanAI/releases/tag/latest-apk](https://github.com/asmitsr/DoremanAI/releases/tag/latest-apk)**

Inside that release, download **`app-debug.apk`**. It is built automatically by GitHub Actions on every push to `main` and is always overwritten under the `latest-apk` tag, so it is always the newest code — but it is a debug build, not the signed release.

---

## What's new

### v2.1.1 (current release, versionCode 11)

- **Emergency fix:** the 3D mascot home screen from v2.1.0 crashed on some phones as soon as Home opened, so Home is back to the original Orb (tap = mic). The mascot files stay in the repo, unused, until the crash cause is found.
- Nothing else changed: automation, voice, gateway, AI, Settings, Code Studio, and the English interface are the same as v2.1.0.

### Latest on main (in the code, not in a release yet)

Two bugs reported by community contributor Matt Abate (mattabate) — GitHub Issues #1 and #2 — fixed in commit `dfa346f`:

- Play Store install no longer taps Install a second time after a successful tap.
- A local voice command (for example "volume up") now cancels an in-flight AI request first, so the older AI reply can no longer speak or run its actions afterwards.

### v2.1.0

- 3D mascot on Home: an original low-poly robot (SceneView + a small GLB model made for this app), with blink and idle animation. The mascot is an original design, not a copy of any existing character.

### v2.0.0

- **Full English interface:** every visible label, message, and notification is in English. Voice commands still work in Bengali and English.
- **New Home dashboard:** greeting + assistant-status card, Quick Actions (AI Chat, Automations, App Control, Code Studio), and Recent Activity from your last 3 real chat messages.
- Settings regrouped into 6 sections; Code Studio gained line numbers and separate Run / Stop / Save / Export buttons.

### v1.3.1

- Fixed "stuck after one task": listening now retries every 500 ms, up to 20 times (10 seconds), instead of checking only once 1,200 ms after an action.
- Added Stay Awake, so the foreground voice service returns after the app process is recreated.
- Clarified permissions: Android has no single "Screen Share" permission. Accessibility is the screen-reading and tapping permission used for automation, Overlay is the floating button, and live-video MediaProjection was deliberately not added.

### v1.2.0

- Midnight Indigo theme (`#282B4A` background + `#EEEBDA` surfaces/text), alongside Dark and Light.
- Bengali + English language switch for speech recognition, TTS voice, and AI replies.
- Gateway fixes: Bearer <redacted> now sends only `Authorization: Bearer`; a blank gateway key no longer silently sends your Gemini key (a clear error is shown instead); Test Connection shows only real models; a blank model no longer defaults to a stale model.
- Longer silence windows (2500 ms / 1200 ms), and words recognised before a timeout are now delivered instead of dropped.
- Siri-like fast commands: call, open app, Play Store install, YouTube search, volume, and media commands now run locally without waiting for the AI — the AI still handles everything else.

### v1.1.0

- Custom Gateway, silent wake-word, phonetic calling, Play Store automation, WhatsApp auto-send, booster/cleaner.

---

## Core Features

| Feature | Status | Description |
| :--- | :---: | :--- |
| **Custom Gateway** | Active | Support for third-party Base URLs and premium models (Claude, GPT, and other OpenAI-compatible models). The app holds one gateway at a time. |
| **Google Gemini Official API** | Active | Direct Gemini mode, with fallback models. |
| **Silent Wake-Word ("Doreman")** | Active | Wakes up instantly and silently upon hearing the wake word, without annoying beep sounds. |
| **Phonetic Contact Calling** | Active | Automatically searches and calls saved contacts using Bengali or English pronunciation. |
| **Play Store Automation** | Active | Opens the Play Store via voice command and automatically taps the `Install` button. |
| **WhatsApp Auto-Send** | Active | Finds contacts or numbers, types messages, and automatically triggers the `Send` button. |
| **Mobile Booster & Cleaner** | Active | Clears device cache and launches system cleaner tools instantly. |
| **Bengali + English voice** | Active | Voice commands, TTS, and AI replies in Bengali or English. The app interface is in English. |
| **Midnight Indigo Theme** | Active | `#282B4A` + `#EEEBDA` theme, alongside Dark and Light. |
| **Stay Awake** | Active | Keeps the voice service listening across tasks and after the app process is recreated, when enabled. |

---

## Quick Setup

1. **Download APK:** Use the download link above and install it on your device.
2. **Permissions:** Open the app, go to **Settings > Permission Setup Hub**, and grant all required permissions including **Accessibility** (needed for auto-tap / auto-install / WhatsApp auto-send).
3. **Configure AI Brain:** Go to **Settings > AI Brain & Custom Gateway**, choose Gemini or Custom Gateway, enter your key, and press **Test Connection**.
4. **Language & Look:** Go to **Settings > Voice > Language** for Bengali / English, and **Settings > Theme** for Dark / Light / Midnight Indigo.
5. **Talk:** Say **"Doreman"** (or tap the mic) and give a command — see the command list below.

---

## Third-party key setup (Custom Gateway) — read this first

A third-party key works **only if all four match your gateway**:

1. **Base URL** — for example `https://integrate.api.nvidia.com` or your CodeCraft base URL. Do not add `/v1/chat/completions` or `/v1/messages` yourself; the app appends the endpoint.
2. **Auth Scheme** — `Authorization: Bearer` for most OpenAI-compatible gateways, `x-api-key (Claude)` only for Claude-style gateways.
3. **Format** — `Auto` tries OpenAI first, then Anthropic; or pick `OpenAI` (`/v1/chat/completions`) / `Anthropic` (`/v1/messages`) explicitly.
4. **Model name** — must be a model your key actually has access to. Press **Test Connection** and pick from the list it returns.

### Why keys failed before v1.2.0

- **Bearer <redacted> extra headers:** Bearer <redacted> used to send Anthropic headers on every call, which strict OpenAI-compatible gateways rejected. Now Bearer <redacted> sends only `Authorization: Bearer <key>`.
- **Silent Gemini-key fallback:** If the gateway key was blank, the app quietly sent your Gemini key to the third-party gateway, producing a confusing 401. Now a blank gateway key shows a clear "key not set" error.
- **Fake Test Connection:** The test used to show a hard-coded model list even when the gateway was unreachable. Now a failed test shows the real error, and a successful test shows only the models your key can actually use.
- **Stale default model:** A blank model used to default to a model many gateways do not offer. Now a blank model is a clear error telling you to pick a model from Test Connection.

---

## Commands

Say **"Doreman"** first, or tap the mic.

| What you want | Bengali example | English example |
| :--- | :--- | :--- |
| Call someone | "বাবাকে কল দাও" | "Call Baba" |
| Open an app | "YouTube খোলো" | "Open YouTube" |
| Download a game/app | "Free Fire ডাউনলোড করো" | "Download Free Fire" |
| Play on YouTube | "ইউটিউবে অরিজিৎ সিং চালাও" | "YouTube search Arijit Singh" |
| WhatsApp message | "হোয়াটসঅ্যাপে এম এ কে মেসেজ দাও কেমন আছো" | "WhatsApp MA: how are you" |
| Volume | "ভলিউম বাড়াও / কমাও" | "Volume up / down" |
| Music control | "পরের গান / গান থামাও" | "Next song / pause music" |
| Clean phone | "মোবাইল ক্লিন করো" | "Clean my phone" |
| Lock screen | "স্ক্রিন লক করো" | "Lock screen" |

> Fast-path note: Call, Open App, Play Store Install, YouTube Search, Volume, and Media are handled locally for speed; everything else goes through the AI plan, exactly as before. SMS/WhatsApp stay on the AI path because their target + message parsing is ambiguous in free speech.

---

## Speech tips

- **Long sentences:** The app gives you more silence time before a session ends (complete-silence 2500 ms), and words recognised before a timeout are delivered instead of dropped. If you pause mid-sentence, the recognised part is still used.
- **If speech stops early:** Check that the correct language is selected (Settings → Voice → Language), that microphone permission is granted, and that a Bengali TTS voice is installed for Bengali replies (Settings → Voice → TTS engine settings).
- **Wake word:** The app listens in the foreground for "Doreman" / "ডোরেমন" and common variants. There is no on-device hotword chip — it is repeated recognizer sessions while the app is in the foreground, so it stops when you leave the app.

---

## Build from source

```bash
# Android Studio (recommended) — or:
./gradlew assembleDebug
# Output: app/build/outputs/apk/debug/app-debug.apk
```

Requirements: Java 17, Gradle 8.5 (as in `.github/workflows/build.yml`), `compileSdk 34`, `minSdk 26`.
Every push to `main` also builds the debug APK automatically via GitHub Actions and publishes it to the `latest-apk` release (see Download above).

---

## Permissions

| Permission | Why it is needed |
| :--- | :--- |
| Microphone (`RECORD_AUDIO`) | Voice commands and wake-word listening |
| Contacts (`READ_CONTACTS`) | Phonetic contact search for calls / messages |
| Phone (`CALL_PHONE`) | Placing calls (falls back to the dialer if not granted) |
| SMS (`SEND_SMS`) | SMS commands |
| Notifications | Automation status |
| Accessibility Service | Auto-tap Install, WhatsApp auto-send, screen automation — only when you enable it |
| `QUERY_ALL_PACKAGES` | Finding installed apps by name |

---

## Privacy

- **Your API keys never leave your device except to the provider you chose** (Gemini or your own gateway). They are stored in the Android Keystore (AES-GCM) — there is no Doreman server, and keys are never put in share links or logs.
- Voice is processed by Android's on-device / Google speech service and by your chosen AI provider, as with any voice assistant.
- Saved preferences ("memory") are stored on-device and only used if you keep Memory enabled.

---

## Troubleshooting

| Symptom | Likely cause / fix |
| :--- | :--- |
| **401 / Invalid key** | Wrong key, or a Gemini key pasted into Gateway mode (or vice versa). Re-enter the key for the mode you selected and Test Connection again. |
| **404 / Model not found** | Model name does not exist on your gateway, or Base URL / Format is wrong. Test Connection → pick a model from the returned list. |
| **429 Rate limit** | Too many requests in a short time. Wait, then retry; or switch to a lighter model. |
| **503 / 529 High demand** | The model/gateway is overloaded. Wait and retry, or pick another model. |
| **Test Connection shows no models** | The gateway may not support `/v1/models`, or the key is wrong for that endpoint. The model list is not required — you can type a model ID manually if you know it works. |
| **Speech stops on long sentences** | Some devices ignore the silence hints. Speak in shorter phrases, and check the language setting matches the language you are speaking. |
| **TTS voice missing** | Install the Bengali/English TTS voice from Settings → Voice → TTS engine settings. |
| **Call goes to dialer, not direct call** | `CALL_PHONE` permission is not granted — grant it, or the app intentionally falls back to the dialer. |
| **Auto-install / auto-send does not tap** | Accessibility Service is off. Enable it in Settings → Screen Automation. |

---

## Tech Stack

- Kotlin 1.9.22, Jetpack Compose Material 3
- Android SpeechRecognizer + TextToSpeech
- OkHttp gateway client (OpenAI + Anthropic formats)
- Google Gemini API + Custom Gateways (Claude / GPT / others)
- DataStore + Android Keystore for settings and keys

## License

This project has no open-source license. All rights reserved — the APK is sold as a product, and the code is not licensed for reuse or redistribution.
