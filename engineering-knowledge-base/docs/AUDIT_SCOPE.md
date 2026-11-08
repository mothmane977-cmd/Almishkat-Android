# Audit scope and confidence

All 38 repository names were queried via GitHub repository metadata endpoints on 2026-11-07. 37 returned structured metadata, one (`Shopify/react-native-skia`) returned a redirect and remains unresolved. This is a **metadata census and targeted documentation review**, not a full forensic source-code audit of 38 projects.

For selected repositories, README and licensing were inspected (including FFmpegKitNext, Remotion, QuranCaption, Quran data, direct Quran video editors). For three agent-skill repositories, Git trees were enumerated and SKILL.md locations verified. This package's 15 SKILL.md files are original synthesized guidance, **not copies** of those upstream files.

No Android APK compilation, device testing, full transitive dependency graph analysis, CVE scan, source-line security audit, or verification of third-party Quran audio redistribution rights was performed. Stars are ephemeral snapshots and should not be used as code-quality scores.

Known license ambiguities: `alhaq-initiative/Quranic-Reels-Generator` README MIT badge vs GitHub Apache-2.0 metadata; `zonetecde/QuranCaption` LICENCE CC BY-NC 4.0 vs GitHub API NOASSERTION; `remotion-dev/remotion` custom; all UNKNOWN must be independently checked before reuse.
