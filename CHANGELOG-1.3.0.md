# DoremanAI v1.3.0 — Full-phone control (LIA / private-assistant style)

## What the two videos actually use
1. **LIA AI (প্রথম ভিডিও):** Accessibility permission + Overlay (অন্য অ্যাপের উপরে দেখানো) + Background permission। এর মাধ্যমে WhatsApp মেসেজ পাঠানো, YouTube-এ গান চালানো, Facebook পোস্ট — অর্থাৎ Accessibility দিয়ে অন্য অ্যাপের বোতামে ট্যাপ করা।
2. **Private assistant (দ্বিতীয় ভিডিও):** Android Accessibility Service দিয়ে স্ক্রিনের লেখা/বোতাম পড়া, AI দিয়ে পরের ধাপ ঠিক করা, তারপর Accessibility gesture দিয়ে ট্যাপ করা। রুট লাগে না। AI provider হিসেবে OpenRouter / DeepSeek / যেকোনো OpenAI-compatible gateway — Doreman-এর Custom Gateway-এর মতোই।

v1.3.0 ঠিক এই দুই প্যাটার্নই Doreman-এ এনেছে — কোনো নতুন কোম্পানির কোড কপি করা হয়নি।

## New in v1.3.0
- **স্ক্রিন দেখা (ScreenWatcher):** `SnehaAccessibilityService.onAccessibilityEvent()` আগে খালি ছিল। এখন প্রতিটি উইন্ডো বদলে সামনের অ্যাপের নাম + সর্বোচ্চ ৬০টি লেখা/বোতাম (পাসওয়ার্ড ফিল্ড কখনো না) স্ন্যাপশট হিসেবে রাখে। "স্ক্রিনে কী আছে" বললে সেটা পড়ে শোনায়, WhatsApp/Settings-এ গেলেও।
- **সারাক্ষণ চালু (StayAwake):** নতুন `DoremanVoiceService` ফোরগ্রাউন্ড সার্ভিস (microphone type) + নোটিফিকেশন + ফ্লোটিং "Doreman" বোতাম। অন্য অ্যাপে থাকলে বোতামে ট্যাপ করলেই Doreman সামনে আসে, আর v1.2.1-এর ON_RESUME লজিক পরের কমান্ড "Doreman" না বললেও চালায়। "সারাক্ষণ চালু রাখো" / "সারাক্ষণ বন্ধ করো"।
- **ফ্ল্যাশলাইট:** "ফ্ল্যাশলাইট জ্বালাও / বন্ধ করো", "টর্চ জ্বালাও" — CameraManager torch, Camera অনুমতি লাগে।
- **WiFi:** "ওয়াইফাই চালু/বন্ধ করো", "X ওয়াইফাইতে কানেক্ট করো", "ওয়াইফাই স্ট্যাটাস"। কানেক্ট = WiFi সেটিংস খুলে Accessibility দিয়ে SSID-তে ট্যাপ।
- **Bluetooth:** "ব্লুটুথ চালু/বন্ধ করো", "ব্লুটুথে X কানেক্ট করো", "পেয়ার করা ডিভাইস"। কানেক্ট = Bluetooth সেটিংস খুলে ডিভাইসে ট্যাপ, তারপর A2DP স্ট্যাটাস চেক।
- **নতুন কমান্ড (Local + LLM):** ActionPlan-এ `flashlight`, `wifi`, `bluetooth`, `stay_awake` যোগ হয়েছে; SystemPrompt-এ একই নিয়ম।

