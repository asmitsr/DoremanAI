# DoremanAI v2.1.0 — 3D Mascot (SceneView + original low-poly GLB)

Version: 2.1.0 (versionCode 10), up from 2.0.0 (versionCode 9).
Source: local copy of the live v2.0 + HomeScreen fix (head a2332ed) in
`~/workspace/doremanai-update-2.1/`. NOT pushed to GitHub in this step.
Do NOT push `tools/` — it is the local GLB generator, not app code.

## SceneView version + compatibility evidence

- Dependency added (only dependency change):
  `implementation("io.github.sceneview:sceneview:2.1.0")`
- Why 2.1.0 and NOT 4.x: SceneView 4.x requires compileSdk 37, AGP 9.4,
  Kotlin 2.4 and JVM 21. This project is compileSdk 34, targetSdk 34,
  minSdk 26, Java/Kotlin JVM 17, Compose BOM 2024.02.00, Kotlin compiler
  extension 1.5.8, Gradle 8.5 — upgrading that toolchain is out of scope
  and risky for the working app.
- Why 2.1.0 fits (evidence from its published dependencies): it was built
  against Compose 1.6.4, androidx core-ktx 1.12.0 and kotlin-stdlib 1.8.22.
  The project's Compose BOM 2024.02.00 is the Compose 1.6.x line and it
  already uses core-ktx 1.12.0, so no Compose/core version clash is
  expected from this dependency itself. compileSdk 34 can consume a library
  built for an older SDK; minSdk 26 is above SceneView's minSdk 24 floor.
- API used (SceneView 2.x line, from its docs/samples and the published
  ModelNode source): `Scene` composable with `engine`, `modelLoader`,
  `cameraNode`, `childNodes`; `rememberEngine()`, `rememberModelLoader()`,
  `rememberCameraNode()`, `rememberNodes()`; `ModelNode(modelInstance =
  modelLoader.createModelInstance("models/doreman.glb"), scaleToUnits =
  1.0f)`; `ModelNode.playAnimation(name, loop)` /
  `stopAnimation(name)` (the 2.x ModelNode keeps one PlayingAnimation per
  animation index, so the blink track can run alongside a base track).
- API uncertainty (honest): there is no Android SDK here, so the Gradle
  build was NOT verified. If the 2.1.0 `Scene`/`Position`/`cameraNode`
  signatures differ in a detail, the first Actions build will fail on
  Mascot3D.kt only — the GLB, state mapping and HomeScreen logic are
  unaffected, and the fix is confined to that one file. The main agent
  must verify via GitHub Actions after push and must NOT report the APK
  as live until the head run is green.

## GLB model (original, programmatic, no Blender)

- Path: `app/src/main/assets/models/doreman.glb`
- Size: 18,124 bytes (well under the 500 KB target)
- Generator (local only, do not push): `tools/generate_doreman_glb.py`
  (pure Python, no external libraries). Re-run it to regenerate the GLB.
- Design — ORIGINAL robot, not a Doraemon copy: round violet/indigo head
  and body, dark eyes with small white highlights, flat pink mouth,
  cyan-tipped antenna, two arms. No blue cat face, no red nose, no bell,
  no pocket, no copied artwork.
- Nodes (14): root, body, head, eyeLeft, eyeRight, mouth, antenna,
  armLeft, armRight, armLeftMesh, armRightMesh, antennaTip,
  eyeLeftHighlight, eyeRightHighlight.
- Meshes (8, shared geometry): violet sphere, dark sphere, eye sphere,
  mouth box, antenna cylinder, arm box, tip sphere, highlight sphere.
- Animations (7, exact names, all targeting real nodes):
  - `idle` — root translation bob (2.0s loop)
  - `blink` — eyeLeft/eyeRight scale Y close (3.0s loop, brief close)
  - `listening` — head rotation Y turn (1.6s loop)
  - `speaking` — mouth scale Y (1.0s loop)
  - `thinking` — head rotation (tilt + turn, 2.4s loop)
  - `happy` — root jump + both arms up (2.0s, played once)
  - `touch` — head tilt + right-arm wave (1.5s, played once)
