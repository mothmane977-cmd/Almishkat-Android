---
name: almishkat-audio-engineering
description: Audio decoding and waveform for the ALMISHKAT native Android Quran video editor.
license: Project-authored guidance; see project LICENSE for redistribution terms.
---

# ALMISHKAT: Audio decoding and waveform

## When to invoke
Use when a task involves audio decoding and waveform. Do not invoke unrelated skills.

## Verified project baseline
- Native Android Kotlin 2.2.20, AGP 8.10.1, minSdk 26, targetSdk 36.
- Jetpack Compose UI, app module `:app`; domain/data/ui/core directories are not necessarily independent Gradle modules.
- SQLite schema 11 and existing MediaCodec/MediaMuxer export engine.
- Hafs-only v1.0; protect Uthmani text integrity.

## Procedure
Inspect MediaExtractor/MediaCodec error paths, duration, sample rate, channel count, waveform memory and API 26 support; do not bundle audio without permission.

## Evidence and quality gates
1. Read the exact current source files and build configuration before proposing changes.
2. Document repository URL, license and source revision for each external reference.
3. State whether findings are code-verified, documentation-based, or hypotheses.
4. Produce a minimal patch and regression tests; do not replace existing architecture without benchmark evidence.
5. Check Quran text integrity, recitation rights, security, accessibility, and output consistency where applicable.
6. If unable to run tests, mark them NOT RUN and provide reproducible commands.

## External references (NOT vendored or automatically installed)
- https://github.com/androidx/media
- https://github.com/Mushaf-Learning/quran-audio

## Deliverables
- Findings with severity and file:line evidence.
- Proposed implementation and rollback plan.
- Tests executed, failures, and unresolved risks.
