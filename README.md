<div align="center">

# 🤖 Doreman AI (ডোরেমন এআই)

[![Kotlin](https://img.shields.io/badge/KOTLIN-1.9.22-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![Jetpack Compose](https://img.shields.io/badge/JETPACK%20COMPOSE-MATERIAL%203-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Custom Gateway](https://img.shields.io/badge/CUSTOM%20GATEWAY-CLAUDE%20%7C%20GPT-10B981?style=for-the-badge)](https://github.com/)
[![Google Gemini](https://img.shields.io/badge/GOOGLE%20GEMINI-API-8E75B2?style=for-the-badge)](https://ai.google.dev/)

**An advanced Siri-style voice-controlled personal assistant for Android, powered by cutting-edge AI.**

**আপনার অ্যান্ড্রয়েড ফোনের জন্য সিরি-স্টাইলের ভয়েস অ্যাসিস্ট্যান্ট — বাংলায় বা ইংরেজিতে কথা বলুন, আর ফোন নিজেই কাজ করুক।**

</div>

---

## 📥 Download / ডাউনলোড

**Latest APK (direct link, always the newest build):**

👉 **[https://github.com/asmitsr/DoremanAI/releases/tag/latest-apk](https://github.com/asmitsr/DoremanAI/releases/tag/latest-apk)**

> Inside that release, download **`app-debug.apk`** and install it on your phone.
> এই রিলিজের ভেতর থেকে **`app-debug.apk`** ডাউনলোড করে ফোনে ইনস্টল করুন।
>
> ⚠️ This is a **debug APK** built automatically by GitHub Actions on every push to `main` — it is always overwritten under the `latest-apk` tag, so the link above always points to the newest version. It is **not** a signed Play Store release.
> এটি একটি **ডিবাগ APK**, `main`-এ প্রতিটি পুশের পর GitHub Actions স্বয়ংক্রিয়ভাবে বানায়। তাই লিংকটি সবসময় নতুন ভার্সনেই যাবে, কিন্তু এটি Play Store-এর সাইন করা রিলিজ নয়।

---

## 📌 Introduction / পরিচয়

**Doreman AI** হলো আপনার অ্যান্ড্রয়েড ডিভাইসের জন্য তৈরি একটি স্মার্ট ও দ্রুতগতির ভয়েস-কন্ট্রোলড পার্সোনাল অ্যাসিস্ট্যান্ট। এটি আপনার প্রাত্যহিক কাজগুলোকে ভয়েস কমান্ডের মাধ্যমে সম্পূর্ণ স্বয়ংক্রিয় করে তোলে।

Doreman AI is a smart, fast, Siri-style voice assistant for Android. Speak a command — in Bengali or English — and it opens apps, calls contacts, installs games, sends WhatsApp messages, plays YouTube and controls your phone.

---

## ✨ What's new in v1.2.0 / v1.2.0-এ যা নতুন

| Change | Details |
| :--- | :--- |
| 🎨 **Midnight Indigo theme** | New theme: Midnight Indigo `#282B4A` background + Vanilla `#EEEBDA` surfaces/text. Settings → Theme → “মিডনাইট ইন্ডিগো”. |
| 🤖 **New home animation** | The old 2D purple orb is replaced by an **original** animated robot companion (blinking eyes, speaking mouth, orbiting rings, voice-reactive pulse). It is an original design — not a copy of any existing cartoon character. |
| 🌐 **Bengali + English** | Real language switch: Settings → Voice → Language. It changes speech recognition (`bn-BD` / `en-US`), TTS voice and the AI's reply language. |
| 🔑 **Third-party key fixes** | Bearer mode now sends only `Authorization: Bearer`; a blank gateway key no longer silently sends your Gemini key (clear error instead); Test Connection no longer shows fake model lists; a blank model no longer defaults to a stale model. |
| 🎙️ **Long speech fixed** | Longer silence windows (2500ms / 1200ms) and words recognised before a timeout are now delivered instead of dropped. |
| ⚡ **Siri-like fast commands** | Call, open app, Play Store install, YouTube search, volume and media commands now run locally without waiting for the AI — the AI still handles everything else. |

---

## ⚙️ Core Features

| Feature | Status | Description |
| :--- | :---: | :--- |
| **Custom Gateway (`sk-...` Token)** | ✅ Active | Support for third-party Base URLs and premium models (Claude Opus/Sonnet, GPT-4o). |
| **Google Gemini Official API** | ✅ Active | Direct Gemini mode, with fallback models. |
| **Silent Wake-Word (`"Doreman"`)** | ✅ Active | Wakes up instantly and silently upon hearing the wake word without annoying beep sounds. |
| **Phonetic Contact Calling** | ✅ Active | Automatically searches and calls saved contacts using Bengali or English pronunciation. |
| **Play Store Automation** | ✅ Active | Opens the Play Store via voice command and automatically taps the `Install` button. |
| **WhatsApp Auto-Send** | ✅ Active | Finds contacts or numbers, types messages, and automatically triggers the `Send` button. |
| **Mobile Booster & Cleaner** | ✅ Active | Clears device cache and launches system cleaner tools instantly. |
| **Bengali + English** | ✅ Active | Full language switch for voice, TTS and AI replies. |
| **Midnight Indigo Theme** | ✅ Active | `#282B4A` + `#EEEBDA` theme, alongside Dark and Light. |

---

## 🚀 Quick Setup / দ্রুত সেটআপ

1. **Download APK:** Use the direct link above, download **`app-debug.apk`**, and install it on your device.
2. **Permissions:** Open the app, go to **Settings (⚙️) > Permission Setup Hub**, and grant all required permissions including **Accessibility** (needed for auto-tap / auto-install / WhatsApp auto-send).
3. **Configure AI Brain:** Go to **Settings (⚙️) > AI Brain & Custom Gateway**, choose Gemini or Custom Gateway, enter your key, and press **Test Connection**.
4. **Language & Look:** Go to **Settings > Voice > Language** for বাংলা / English, and **Settings > Theme** for Dark / Light / Midnight Indigo.
5. **Talk:** Say **“Doreman”** (or tap the mic) and give a command — see the command list below.

---

## 🔑 Third-party key setup (Custom Gateway) — read this first

A third-party key works **only if all four match your gateway**:

1. **Base URL** — e.g. `https://tabitoken.com`. Do not add `/v1/chat/completions` or `/v1/messages` yourself; the app appends the endpoint.
2. **Auth Scheme** — `Authorization: Bearer` for most OpenAI-compatible gateways, `x-api-key (Claude)` only for Claude-style gateways.
3. **Format** — `Auto` tries OpenAI first, then Anthropic; or pick `OpenAI` (`/v1/chat/completions`) / `Anthropic` (`/v1/messages`) explicitly.
4. **Model name** — must be a model your key actually has access to. Press **Test Connection** and pick from the list it returns.

### What v1.2.0 fixed, and why keys failed before

- **Bearer sent extra headers:** Bearer mode used to send Anthropic headers on every call, which strict OpenAI-compatible gateways rejected. Now Bearer sends **only** `Authorization: Bearer <key>`.
- **Silent Gemini-key fallback:** If the gateway key was blank, the app quietly sent your Gemini key to the third-party gateway, producing a confusing 401. Now a blank gateway key shows a clear “key not set” error.
- **Fake Test Connection:** The test used to show a hard-coded model list even when the gateway was unreachable. Now a failed test shows the real error, and a successful test shows only the models your key can actually use.
- **Stale default model:** A blank model used to default to `claude-3-5-sonnet-20241022`, which many gateways do not offer. Now a blank model is a clear error telling you to pick a model from Test Connection.

---

## 🗣️ Commands / কমান্ডের তালিকা

Say **“Doreman”** first, or tap the mic. Examples in both languages:

| What you want | Bengali | English |
| :--- | :--- | :--- |
| Call someone | “বাবাকে কল দাও” | “Call Baba” |
| Open an app | “YouTube খোলো” | “Open YouTube” |
| Download a game/app | “Free Fire ডাউনলোড করো” | “Download Free Fire” |
| Play on YouTube | “ইউটিউবে অরিজিৎ সিং চালাও” | “YouTube search Arijit Singh” |
| WhatsApp message | “হোয়াটসঅ্যাপে এম এ কে মেসেজ দাও কেমন আছো” | “WhatsApp MA: how are you” |
| Volume | “ভলিউম বাড়াও / কমাও” | “Volume up / down” |
| Music control | “পরের গান / গান থামাও” | “Next song / pause music” |
| Clean phone | “মোবাইল ক্লিন করো” | “Clean my phone” |
| Lock screen | “স্ক্রিন লক করো” | “Lock screen” |

> Fast-path note: Call, Open App, Play Store Install, YouTube Search, Volume and Media are handled locally for speed; everything else goes through the AI plan, exactly as before. SMS/WhatsApp stay on the AI path because their target + message parsing is ambiguous in free speech.

---

## 🎙️ Speech tips / কথা বলার টিপস

- **Long sentences:** v1.2.0 gives you more silence time before a session ends (complete-silence 2500ms), and words recognised before a timeout are delivered instead of dropped. If you pause mid-sentence, the recognised part is still used.
- **If speech stops early:** Check that the correct language is selected (Settings → Voice → Language), that microphone permission is granted, and that a Bengali TTS voice is installed for Bengali replies (Settings → Voice → TTS engine settings).
- **Wake word:** The app listens in the foreground for “Doreman” / “ডোরেমন” and common variants. There is no on-device hotword chip — it is repeated recognizer sessions while the app is in the foreground, so it stops when you leave the app.

---

## 🛠️ Build from source / সোর্স থেকে বানানো

```bash
# Android Studio (recommended) — or:
./gradlew assembleDebug
# Output: app/build/outputs/apk/debug/app-debug.apk
```

Requirements: Java 17, Gradle 8.5 (as in `.github/workflows/build.yml`), `compileSdk 34`, `minSdk 26`.
Every push to `main` also builds the debug APK automatically via GitHub Actions and publishes it to the `latest-apk` release (see Download above).

---

## 🔐 Permissions / পারমিশন

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

## 🔒 Privacy / প্রাইভেসি

- **Your API keys never leave your device except to the provider you chose** (Gemini or your own gateway). They are stored in the Android Keystore (AES-GCM) — there is no Doreman server, and keys are never put in share links or logs.
- Voice is processed by Android's on-device / Google speech service and by your chosen AI provider, as with any voice assistant.
- Saved preferences (“memory”) are stored on-device and only used if you keep Memory enabled.

---

## 🧰 Troubleshooting / সমস্যা হলে

| Symptom | Likely cause / fix |
| :--- | :--- |
| **401 / Invalid key** | Wrong key, or a Gemini key pasted into Gateway mode (or vice versa). Re-enter the key for the mode you selected and Test Connection again. |
| **404 / Model not found** | Model name does not exist on your gateway, or Base URL / Format is wrong. Test Connection → pick a model from the returned list. |
| **429 Rate limit** | Too many requests in a short time. Wait, then retry; or switch to a lighter model. |
| **503 / 529 High demand** | The model/gateway is overloaded. Wait and retry, or pick another model. |
| **Test Connection shows no models** | The gateway may not support `/v1/models`, or the key is wrong for that endpoint. The model list is not required — you can type a model ID manually if you know it works. |
| **Speech stops on long sentences** | v1.2.0 improves this, but some devices ignore the silence hints. Speak in shorter phrases, and check the language setting matches the language you are speaking. |
| **TTS voice missing** | Install the Bengali/English TTS voice from Settings → Voice → TTS engine settings. |
| **Call goes to dialer, not direct call** | `CALL_PHONE` permission is not granted — grant it, or the app intentionally falls back to the dialer. |
| **Auto-install / auto-send does not tap** | Accessibility Service is off. Enable it in Settings → Screen Automation. |

---

## 📜 Changelog

### v1.2.0 (this update)
- Midnight Indigo `#282B4A` + Vanilla `#EEEBDA` theme + Settings option.
- Original animated robot-companion home animation (replaces the 2D orb; not a copy of any cartoon character).
- Bengali/English language setting (`values-bn`, `values-en`) driving speech, TTS and AI reply language.
- Gateway fixes: Bearer-only headers, no silent Gemini-key fallback, honest Test Connection, no stale default model.
- Speech: longer silence windows + timeout delivery of recognised words.
- Local fast-path commands (call, open app, Play Store, YouTube, volume, media) with LLM fallback preserved.
- Version bumped consistently to 1.2.0 (`versionCode 5`); README rewritten with direct download link, setup, commands, permissions, privacy and troubleshooting.

### v1.1.0
- Custom Gateway, silent wake-word, phonetic calling, Play Store automation, WhatsApp auto-send, booster/cleaner.

---

## 🧑‍💻 Tech Stack

- Kotlin 1.9.22, Jetpack Compose Material 3
- Android SpeechRecognizer + TextToSpeech
- OkHttp gateway client (OpenAI + Anthropic formats)
- Google Gemini API + Custom Gateways (Claude / GPT / others)
- DataStore + Android Keystore for settings and keys

## 📄 License

MIT License — see `LICENSE` (if present in the repo).

---

*Note on the home animation: the new robot companion is an original design drawn in code with the app's own theme colours. It is not official artwork of any existing character, and it does not use any existing character's name, bell, pocket or signature colours.*
