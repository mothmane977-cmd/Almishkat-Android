# Integration decisions and gating

## Adopt as external guidance, not source-code dependency
- `android/skills`, `Kotlin/kotlin-agent-skills`, `GuillemRoca/agent-skills-android`, `siifii/android-agent-engineering` — selectively consult skills and adapt original project-specific procedures; inspect upstream changes and licenses.
- `android/nowinandroid`, `android/architecture-samples` — architecture and test references, no mass migration.

## Benchmark candidates (not yet installed)
- `androidx/media`: compare Media3 Transformer vs current MediaCodec/MediaMuxer on identical 9:16 scene; test Arabic glyphs, word timing, audio/video drift, codecs, API26/34/36, memory, duration and file size.
- `airbnb/lottie-android`: UI animation proof of concept; export parity requires separate implementation.
- `square/leakcanary`: debug-only, measure overhead.
- `arthenica/ffmpeg-kit-next`: only if Media3/current engine insufficient; local NDK build, ABIs, LGPL/GPL and codec license audit.

## Quran data and timing
- `dotquran/corpus`, `risan/quran-json`, `Abdelrhman1326/Quran-DB`: compare text against pinned Hafs source with exact Unicode checks; data licenses distinct from code licenses.
- `Mushaf-Learning/quran-audio`: metadata only until recordings' distribution rights verified.
- `cpfair/quran-align`: timing research only; 2017 code, validate against exact recording and manual reference.

## Avoid direct adoption
- React Native packages: different runtime than native Compose; no justification for dual UI stack.
- `realm/realm-kotlin`: no evidence that replacing SQLite v11 solves a real defect.
- `zonetecde/QuranCaption`: CC BY-NC 4.0 source; no commercial code import without authorization.
- `remotion-dev/remotion`: special license and web/backend renderer; not a native Android export replacement.
- Repositories with UNKNOWN license: reference only, do not copy code.

## Gates before implementation
1. Pin exact upstream release/commit and verify complete LICENSE and transitive licenses.
2. Establish current APK build and device baseline; verify Gradle wrapper availability and SDK.
3. Implement on isolated branch with rollback plan.
4. Run Quran exact-text integrity tests and Arabic rendering screenshot/video golden tests.
5. Run unit, instrumentation, export, performance and security tests.
6. Record PASS/FAIL/NOT RUN, date, device and logs in integration PR.
