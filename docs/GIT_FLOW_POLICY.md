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
- Require review according to the approved review policy; do not represent a missing independent review as completed.
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
6. Obtain independent review when the qualified reviewer and an enforceable approval workflow are available. Until then, **independent-review compliance remains open** and must not be reported as satisfied.
7. Reject secrets, unlicensed assets, unauthorized scope changes, and unexplained data migrations.
8. Do not merge solely because a check passed: required human approval and project release decisions are separate gates.

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

**Known gap:** required approving reviews are currently set to **0**. A qualified independent reviewer and an enforceable review policy must be arranged and verified before this acceptance criterion can be closed. Avoid prematurely requiring an unavailable reviewer and deadlocking development.

**CI limitation:** the build workflow's `pull_request` trigger was added on `feature/M0-FOUND-01-structure` for PRs targeting `main` and `develop`. A successful PR check was observed; confirm that the relevant workflow is present and reliably runs on each protected target branch before treating enforcement as comprehensively validated. The workflow's concurrency configuration also warrants review.

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
- Independent review requirements are resolved and enforced or explicitly recorded as an unmet acceptance criterion.
- Branch protections and merge routes are tested with auditable evidence.
- No direct modifications to `main` are performed during setup.

**Status:** Policy approved and recorded; **M0-FOUND-02 remains open** pending the above evidence.
