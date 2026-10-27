# DoremanAI v1.2.1 — Bugfix (stuck caption + follow-up commands + call clarity)

Base: repo `asmitsr/DoremanAI`, branch `main`, HEAD `209e797` (v1.2.0).
No push was made by this patch. The parent agent pushes these files after review.
`.github/workflows/build.yml` was deliberately NOT touched.

## The bug, in one paragraph

In v1.2.0, after a call action, the home-screen caption kept showing the old
action summary, and follow-up commands spoken without saying "Doreman" again
were silently dropped, so the app looked frozen. A call made without the
CALL_PHONE permission also opened the dial pad while the caption could read
like a completed direct call.

## Changed files (repo paths, mirror the layout under this folder)

1. `app/src/main/java/com/sneha/ai/ui/MainViewModel.kt`
   - `onAppForegrounded()`: sets `awakeForNextCommand = true`, and resets
     `phase` to Idle when the executor is not busy, then relistens as before.
     Returning from the dialer / WhatsApp / another app no longer leaves the
     assistant deaf.
   - `handle()`: the speak `onDone` for a no-action reply now sets
     `awakeForNextCommand = true` before relistening. If speech is off and
     there are no actions, the reply is already complete, so it also re-arms
     follow-up commands.
   - `runActions()`: its `onDone` now sets `awakeForNextCommand = true`.
   - `runActions()` + `startListening()`: the action summary is now transient.
     `startListening()` clears `lastSpokenReply` (a new listening session
     never shows the old summary), and `runActions()` starts a 10-second
     coroutine that clears `lastSpokenReply` only if it still equals that
     summary and the executor is not busy. A previous timer is cancelled on
     a new action or a new listening session.
   - No signatures changed. `awakeForNextCommand` is still set to `false`
     when a command starts, and re-armed only when a reply/action completes.

2. `app/src/main/java/com/sneha/ai/comms/CommsHelper.kt`
   - `placeCall()`: behaviour unchanged — `ACTION_CALL` when CALL_PHONE is
     granted, `ACTION_DIAL` otherwise. Only the dialer message changed, so it
     can never be mistaken for a direct call: it now says the dial pad was
     opened, the green call button must still be pressed, and granting Call
     permission would make it a direct call. The direct-call message already
     said "সরাসরি কল শুরু" and is unchanged.
   - No WhatsApp-call URI was invented — WhatsApp has no reliable public
     call intent here.

3. `app/src/main/java/com/sneha/ai/ai/SystemPrompt.kt`
   - New rule: a plain call request ("কল দাও" / "ফোন করো" / "Call ...") is
     ONLY `call_contact` — never `whatsapp_msg`, and never a WhatsApp call.
     `whatsapp_msg` is ONLY for an explicit WhatsApp message request.

4. `app/src/main/java/com/sneha/ai/ai/LocalCommands.kt`
   - New guard: any input containing "whatsapp" / "হোয়াটসঅ্যাপ" returns
     `null`, so a WhatsApp request is never fast-pathed to a normal phone
     call and instead goes through the LLM plan. A plain call still only
     ever returns `Action.CallContact`, never a WhatsApp action.
   - No other command parsing changed.

5. `app/src/main/java/com/sneha/ai/ui/SettingsScreen.kt`
   - One-line fix only: `RadioRow`'s `Text` now has an explicit
     `color = MaterialTheme.colorScheme.onSurface`. This fixes the reported
     model-list contrast bug (model names rendered near-black on the dark /
     Midnight background). No broader SettingsScreen refactor was done.

6. `app/build.gradle.kts`
   - `versionCode` 5 → 6, `versionName` "1.2.0" → "1.2.1".

## Deliberately NOT changed

- `HomeScreen.kt`: the caption logic (`partial` → Executing text → Thinking
  → `lastReply` → default prompt) is correct once the ViewModel clears
  `lastSpokenReply`; changing the screen was not needed.
- `ActionExecutor.kt`: it already forwards `placeCall()`'s message, so the
  clearer dialer message reaches the caption with no change here.
- `.github/workflows/build.yml`: left unchanged (the GitHub App cannot
  update workflow files; the existing workflow already builds on push to
  `main` and publishes to the `latest-apk` release).
- Package name, stored keys/prefs, and all existing function signatures.

## Verification done here (and what was NOT verified)

- All six changed files were taken from the live repo at HEAD `209e797`
  (MainViewModel / SystemPrompt / LocalCommands verified byte-identical to
  the v1.2.0 push), edited minimally, and diff-checked against those
  originals. Braces and parentheses balance in every changed file.
- NOT verified: there is no Gradle / Android SDK in this workspace, so the
  patch was NOT compiled here. The first GitHub Actions build after push is
  the compile gate and must be green before the APK is announced. Note for
  the pusher: push the files one-by-one as in v1.2.0 — bulk `push_files`
  failed with 403 on the git-trees API last time, and the workflow file
  cannot be updated by the App.

## Phone-test checklist (after push + green build + install)

1. Call WITH Call permission granted → direct call starts, caption says a
   direct call started.
2. Call WITHOUT Call permission → dial pad opens, caption clearly says the
   dial pad opened and the green call button must still be pressed.
3. Cancel the call, return to the app → the old "call done" caption is gone
   (cleared on the next listen, or within 10 seconds at the latest).
4. Without saying "Doreman" again, speak a second command → it runs
   (after a reply, after an action, and after returning from the dialer).
5. Say a WhatsApp message request → it goes to WhatsApp messaging, not to a
   normal phone call; say a plain "call" request → it never becomes a
   WhatsApp action.
6. Settings → AI Brain model list → model names are readable on the dark /
   Midnight background.

## Known limitation (unchanged from v1.2.0)

Speech recognition is still one Android recognizer session per utterance:
v1.2.0 lengthened the silence windows and rescues timeout words, and v1.2.1
re-arms listening after replies/actions, but there is still no background
on-device hotword detector or cross-session stitching.
