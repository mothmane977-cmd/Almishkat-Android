# TEST ONLY — ADM-M1-133 / develop PR gate verification

**DO NOT MERGE.** This is an intentionally non-production, documentation-only trigger for a Draft pull request targeting `develop`.

Purpose: verify that the already-merged `.github/workflows/db-tests.yml` runs its `db-tests` job on a new `pull_request` event targeting `develop`.

- Authorization: ADM-M1-133, dated 2026-11-13.
- Baseline `develop` commit: `e7cee403036fa3e1f24afb0594b86384aa237f7c`.
- Scope: add **this file only** under the existing `docs/` directory.
- No code, fixtures, tests, build configuration, Quranic assets, or workflow changes are requested.
- The PR remains **Draft**, and must not be merged, closed, or deleted without separate owner authorization.
- Workflow check results, JUnit XML, artifacts, and actual event provenance must be verified separately. This file is not acceptance evidence by itself.
- This test does not verify SQLite behavior on `main` or assert that reconstructed historical fixtures are original.
