# ADM-M1-012 — Android SQLiteOpenHelper RED tests (evidence-only)

**Stage:** M1; **official task:** M1-FIX-01 (D01). **Preparatory task:** M1-TST-01 / Issue #17 / PR #18. Date: 2026-11-11.

**Branch:** `test/M1-FIX-01-android-red` from `develop`. Scope: test-only preparation, **not a production fix**.

## Source identity and guardrails

- Production `app/src/main/java/com/almishkat/studio/QuranLocalDatabase.kt` remains unchanged. Git blob: `497c62c571d54cd0ed1dbe8a7ca074473c63bd20`.
- The production helper targets `almishkat_quran.db` version **10**. It currently executes `createAudioClipTable` for `oldVersion < 4` then `migrateAudioClipLinks` for `oldVersion < 5`. This duplicates `project_id` on v1–v3.
- v1–v3 fixtures in `app/src/androidTest/assets/db/` are explicitly **RECONSTRUCTED**; no authenticated v1–v3 SQL recovered.
- v4/v5 audio schemas are generated test-only scenarios from documented v3→v4 and v4→v5 history; **NOT authentic historical schema fixtures**.
- Strings `TEST_ONLY_NOT_QURAN` are synthetic and **not real Quranic verses**. Each test runs on a new database within a unique test-only cache subfolder. No user database is touched.

## What was added on this branch

1. `app/src/androidTest/java/com/almishkat/studio/D01MigrationInstrumentedTest.kt` — **16** JUnit4 Android instrumentation tests invoking actual, unmodified `QuranLocalDatabase`.
2. `app/src/androidTest/assets/db/v1.sql`, `v2.sql`, `v3.sql` — reconstructed SQL with `PRAGMA user_version`.
3. `app/build.gradle.kts` — **only** adds Android test runner and `androidTestImplementation` dependencies. No production source code changed.
4. `.github/workflows/m1-d01-android-red.yml` — separate branch-only `push` workflow, two lanes API 26 and API 36, saves Gradle log, device metadata, logcat, and Android JUnit reports where available.

## Coverage and intended assertions

| Cases | Purpose | Expected on baseline |
| --- | --- | --- |
| DB-001 / DB-002 / DB-003 | Full Android SQLiteOpenHelper upgrade from v1/2/3; original rows and D01 shape | **RED** because duplicate project_id |
| v4 standard + reopen twice | Add four columns, preserve clip and other rows, no second migration | May pass; full old schema unverified |
| v4 partial | Present partial link columns; must be supported or safely rejected by approved policy | **RED** on current duplicate |
| v4 wrong type / NOT NULL / wrong index / missing table | Reject incompatibilities atomically with explanatory error | Not assumed PASS before device run |
| v5 standard + reopen twice | No re-add; preserve all pre-existing values and correct D01 index | Expected pass, pending runtime |
| v5 partial / type / index / missing table | Detect incompatible/missing schema rather than silently normalize | Not assumed PASS before device run |
| Test-only injected failure | Verify SQLiteOpenHelper transaction rollback, original schema and rows retained | Separate helper test; **not** proof of production D01 correction |
| Baseline duplicate rollback | Verify v1 failure leaves original version, SQL and rows unchanged | Expected positive harness diagnostic |

The Android test compares pre-/post-upgrade row values, `PRAGMA user_version`, column types, NOT NULL / DEFAULT / PK properties, `idx_audio_clip_project`, `integrity_check`, `foreign_key_check`, and two no-op reopens on standard v4/v5 fixtures.

**Do not reclassify DB-001/002/003 as PASS merely because the failure is reproduced.** Their acceptance criteria still fail on the baseline. Also do not claim that v4/v5 synthetic scenarios prove historical compatibility.

## Execution requirements and reporting

- Gradle wrapper `gradlew` was **not found** on `develop`; the isolated workflow uses Gradle 8.14.3 via setup-gradle (AGP 8.10.1, Java 17).
- Command with a running emulator and Gradle 8.14.3: `gradle :app:connectedDebugAndroidTest --no-daemon --stacktrace`.
- **API 26 / API 36 actual results:** **NOT VERIFIED** until GitHub Actions provides uploaded JUnit XML with the device API and logcat. A workflow definition or pending run is NOT a test result.
- Any build/runner/setup failure is **BLOCKED**, not D01 RED and not PASS. Review individual JUnit failures, Gradle exit code, run-id and emulator device fingerprint before reporting outcomes.
- The earlier Python/SQLite 3.46.1 local tests reproduce `duplicate column name: project_id` for reconstructed v1–v3 and preserve rollback; they are **not Android tests**.
- API 26 and API 36 instrumentation concerns this M1 task only; it does not implement or accept M2 CI gates.

## Approval status and blockers

- D01 production code: **UNMODIFIED / UNFIXED**.
- True historical v1–v10 complete fixtures: **UNAVAILABLE**; original v4/v5 DDL still unverified.
- Issue #17 and PR #18: **NOT CLOSED / NOT MERGED**.
- No claim of official DB-001..003 acceptance, all-ten migration coverage, CI required check, or M1 closure.
- Separate D02 background risk not modified.

**Next action:** independent technical audit of test code and actual Android run logs; resolve any harness/build blockers on the isolated branch. Production remediation requires later approval and its own reviewable diff.
