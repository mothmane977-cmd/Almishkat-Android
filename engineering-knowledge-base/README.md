# ALMISHKAT Engineering Knowledge Base

**Version:** 0.1.0 — **Baseline:** 2026-11-07

This is a **portable, searchable, updateable project knowledge base** for ALMISHKAT (native Android Quran Reels Maker). It is not a model-memory installation and does not automatically install dependencies or upstream agent skills.

## Inventory
- 38 original candidate GitHub repositories in `registry/repositories.json` and `.csv`.
- 15 **original ALMISHKAT-specific** `SKILL.md` guides under `.agents/skills/`.
- Local project architecture snapshot and integration decisions in `docs/`.
- Offline full-text keyword search and optional GitHub metadata refresh scripts.

## Use with an AI assistant
Attach this folder to the ALMISHKAT project or commit it to its repository. Ask the assistant to read `README.md`, `registry/repositories.json`, and the relevant `.agents/skills/*/SKILL.md` **before modifying code**. Some assistants auto-discover `.agents/skills/`; others require explicit reference.

## Search
```bash
python3 scripts/search_kb.py media3
python3 scripts/search_kb.py quran
python3 scripts/search_kb.py security
```

## Refresh (network required)
```bash
python3 scripts/refresh_github.py
```
This updates metadata only; **never auto-approves licenses or code reuse**. Review all changes and re-export CSV if desired.

## Evidence limitations
Repository metadata was checked via GitHub API on 2026-11-07 for 37 repositories; Shopify/react-native-skia redirected and was not resolved. Selected README and license documents were reviewed; **not all 38 repositories underwent line-by-line code review, build, tests, or dependency CVE audit**. The original application ZIP was inspected locally but Android builds were not run.

## License rules
Upstream code is **not copied** into this package. This package contains original guidance, metadata, and links. Check each upstream license, commit SHA, assets and data rights before any code import. QuranCaption CC BY-NC 4.0: reference only for commercial use without permission. Remotion has a custom license. Quran text and recitation assets have distinct rights.
