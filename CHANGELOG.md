# DoremanAI — Changelog

## v1.2.0 — All requested updates, in one release (push-ready, NOT yet pushed)

**Base:** `asmitsr/DoremanAI`, branch `main`, HEAD `e9c058e` at time of preparation.
**Package NOT renamed:** `com.sneha.ai` is kept everywhere, so installed-app upgrades and stored keys/prefs are not broken.
**NOT pushed to GitHub by this preparation.** The parent agent must review these files and get the user's push approval first; on push to `main`, the existing workflow builds the debug APK and overwrites the `latest-apk` release.
**NOT compiled here:** Gradle/Android SDK were not available in this workspace, so these changes were checked for syntax consistency (balanced braces, existing imports/signatures, XML validity) but **not** built. The first GitHub Actions build after push is the real compile test — if it fails, fix from its log before announcing the APK.

### Files changed (repo path → what changed)

1. `app/src/main/java/com/sneha/ai/ui/Theme.kt`
   - New `MidnightScheme`: background `#282B4A` (Midnight Indigo), text/surfaces in Vanilla `#EEEBDA`, indigo containers `#4A4E7A`, muted indigo secondary `#AEB2D8`.
   - `SnehaTheme("midnight", …)` now selects it; Dark/Light/System behaviour unchanged.

2. `app/src/main/java/com/sneha/ai/ui/Orb.kt` — **rewritten, same signature**
   - `Orb(phase, level, reduceMotion, modifier)` unchanged, so `HomeScreen.kt` compiles untouched.
   - Idle colours now come from `MaterialTheme.colorScheme` (so Midnight Indigo changes the orb); Listening/Thinking/Speaking/Executing keep distinct state colours.
   - **Original** robot-companion face drawn in code: rounded robot ears/antennae, blinking eyes, speaking mouth that opens with voice level, soft cheeks, orbiting rings, voice-reactive pulse. No Doraemon character design, colours, bell, pocket or artwork is copied.

