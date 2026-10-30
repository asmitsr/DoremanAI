# DoremanAI v2.0.0 — English-only UI + UI/UX redesign

Version: 2.0.0 (versionCode 9), up from 1.3.1 (versionCode 8).
Source: local copy of the live `asmitsr/DoremanAI` main branch at v1.3.1.
This folder is NOT the repository itself. Nothing here was pushed to GitHub,
and no workflow file was touched.

## What changed

### Full English UI (visible text only)
- Every visible Bengali string, label, error message, notification text and
  comment in the app was translated to clear English, including HomeScreen,
  SettingsScreen, CodingScreen, CodingViewModel, MainViewModel (connection
  messages, greetings, errors), ActionPlan descriptions, AiProvider /
  GatewayProvider / GeminiProvider error messages, ActionExecutor and
  DeviceControl result messages, CommsHelper result messages, speech
  input/output errors, and the SystemPrompt examples (now English examples).
- `app/src/main/res/values-bn/strings.xml` now holds the same English
  strings as `values/strings.xml` (same keys). The file was NOT deleted, so
  no resource reference breaks; it simply no longer contains Bengali text.
- Home tool buttons now send English commands ("Clean my phone",
  "Play a popular song", "Lock my phone", "Open the power menu") instead
  of Bengali commands, so they match the English AI prompt.
- `Prefs.kt`: the default language changed from `"bn"` to `"en"`.
  Existing installs that already saved `"bn"` keep that saved value until
  the user changes it in Settings — a default change does not rewrite a
  saved preference.
- The Settings language choice is kept, with options labelled in English
  only ("Bengali", "English").

### UI/UX redesign (Material3 only, no new dependencies)
- `ui/Theme.kt`: added shared Shapes (small 12dp, medium 16dp, large 20dp)
  and passed them to `MaterialTheme`. All existing colour schemes and the
  `SnehaTheme(mode, fontScale)` signature are unchanged.
- New `ui/DoremanComponents.kt`: reusable `DoremanCard`, `SectionTitle`,
  `StatusChip` and `QuickActionItem` composables (Material3 only).
- `ui/HomeScreen.kt` — dashboard, without removing existing behaviour
  (Orb tap = microphone, text input send, tools row, apps dialog, STOP
  button, key-setup banner, dismissible banner all kept):
  - Greeting + assistant-status card ("Good day — Doreman is ready",
    or "Set up your AI key to get started" / "Connection needs attention",
    with a Connected / Connection failed / Key missing / Not tested yet
    chip) built from the existing phase, keySet and connOk state.
  - Quick Actions grid: AI Chat (stays on home), Automations
    (navigates to `settings:auto`), App Control (`settings:apps`),
    Code Studio (`coding`).
  - Recent Activity: the last 3 real chat messages from `vm.messages`
    (role + text), or "No recent activity yet" when empty. No invented
    automation counts, dates or fake history.
  - The dashboard scrolls above a pinned input row, so the new sections
    never push the input off-screen.
- `ui/SettingsScreen.kt`: every page key and page composable works as
  before, but the landing page is grouped under 6 headings in this order:
  "AI & Models" (ai), "Automation & Permissions" (auto, privacy),
  "Voice & Accessibility" (voice), "Apps & Integrations" (apps, coding),
  "Privacy & Security" (memory, notif), "Appearance & About" (look, about).
  `SecCard`, `SwitchRow` and `RadioRow` helpers are unchanged.
  About now reads "Doreman AI — v2.0.0".
- `ui/CodingScreen.kt`: a line-number column (1..N) sits beside the code
  editor, and Run / Stop / Save / Export are separate labelled actions
  (Stop is also kept as the existing "Cancel wait" button while running).
  The Termux permission flow, sandbox preview and approval dialog are
  unchanged, and the honest capability note is kept in English:
  HTML/CSS/JS preview in a sandboxed WebView (network off);
  Python/C/C++/Java/Bash/Node need Termux with approval each time;
  Kotlin and others are only generated, not run.

