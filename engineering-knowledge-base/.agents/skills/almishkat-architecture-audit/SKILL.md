---
name: almishkat-architecture-audit
description: Android architecture and Gradle for the ALMISHKAT native Android Quran video editor.
license: Project-authored guidance; see project LICENSE for redistribution terms.
---

# ALMISHKAT: Android architecture and Gradle

## When to invoke
Use when a task involves android architecture and gradle. Do not invoke unrelated skills.

## Verified project baseline
- Native Android Kotlin 2.2.20, AGP 8.10.1, minSdk 26, targetSdk 36.
- Jetpack Compose UI, app module `:app`; domain/data/ui/core directories are not necessarily independent Gradle modules.
- SQLite schema 11 and existing MediaCodec/MediaMuxer export engine.
- Hafs-only v1.0; protect Uthmani text integrity.

## Procedure
Inspect settings.gradle.kts, app/build.gradle.kts, module boundaries and existing dependency direction; document a minimal refactor plan, never auto-restructure.

## Evidence and quality gates
1. Read the exact current source files and build configuration before proposing changes.
2. Document repository URL, license and source revision for each external reference.
3. State whether findings are code-verified, documentation-based, or hypotheses.
4. Produce a minimal patch and regression tests; do not replace existing architecture without benchmark evidence.
5. Check Quran text integrity, recitation rights, security, accessibility, and output consistency where applicable.
6. If unable to run tests, mark them NOT RUN and provide reproducible commands.

## External references (NOT vendored or automatically installed)
- https://github.com/android/nowinandroid
- https://github.com/android/architecture-samples
- https://github.com/GuillemRoca/agent-skills-android

## Deliverables
- Findings with severity and file:line evidence.
- Proposed implementation and rollback plan.
- Tests executed, failures, and unresolved risks.