3. `app/src/main/java/com/sneha/ai/ui/SettingsScreen.kt`
   - `LookPage`: new radio “মিডনাইট ইন্ডিগো (Midnight Indigo #282B4A + Vanilla #EEEBDA)” writing `K.theme = "midnight"`.
   - `VoicePage`: new “ভাষা / Language” card with বাংলা / English radios writing `K.lang` (`"bn"` / `"en"`), plus a note explaining what the switch changes.
   - `AiPage`: troubleshooting note under Test Connection (Base URL + Auth Scheme + Format + model must match; 401 = wrong key, 404 = wrong model/URL).
   - `AboutPage`: version text updated to v1.2.0.

4. `app/src/main/java/com/sneha/ai/ai/GatewayProvider.kt` — **Gemini provider untouched**
   - Bearer mode: sends **only** `Authorization: Bearer <key>`. Anthropic headers (`x-api-key`, `anthropic-version`) are sent **only** in `x-api-key` mode.
   - Blank gateway key: clear `NO_KEY` error. The old silent fallback to the Gemini key is removed.
   - `listModels()` / Test Connection: hard-coded fallback model list removed. Failures now throw the real error (401/404/429/5xx/network/timeout); a successful empty response returns an empty list, which `MainViewModel.testConnection()` already reports honestly.
   - Blank model: new `requireModel()` throws `MODEL_NOT_FOUND` with a “pick a model from Test Connection” message. The stale `claude-3-5-sonnet-20241022` default is removed (it now appears only in a comment explaining the removal).

5. `app/src/main/java/com/sneha/ai/voice/SpeechInput.kt`
   - Added `EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS = 2500`, `EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS = 1200`, `EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS = 1000`.
   - Partial results are tracked (`lastPartial`, longest form kept). On a speech-timeout/no-match **after** words were recognised, those words are delivered via `onFinal` instead of being dropped. A timeout with no recognised words still reports an error.
   - Language mapping unchanged: `bn` → `bn-BD`, otherwise `en-US`. Signature unchanged.

6. `app/src/main/java/com/sneha/ai/ui/MainViewModel.kt`
   - New fast-path in `send()`: `LocalCommands.parse()` runs **before** the LLM call (skipped on retry). A match adds the user message and goes through the existing `handle()` → `ActionExecutor` flow; a non-match falls through to the unchanged LLM JSON plan flow.
   - `startListening()` `onError` documented: SpeechInput already delivers timeout-recognised words as `onFinal`, so `onError` here means nothing was recognised and relistening drops nothing.

7. `app/src/main/java/com/sneha/ai/ai/LocalCommands.kt` — **new file**
   - Pure parser returning existing `Plan`/`Action` types only. Covered: call a contact (“বাবাকে কল দাও”, “Call Baba”), open an app, Play Store install/download, YouTube search, volume up/down/mute/unmute, media play/pause/next/previous.
   - Deliberately **not** fast-pathed: SMS and WhatsApp (target + message parsing is ambiguous in free speech; they stay on the LLM path, which handles phonetic names and letter-spelling).

8. `app/src/main/java/com/sneha/ai/ai/SystemPrompt.kt`
   - Reply language now follows `Prefs.lang` explicitly: Bengali reply instruction when `lang == "bn"`, English otherwise. Banglish understanding is still stated. No action types were added or removed.

9. `app/src/main/res/values/strings.xml`, `app/src/main/res/values-bn/strings.xml` (new), `app/src/main/res/values-en/strings.xml` (new)
   - Same key set in all three: app name, accessibility description, home hint/call-to-action/stop/app-picker strings, language and theme labels (including Midnight), voice-test and gateway-tip strings.
   - **Not a complete UI translation** (see “Not safely done” below).

10. `app/build.gradle.kts`
    - `versionCode 4 → 5`, `versionName "1.0.0" → "1.2.0"`. Package, SDKs and dependencies unchanged.

11. `.github/workflows/build.yml`
    - Release name `v1.1.0 → v1.2.0`. Build steps, `latest-apk` tag and debug APK unchanged.

12. `README.md` — **rewritten**
    - Direct download link `https://github.com/asmitsr/DoremanAI/releases/tag/latest-apk` (debug APK, rebuilt on push to `main`); stray `[span_0]` markers removed.
    - Bengali + English sections: what it is, v1.2.0 changes, features, quick setup, third-party key setup with the four must-match items and the v1.2.0 fixes, command list, speech tips, build-from-source, permissions, privacy (Android Keystore, no server), troubleshooting table (401/404/429/503, speech timeout, TTS, call fallback, Accessibility), changelog, tech stack, MIT licence, and a note that the home animation is an original design.

### Files deliberately NOT changed

- `GeminiProvider.kt` — untouched, per the brief.
- `HomeScreen.kt` — untouched; it still calls `Orb(phase, level, reduceMotion, modifier)` and the theme wrapper unchanged.
- `SpeechOutput.kt` — no code change needed: `MainViewModel.speak()` already passes `settings.value.lang` to it, and it already picks `bn-BD/bn-IN/bn` vs `en-US/en-GB/en-IN` TTS voices. The new language setting therefore wires through it automatically.
- `ActionPlan.kt`, `ActionExecutor.kt`, `CommsHelper.kt` — untouched; fast-path actions reuse their existing types and execution paths (including `placeCall()`'s dialer fallback and Accessibility gating).
- Package name, DataStore name (`sneha_prefs`) and Keystore alias — untouched, to avoid breaking upgrades and stored keys.

### What could NOT safely be done, and why

1. **Exact Doraemon 3D animation — not done.** Doraemon is a copyrighted character. The replacement is an original 2D-drawn (Canvas) robot companion with 3D-style gloss, pulse and orbiting rings — not a `.glb`/`.gltf` 3D model, and not a Doraemon replica. A true 3D model would also need a new SceneView/Filament dependency and a licensed/original model asset, which was not available here.
2. **Full UI translation — not done.** `HomeScreen.kt` and `SettingsScreen.kt` contain many hard-coded Bengali/English literals (tool rows, section titles, notes, button labels, error strings in `MainViewModel`/`AiProvider`). The new `values-bn`/`values-en` files are the key foundation, but extracting every literal into `stringResource()` calls is a large, error-prone refactor that should be done as its own pass with a build to verify each screen.
3. **Signed / Play Store APK — not done.** The existing workflow builds a debug APK only and there is no signing config in the repo. The download link is therefore the debug APK from the `latest-apk` release, not a signed release.
4. **True continuous / on-device hotword listening — not done.** Android `SpeechRecognizer` sessions still end on silence; v1.2.0 lengthens the silence windows and rescues timeout-recognised words, but it does not add a background hotword detector, continuous dictation, or cross-session sentence stitching beyond the timeout rescue. Hands-free relistening still restarts a new session after each utterance.
5. **Compile verification — not done here.** No Gradle/Android SDK in this workspace. Treat the first Actions build as the compile gate.

### What the user must test on a real phone after push

1. **Build:** Push → Actions → `latest-apk` release appears with a fresh `app-debug.apk`; install it (uninstall the old build first if install conflicts, since the package is unchanged this should upgrade normally).
2. **Theme:** Settings → Theme → Midnight Indigo — home, settings and orb should all use indigo/vanilla and remain readable; switch back to Dark/Light to confirm no regression.
3. **Language:** Settings → Voice → Language → English, then speak in English and confirm recognition, TTS voice and AI reply language all switch; switch back to বাংলা and repeat.
4. **Gateway:** Settings → AI Brain → Custom Gateway — (a) wrong key → Test Connection must now show a real error, not a model list; (b) correct key + matching Base URL/Auth Scheme/Format → Test Connection lists real models; (c) pick a model and send a message; (d) blank gateway key must show “key not set”, never a Gemini-key 401.
5. **Speech:** Speak a long sentence with pauses — the recognised words should survive a pause/timeout instead of vanishing; try a wake-word + long command.
6. **Commands:** “বাবাকে কল দাও” / “Call <name>” (check it calls the right contact or shows the existing not-found/dialer behaviour), “YouTube খোলো”, “Free Fire ডাউনলোড করো”, volume and next-song commands — and confirm an ordinary question still goes to the AI and gets a normal answer (LLM fallback).
7. **Animation:** Home robot should blink, pulse with voice level, and change mouth/state for Listening/Speaking/Thinking/Executing; with Reduce Motion on, it should stay static.
