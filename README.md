<div align="center">

# 🤖 Doreman AI (ডোরেমন এআই)

[![Kotlin](https://img.shields.io/badge/KOTLIN-1.9.22-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![Jetpack Compose](https://img.shields.io/badge/JETPACK%20COMPOSE-MATERIAL%203-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Custom Gateway](https://img.shields.io/badge/CUSTOM%20GATEWAY-CLAUDE%20%7C%20GPT-10B981?style=for-the-badge)](https://github.com/)
[![Google Gemini](https://img.shields.io/badge/GOOGLE%20GEMINI-API-8E75B2?style=for-the-badge)](https://ai.google.dev/)

**An advanced Siri-style voice-controlled personal assistant for Android, powered by cutting-edge AI.**

</div>

---

## 📌 Introduction

**Doreman AI** হলো আপনার অ্যান্ড্রয়েড ডিভাইসের জন্য তৈরি একটি স্মার্ট ও দ্রুতগতির ভয়েস-কন্ট্রোলড পার্সোনাল অ্যাসিস্ট্যান্ট। এটি আপনার প্রাত্যহিক কাজগুলোকে ভয়েস কমান্ডের মাধ্যমে সম্পূর্ণ স্বয়ংক্রিয় করে তোলে।

---

## ⚙️ Core Features (v1.1.0)

| Feature | Status | Description |
| :--- | :---: | :--- |
| **Custom Gateway (`sk-...` Token)** | ✅ Active | Support for third-party Base URLs and premium models (Claude Opus/Sonnet, GPT-4o). |
| **Silent Wake-Word (`"Doreman"`)** | ✅ Active | Wakes up instantly and silently upon hearing the wake word without annoying beep sounds. |
| **Phonetic Contact Calling** | ✅ Active | Automatically searches and calls saved contacts using Bengali or English pronunciation. |
| **Play Store Automation** | ✅ Active | Opens the Play Store via voice command and automatically taps the `Install` button. |
| **WhatsApp Auto-Send** | ✅ Active | Finds contacts or numbers, types messages, and automatically triggers the `Send` button. |
| **Mobile Booster & Cleaner** | ✅ Active | Clears device cache and launches system cleaner tools instantly. |

---

## 📥 Download & Quick Setup

1. **Download APK:** Go to the [Releases](https://github.com/asmitsr/DoremanAI/releases) section on the right, download the latest **`app-debug.apk`**, and install it on your device[span_0](start_span)[span_0](end_span).
2. **Permissions:** Open the app, navigate to **Settings (⚙️) > Permission Setup Hub**, and grant all required permissions including **Accessibility**.
3. **Configure AI Brain:** Go to **Settings (⚙️) > AI Brain & Custom Gateway**, input your **Gateway Base URL** & **API Key** (or Gemini API Key), and tap **Test Connection**.

---

## 🛠️ Tech Stack

* **Language:** Kotlin[span_1](start_span)[span_1](end_span)
* **UI Toolkit:** Jetpack Compose (Material 3)[span_2](start_span)[span_2](end_span)
* **AI Integration:** Google Gemini API & Custom LLM Gateways[span_3](start_span)[span_3](end_span)

---

## 📄 License

This project is open-source and available under the [MIT License](LICENSE).