## Honest Android limits — এগুলো কোনো অ্যাপই চুপচাপ করতে পারবে না
- **WiFi ON/OFF সরাসরি হবে না:** Android 10+ এ সাধারণ অ্যাপের `setWifiEnabled()` সবসময় false দেয় (শুধু Device Owner / সিস্টেম অ্যাপ পারে)। তাই Doreman WiFi সেটিংস খুলে দেবে, টগলটা আপনাকে একবার চাপতে হবে। এটা ভিডিওর অ্যাপগুলোর ক্ষেত্রেও একই।
- **WiFi পাসওয়ার্ড টাইপ করবে না:** সেভ করা / ওপেন নেটওয়ার্কে ট্যাপেই কানেক্ট হবে। নতুন সিকিউরড WiFi-তে পাসওয়ার্ডের ঘরে Doreman লিখবে না (পাসওয়ার্ড ফিল্ডে লেখা কোডেই নিষিদ্ধ, নিরাপত্তার জন্য) — পাসওয়ার্ড নিজে টাইপ করে Connect চাপুন। ভয়েসে WiFi পাসওয়ার্ড বলবেন না।
- **Bluetooth পেয়ারিং:** আগে থেকে পেয়ার করা ডিভাইসেই কানেক্ট হবে। নতুন ডিভাইস পেয়ার করতে সেটিংসে Confirm লাগতে পারে।
- **সবসময় মাইক শোনা নয়:** ব্যাকগ্রাউন্ডে লুকিয়ে চিরকাল শোনা Android-এ সাধারণ অ্যাপের জন্য নিষিদ্ধ/অনির্ভরযোগ্য, আর ব্যাটারিও খাবে। v1.3.0-এর চুক্তি: ফোরগ্রাউন্ড সার্ভিস + ফ্লোটিং বোতাম = এক ট্যাপে ফিরে আসা, লুকিয়ে শোনা নয়।

## Install-এর পর একবার যা করতে হবে (নাহলে কাজ করবে না)
1. Settings > স্ক্রিন অটোমেশন থেকে **Accessibility চালু** করুন।
2. **Display over other apps** (Overlay) অনুমতি দিন — নাহলে ফ্লোটিং বোতাম আসবে না (তখন নোটিফিকেশনে ট্যাপ করে ফিরতে হবে)।
3. **Camera, Bluetooth, Location** অনুমতি দিন (ফ্ল্যাশলাইট / Bluetooth / WiFi স্ট্যাটাসের জন্য)।
4. **Battery optimization অফ** করুন (নাহলে ফোন সার্ভিস মেরে দেবে)।
5. প্রথমবার ভয়েসে বলুন: "সারাক্ষণ চালু রাখো"।

## Test checklist
1. WhatsApp-এ গিয়ে ফ্লোটিং বোতামে ট্যাপ → Doreman ফিরে আসে, "Doreman" না বললেও পরের কমান্ড চলে।
2. "স্ক্রিনে কী আছে" → সামনের অ্যাপের লেখা পড়ে।
3. "ফ্ল্যাশলাইট জ্বালাও" / "ফ্ল্যাশলাইট বন্ধ করো"।
4. "ওয়াইফাই স্ট্যাটাস", সেভ করা WiFi-তে কানেক্ট, WiFi অন/অফ (সেটিংস খুলবে + টগলের কথা বলবে)।
5. "ব্লুটুথ চালু করো", পেয়ার করা ডিভাইসে কানেক্ট।
6. "সারাক্ষণ বন্ধ করো" → ফ্লোটিং বোতাম + নোটিফিকেশন চলে যায়।

## Files (v1.3.0, versionCode 7)
- `app/src/main/AndroidManifest.xml` (নতুন অনুমতি + VoiceService + MainActivity singleTop)
- `app/build.gradle.kts` (1.3.0 / 7)
- `app/src/main/java/com/sneha/ai/automation/SnehaAccessibilityService.kt` (স্ক্রিন স্ন্যাপশট)
- `app/src/main/java/com/sneha/ai/automation/DeviceControl.kt` (নতুন)
- `app/src/main/java/com/sneha/ai/automation/DoremanVoiceService.kt` (নতুন)
- `app/src/main/java/com/sneha/ai/automation/ActionExecutor.kt` (নতুন অ্যাকশন ডিসপ্যাচ + ReadScreen স্ন্যাপশট ফলব্যাক)
- `app/src/main/java/com/sneha/ai/ai/ActionPlan.kt` (flashlight/wifi/bluetooth/stay_awake)
- `app/src/main/java/com/sneha/ai/ai/LocalCommands.kt` (বাংলা/ইংরেজি নতুন কমান্ড)
- `app/src/main/java/com/sneha/ai/ai/SystemPrompt.kt` (নতুন অ্যাকশন + নিয়ম)
- `app/src/main/res/xml/accessibility_service_config.xml` (flagRetrieveInteractiveWindows)