- Important glTF detail fixed during generation: animation scale values
  are ABSOLUTE, not relative. Blink/speaking outputs therefore use the
  eye/mouth nodes' real base scales (eyes [0.11, 0.13, 0.05], mouth
  [0.14, 0.035, 0.02]); the first draft used [1, 1, 1] and would have
  made the eyes/mouth huge, and was corrected before delivery.
- No skeleton: these are node-transform animations, not a skeletal rig.
  That is enough for head/eye/mouth/arm motion, but it is not a
  professional Blender character with bones, morphs or facial rigging.

## New / changed files

- NEW `app/src/main/java/com/sneha/ai/ui/Mascot3D.kt` — loads
  `models/doreman.glb` via SceneView 2.1.0, plays the mapped animation,
  layers blink on top, shows a static frame when reduceMotion is true,
  and falls back to the existing `Orb()` if model creation or animation
  throws — the home screen never crashes because of the mascot.
- NEW `app/src/main/java/com/sneha/ai/ui/MascotState.kt` — animation-name
  constants, `baseFor(Phase)`, `resolve(Phase, transient)`, and the
  transient durations (happy 2000 ms, touch 1500 ms).
- CHANGED `app/src/main/java/com/sneha/ai/ui/HomeScreen.kt` — only the
  Orb call site and the two trigger counters. The Orb is replaced by a
  tappable Box containing `Mascot3D`; the tap target is the Box, not the
  Scene surface, so tap behaviour is identical for the 3D model and the
  Orb fallback.
- CHANGED `app/build.gradle.kts` — SceneView 2.1.0 added, version
  2.1.0 / code 10. Nothing else changed.
- NEW `app/src/main/assets/models/doreman.glb` (see above).
- UNCHANGED: every automation, voice, gateway, AI, coding, settings and
  MainViewModel/ActionExecutor file (verified by diff against the 2.0
  tree: only `build.gradle.kts` and `HomeScreen.kt` differ, plus the new
  files). The old `Orb.kt` is kept as the fallback and is still used
  whenever the model fails.

## State mapping

| Assistant state | GLB animation | Loop |
| --------------- | ------------- | ---- |
| Idle | idle (+ blink) | yes |
| Listening | listening (+ blink) | yes |
| Thinking | thinking (+ blink) | yes |
| Speaking | speaking (+ blink) | yes |
| Executing | idle (+ blink) | yes |
| Action just completed | happy (+ blink) | once, ~2s, then base |
| Mascot tapped | touch (+ blink) | once, ~1.5s, then base |
| reduceMotion on | (static frame, no animation) | — |

- Happy trigger: HomeScreen watches `vm.exec.running` go from true to
  false. ExecStatus has NO error flag, so a finish is treated as a
  success unless its status text mentions "error"/"fail" — that is an
  approximation, documented here, not a change to executor logic.
- Touch trigger: the mascot tap increments `touchTrigger` AND calls the
  existing `onMic()`, so microphone behaviour is unchanged.

## Fallback behaviour

- Model missing/corrupt, SceneView unavailable, or any animation error
  → `Orb()` (the previous 2D robot) is shown with the same phase, voice
  level and reduceMotion handling. No crash, no blank home screen.
- If SceneView fails at Gradle level (API mismatch), the failure is
  confined to Mascot3D.kt; reverting to the 2.0 HomeScreen (Orb only)
  restores a green build.

## Honest limits

- Simple low-poly programmatic model, not a professional Blender
  character: flat colours, no textures, no facial morphs, no legs, no
  detailed hands. It reads as a small robot companion, not a polished
  game character.
- Animation quality is limited to head/eye/mouth/arm/root transforms;
  there is no lip-sync to actual speech, no gaze tracking, and no
  skeletal deformation.
- APK size increase from SceneView/Filament native libraries is NOT
  quantified here (no build was run); expect several MB on top of the
  15.32 MB v2.0 APK, and check the real release asset size after the
  first green build.
- The SceneView 2.1.0 API was researched from docs/samples/source, not
  compiled here. The main agent must verify with GitHub Actions and
  must not claim a green build or a live APK until the head run passes.
- No Bengali text was added to any app UI file in this change
  (verified by grep: 0 Bengali characters in the new/changed files).