### Files changed (28, including 1 new file)
- `app/build.gradle.kts` — version only (2.0.0 / 9).
- `app/src/main/java/com/sneha/ai/ai/ActionPlan.kt`
- `app/src/main/java/com/sneha/ai/ai/AiProvider.kt`
- `app/src/main/java/com/sneha/ai/ai/GatewayProvider.kt`
- `app/src/main/java/com/sneha/ai/ai/GeminiProvider.kt`
- `app/src/main/java/com/sneha/ai/ai/LocalCommands.kt`
- `app/src/main/java/com/sneha/ai/ai/SystemPrompt.kt`
- `app/src/main/java/com/sneha/ai/apps/AppLauncher.kt`
- `app/src/main/java/com/sneha/ai/automation/ActionExecutor.kt`
- `app/src/main/java/com/sneha/ai/automation/DeviceControl.kt`
- `app/src/main/java/com/sneha/ai/automation/DoremanVoiceService.kt`
- `app/src/main/java/com/sneha/ai/automation/SnehaAccessibilityService.kt`
- `app/src/main/java/com/sneha/ai/coding/TermuxRunner.kt`
- `app/src/main/java/com/sneha/ai/comms/CommsHelper.kt`
- `app/src/main/java/com/sneha/ai/data/MemoryStore.kt`
- `app/src/main/java/com/sneha/ai/data/Prefs.kt`
- `app/src/main/java/com/sneha/ai/data/SecureKeyStore.kt`
- `app/src/main/java/com/sneha/ai/media/YouTubeHandoff.kt`
- `app/src/main/java/com/sneha/ai/ui/CodingScreen.kt`
- `app/src/main/java/com/sneha/ai/ui/CodingViewModel.kt`
- `app/src/main/java/com/sneha/ai/ui/DoremanComponents.kt` (new)
- `app/src/main/java/com/sneha/ai/ui/HomeScreen.kt`
- `app/src/main/java/com/sneha/ai/ui/MainViewModel.kt`
- `app/src/main/java/com/sneha/ai/ui/SettingsScreen.kt`
- `app/src/main/java/com/sneha/ai/ui/Theme.kt`
- `app/src/main/java/com/sneha/ai/voice/SpeechInput.kt`
- `app/src/main/java/com/sneha/ai/voice/SpeechOutput.kt`
- `app/src/main/res/values-bn/strings.xml`
- Unchanged (no Bengali text and no redesign needed): `MainActivity.kt`,
  `SnehaApp.kt`, `Orb.kt`, `ChatStore.kt`, `ProjectStore.kt`,
  `values/strings.xml`, `values-en/strings.xml`, `AndroidManifest.xml`,
  `res/xml/accessibility_service_config.xml`, and all workflow files.

## Bengali grep result (honest, not zero)
After the translation, 86 lines still contain Bengali script (U+0980–U+09FF).
All 86 are non-visible matching logic, deliberately kept so existing
behaviour does not break:
- `LocalCommands.kt` (34): Bengali voice-command keywords and suffixes.
- `AppLauncher.kt` (20): Bengali app-name aliases (e.g. YouTube, WhatsApp)
  and the "settings" alias check.
- `CommsHelper.kt` (23): Bengali contact-name aliases, the Bengali-to-Latin
  transliteration table, and sensitive-word detection.
- `ActionExecutor.kt` (3): on-screen button labels tapped by Accessibility
  (Install / Search in Bengali) — these match what other apps display.
- `SnehaAccessibilityService.kt` (3): on-screen Send / Install / Search
  label lists for the same reason.
- `ActionPlan.kt` (1): the "off" state word used when parsing an action.
- `MemoryStore.kt` (1): sensitive-word detection (password / OTP / PIN).
- `MainViewModel.kt` (1): the wake-word list, kept for compatibility.
No visible UI string, error message, notification or resource string
contains Bengali script any more.

## Honest limits — read before pushing
- NOT compiled here. There is no Android SDK in this environment, so no
  Gradle build was run and the build is NOT claimed green. Push this to
  GitHub and judge only the latest Actions run for the new head commit,
  per the existing build-verification rule, before sharing an APK.
- No new dependencies were added; everything uses the existing Compose
  Material3 setup in `app/build.gradle.kts`.
- Automation engine logic, function signatures, class/package names,
  navigation route strings, preference keys and API/model names are
  unchanged. Only string contents, comments and the UI layout pieces
  listed above changed.
- Code Studio line numbers scroll with the page (the whole screen uses
  the existing vertical scroll); they are not synced to the text field's
  own internal scroll, and their line height is an approximation of the
  editor's 12sp monospace text, not a full editor gutter.
- The Bengali language setting still exists (labelled "Bengali" in
  English). If a user selects it, speech recognition (bn-BD) and the AI
  reply language still follow that setting, and Bengali voice commands
  still match via LocalCommands — only the app's own visible/spoken UI
  strings are English. If the goal is to remove Bengali input entirely,
  that setting and the matching tables must be removed in a separate,
  explicitly approved step.
- Existing installs keep a previously saved `"bn"` preference (see above);
  only fresh installs / unset preferences default to English.
- Brace/paren counts: every Kotlin file balances except `ActionPlan.kt`
  (70 `{` vs 69 `}`), which was already 70/69 in the original v1.3.1 file
  because of a `{` inside a string literal — this is pre-existing, not
  introduced by v2.0.
