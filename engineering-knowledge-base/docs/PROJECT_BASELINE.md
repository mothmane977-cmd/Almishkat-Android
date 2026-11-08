# Verified local application baseline (2026-11-07)

Input archive: `Almishkat_PhoneBuild_Enhanced.zip` (97 files; 25 Kotlin files observed under app/src). Confirmed by inspection of `app/build.gradle.kts` and `docs/PROJECT_STATE.md`.

- Android native `com.almishkat.studio`; `:app` module; Kotlin 2.2.20 and AGP 8.10.1 per project configuration.
- compileSdk/targetSdk 36, minSdk 26, Java 17, Compose BOM 2025.08.00.
- No Media3, FFmpegKit, Lottie or LeakCanary dependency currently listed in `app/build.gradle.kts`.
- `VideoExportEngine.kt` already uses Android MediaCodec and MediaMuxer; retain pending comparative benchmarks.
- `QuranLocalDatabase.kt` SQLite schema version 11 per project state, with migration and Hafs integrity work claimed; tests still need independent execution.
- 95 issue roadmap, M0/M1 marked complete in PROJECT_STATE; status is self-reported until CI/device tests are executed.
- Project code GPL-3.0 per ADR; sacred text and audio excluded in NOTICE.

**Do not:** auto-upgrade to AGP9, auto-migrate to Room, switch to React Native, assume Quran audio can be redistributed, or claim a successful APK build without running it.
