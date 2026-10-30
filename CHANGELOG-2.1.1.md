# Doreman AI v2.1.1 — Emergency fix: 3D mascot temporarily disabled

## Why
v2.1.0 (SceneView 3D mascot on Home) crashed on the user's phone as soon as
the Home/mascot screen was shown. The Orb fallback inside Mascot3D could not
help if the SceneView/Filament engine itself failed during Home composition,
so the safest emergency fix is to stop invoking Mascot3D from Home entirely.

## Changes (vs v2.1.0)
- app/src/main/java/com/sneha/ai/ui/HomeScreen.kt: reverted to the v2.0 Orb
  call site, byte-identical to v2.0 HomeScreen. Tap = mic, as before.
- app/build.gradle.kts: versionName 2.1.1, versionCode 11. SceneView
  dependency is still present but no composable in the app invokes it now,
  so no Filament engine is created at runtime.
- Kept in repo, unused for now: Mascot3D.kt, MascotState.kt,
  app/src/main/assets/models/doreman.glb. They will be re-enabled only after
  the crash cause is isolated (likely SceneView engine/camera/model-load
  behaviour on the user's device, not a compile error — v2.1.0 run #89 was green).

## Not changed
Automation, voice, gateway, AI, Settings, Code Studio, language (English UI).
Still a debug APK, not a signed release.
