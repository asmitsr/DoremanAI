# DoremanAI — Changelog (verified notes only)

Note: the repo README's "What's new in v1.2.0" section is STALE. It is not
the current version. The latest version verified from the repo itself is
v2.1.1, below. No other versions, dates, or features are claimed here.

## v2.1.1 — emergency fix (commit messages dated 30 Oct 2026)

- Source: the repo's own CHANGELOG-2.1.1.md and commit messages on main,
  including "Doreman AI v2.1.1: Home back to Orb, mascot not invoked
  (emergency fix)" and "Doreman AI v2.1.1: version 2.1.1 code 11
  (emergency fix)".
- versionName 2.1.1, versionCode 11.
- Home reverted to the Orb call site; the 3D mascot (Mascot3D) is no longer
  invoked from Home. Mascot3D.kt, MascotState.kt and the model asset are
  kept in the repo, unused for now.
- Reason given in CHANGELOG-2.1.1.md: v2.1.0's SceneView 3D mascot crashed
  on the user's phone when the Home/mascot screen was shown.
- Still a debug APK, not a signed release. The README's Download section
  states the APK is distributed as app-debug.apk under the latest-apk
  release, rebuilt by GitHub Actions on every push to main.

## Latest commit on main at the time of this work

- 3 Nov 2026, commit 50c7ecd: "README: drop extra emojis, say it plainly".
  (Verified via the connector's commit listing; this is a README-only
  change, not a version change.)

## What this file does not claim

- It does not re-state the README's v1.2.0 section as current, and it does
  not invent a release date, feature list, or download link for v2.1.1
  beyond what CHANGELOG-2.1.1.md, the README Download section, and the
  commit messages above actually say.
