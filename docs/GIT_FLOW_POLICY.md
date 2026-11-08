# ALMISHKAT STUDIO — Git Flow Policy

**Project:** Almishkat Studio (المشكاة ستوديو)  
**Repository:** `mothmane977-cmd/Almishkat-Android`  
**Document status:** Approved project policy; implementation and compliance verification remain in progress  
**Task:** M0-FOUND-02 — Git Flow and Branch Protection  
**Approved:** 2026-11-08

## 1. Binding principles

- Preserve existing source code, project data, and Git history.
- Do not push changes directly to protected branches.
- Develop changes on separate branches and submit Pull Requests (PRs).
- Never merge when mandatory checks fail or remain pending.
- Require a documented technical self-review and mandatory CI checks before merging. Independent human review is recommended when a qualified reviewer is available, but is not mandatory during solo development.
- Do not publish a release without explicit project-owner approval.
- Do not introduce features outside the approved roadmap or advance to M1 before M0 closure.
- Never commit secrets or unlicensed resources.

## 2. Branch roles

| Branch | Purpose |
| --- | --- |
| `main` | Approved stable release source |
| `develop` | Integration branch for reviewed changes |
| `feature/*` | Approved development tasks |
| `fix/*` | Defect fixes within the approved stage |
| `hotfix/*` | Urgent fixes for an existing stable release |
| `release/*` | Release preparation, testing, and approval |
| `backup/*` | Exceptional reference snapshots, not a normal merge route |

Branch creation does not authorize bypassing protections. Existing work branches are preserved.

## 3. Pull Request requirements

1. Work from a separate branch linked to a documented issue or approved task.
2. Explain the purpose, scope, risks, and relevant acceptance criteria.
3. Provide test evidence and document any unresolved limitations.
4. Require all configured status checks to pass.
5. Resolve review discussions before approval and merge.
6. During solo development, document a technical self-review of the diff, risks, test evidence, and acceptance criteria. Independent human review is recommended when a qualified reviewer is available, but is **not an acceptance criterion for M0-FOUND-02**.
7. Reject secrets, unlicensed assets, unauthorized scope changes, and unexplained data migrations.
8. Do not merge solely because a check passed: documented technical review and explicit project-owner authorization for merge or release remain separate gates.

## 4. Merge routes

**Normal development:** `feature/*` or `fix/*` → PR → `develop`.

**Release:** `develop` → `release/*` → tests and review → PR → `main`. Reconcile approved release changes back into `develop` without rewriting protected history.

**Urgent fix:** branch `hotfix/*` from the approved stable baseline → tests and review → PR → `main`; then propagate the fix to `develop` through a separate reviewed PR.

Do not merge or rebase protected branches directly. Prefer an auditable merge strategy compatible with repository settings.

## 5. Protected-branch baseline (verified 2026-11-08)

The active GitHub ruleset **Almishkat Protected Branches** targets `main` and `develop` and currently requires:

- Pull Request before merge.
- Successful `Clean Clone Verification and APK Build` status check from GitHub Actions.
- No branch deletions and no force pushes.
- No bypass actors in the ruleset.

**Approved solo-developer exception (2026-11-08):** the project owner is currently the sole developer and explicitly removed mandatory independent human review from M0-FOUND-02 acceptance criteria. Required approving reviews remain **0**. Documented self-review and mandatory CI checks remain required. Reconsider independent review when a qualified reviewer joins; do not describe self-review as independent review.

**CI verification (2026-11-08):** the build workflow's `pull_request` trigger is present on `feature/M0-FOUND-01-structure`; PR #1 and push builds succeeded after isolating concurrency groups by event and branch. The temporary negative-test PR #2 produced a failed required check and GitHub reported `blocked`, demonstrating enforcement on `develop`. The workflow is not yet part of the protected target branches' committed baseline; verify future PR execution after approved integration. Do not merge PR #2.

## 6. Releases and tags

- Create a release tag (for example `v1.0.0`) only after required tests, reviews, and explicit approval.
- Record the exact commit, dependencies, build environment, and release artifacts.
- The approved release must be reproducible from its Git tag.
- Store signing keys, credentials, and production secrets outside Git, using appropriately protected secret storage.
- Do not upload or publish an AAB to Google Play without project-owner approval.

## 7. Rollback and recovery

- Identify the change responsible for a regression.
- Prefer `git revert` and a reviewed corrective PR over rewriting protected history.
- Test the correction on an isolated branch.
- Document the reason, scope, outcome, and any necessary data recovery.
- Never delete historical evidence merely to conceal a defect.

## 8. Acceptance and closure evidence

M0-FOUND-02 may be closed only after:

- This policy is reviewed, saved, and linked from contribution documentation when that document is prepared.
- GitHub settings are verified against this policy.
- Required checks are shown to pass and to block an intentionally failing test PR without merging it.
- The project-owner-approved solo-developer review exception is documented; independent human review is optional for M0-FOUND-02, while documented technical self-review remains mandatory.
- Branch protections and merge routes are tested with auditable evidence.
- No direct modifications to `main` are performed during setup.

**Status:** Policy updated by explicit project-owner decision on 2026-11-08. **M0-FOUND-02 remains open** pending any remaining acceptance evidence and authorized integration; the independent human-review requirement is no longer a blocker.
