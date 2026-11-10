# M1-TST-01 — Historical SQLite fixture provenance (work in progress)

Date: 2026-11-10. Base branch: `develop`. Tracking: #17.

## Verified repository evidence

- `app/src/main/java/com/almishkat/studio/QuranLocalDatabase.kt` declares `SQLiteOpenHelper(..., "almishkat_quran.db", null, 10)`; its source blob SHA on `develop` is `497c62c571d54cd0ed1dbe8a7ca074473c63bd20`.
- Current-path commit history returns only the M0 reorganization commit `44c070edc541f36f5d4029e97572c4932ab60075`. This **does not prove** historical schemas are absent; earlier versions may live in previous paths or archived ZIPs.
- The M0 tree contains archived source snapshots at `archive/source-snapshots/`, including `Almishkat-V23-SQLite-Fix.zip`, `Almishkat-V24-Export-Button-Source.zip` and `Almishkat-V25-Quran-Scroll-Fix-Source.zip`. ZIP filenames do **not** establish SQLite schema versions.
- `docs/PROJECT_V12_PERSISTED_CLIPS_AR.md` describes SQLite v3→v4; `docs/PROJECT_V13_AYAH_LINKING_AR.md` describes v4→v5. These documents are **secondary descriptions**, not schema fixtures.
- Source inspection identifies a potential v1–v3 duplicate-column path: `onUpgrade` invokes `createAudioClipTable` (already includes link columns) and then `migrateAudioClipLinks` (`ALTER TABLE` adds those columns). This must be reproduced with actual historical fixtures before a fix is certified.
- Source inspection also finds no `createStudioScenesTable` call in `onUpgrade`. This needs an upgrade regression test.

## Fixture evidence register

| SQLite version | Original schema/commit SHA | Fixture status |
| --- | --- | --- |
| v1 | Not yet recovered | Pending |
| v2 | Not yet recovered | Pending |
| v3 | Not yet recovered | Pending |
| v4 | Not yet recovered | Pending |
| v5 | Not yet recovered | Pending |
| v6 | Not yet recovered | Pending |
| v7 | Not yet recovered | Pending |
| v8 | Not yet recovered | Pending |
| v9 | Not yet recovered | Pending |
| v10 | Current source exists; SQL fixture not yet generated | Pending |

## Mandatory acceptance protocol

1. Trace earlier paths and inspect the archived ZIPs; record exact source commit SHA or archive hash for each recovered schema.
2. Reconstruct only unrecoverable schemas; mark `reconstructed` inside each SQL fixture and in this register. Never label reconstructions original.
3. Add `app/src/test/resources/db/v1.sql` … `v10.sql` and a loader test that checks `PRAGMA user_version`.
4. Run baseline migrations and capture red test results **before** implementing D1 fixes. Preserve Quran data, projects, timing, audio clips, scenes and layers.
5. Add CI `db-tests` and verify protected branch check; do not claim successful migration until 10/10 passes on an Android-compatible SQLite implementation.

**Status:** evidence collection only. No fixture or passing test is claimed. No database data or schema has been changed.
