# Y700 0.2.0 upstream refresh and curated integration plan

**Goal:** produce an upstream-current Lenovo Legion Y700 Gen 3 branch that preserves paired Joy-Con behavior and the isolated stable APK contract, while integrating only open-PR improvements with clear device value and acceptable risk.

**Approach:** preserve `y700/stable` as the integration/delivery lane; synchronize upstream first, then evaluate all open PRs against the synchronized tree, integrate the smallest justified patches, and validate tests/build/signature/hardware as distinct gates.

**Tech/context:** Android/Kotlin/Java/C/C++, Gradle, Legacy and Modern flavors, NDK `27.3.13750724`, SDL-backed app-private `evshim`, GitHub Actions, Lenovo Legion Y700 Gen 3.

**Execution graph:** `W0 governance -> W1 upstream sync -> W2 PR inventory/triage -> W3 selected integration -> W4 tests + isolated build/signature -> W5 hardware gate -> W6 handoff`

**Verification strategy:** `.agents/validation.md` is the only command authority. Every selected PR needs diff/check/context inspection plus targeted evidence; final code needs Legacy+Modern tests, deterministic scope report, isolated Modern APK build/signature/package checks, and device verification for hardware claims.

**Residual uncertainty:** upstream changed by two commits after the Joy-Con baseline; 60 open PRs vary widely in age, scope, and CI quality; this runtime currently lacks Java/Android SDK, so local Gradle execution may require an alternate approved environment or same-head CI.

## Functional versions

### 0.1.0 — Paired Joy-Con isolated Y700 build

- Promise: two complementary Joy-Cons operate as one logical controller in an independently installable GameNative build.
- Supported workflows: discovery/pairing, input, touch coexistence, reconnect, player-slot persistence, isolated Modern APK installation.
- Explicit exclusions: current upstream changes and unrelated open PRs.
- Authority boundaries: no public release or upstream mutation.
- Acceptance evidence: prior hardware observations; CI runs `33308448387` and `33312569000`.
- Upgrade path: merge current upstream, resolve conflicts preserving the branch contract, then add only accepted PR patches.
- Rollback/recovery: retain `5565d60a` as the known-good baseline and revert integration commits without rewriting it.
- Status: `functional`.

### 0.2.0 — Current, curated Y700 build

- Promise: current upstream GameNative plus evidence-backed Y700 improvements, with no Joy-Con, packaging, signing, or isolation regression.
- Supported workflows: all 0.1.0 workflows plus improvements explicitly accepted in the PR decision record.
- Explicit exclusions: cosmetic/general features without Y700 value, risky overlapping controller rewrites without stronger evidence, public release, and external PR administration.
- Authority boundaries: fork integration/CI allowed; upstream and publication actions require approval.
- Acceptance evidence: same-head Legacy+Modern tests, selected-PR targeted checks, isolated signed APK verification, final diff review, and Y700 hardware journey.
- Upgrade path: maintain explicit upstream merge points and one auditable integration commit per selected logical change.
- Rollback/recovery: revert the selected integration commit or reset a disposable recovery branch to the last green merge point; never rewrite the shared stable branch without approval.
- Status: `building`.

## Work packages

### W0 — Retrofit project-agent governance

- Objective: make repo truth, authority, commands, decisions, and recovery discoverable without chat history.
- Depends on: none.
- Mode: sequential.
- Write surface: `AGENTS.md`, `STATE.md`, `.agents/`, `plans/`, `decisions/`, `outputs/`.
- Produces: portable project contract and validated quality policy.
- Acceptance evidence: policy schema validation, quality report, `git diff --check`, and placeholder/private-path scan.
- Rollback/recovery: one documentation/governance commit can be reverted independently.

### W1 — Synchronize current upstream

- Objective: integrate `origin/master` into `y700/stable` without losing branch-specific controls.
- Depends on: W0.
- Mode: sequential.
- Write surface: files changed by the two upstream commits plus conflict resolutions.
- Produces: auditable upstream merge point and preserved Y700 invariants.
- Acceptance evidence: ancestry/divergence checks, reviewed merge diff, targeted affected tests, then required full gates.
- Rollback/recovery: abort conflicted merge before commit or revert the completed merge commit.

### W2 — Inventory and prioritize all open upstream PRs

- Objective: classify every open PR as integrate, defer, or reject for this branch.
- Depends on: W1 synchronized base.
- Mode: parallel read-only research where write surfaces do not overlap.
- Write surface: `outputs/upstream-pr-evaluation.md` only.
- Produces: ranked decision record with value, compatibility, overlap, risk, evidence, and exact rationale.
- Acceptance evidence: metadata, changed-file list, full patch/context, CI status, and comparison against Y700/Joy-Con branch changes for every shortlist candidate.
- Rollback/recovery: report-only; no external PR mutations.

### W3 — Integrate selected improvements

- Objective: apply only changes whose benefit exceeds compatibility and maintenance cost.
- Depends on: W2 decisions.
- Mode: sequential per overlapping subsystem; independent non-overlapping candidates may form a review wave.
- Write surface: candidate-specific product/test files; preserve one logical commit per accepted change when practical.
- Produces: integrated branch plus regression evidence.
- Acceptance evidence: selected-PR targeted tests/reproductions, conflict review, `git diff --check`, and no forbidden package/signing/isolation drift.
- Rollback/recovery: revert candidate commit independently.

### W4 — Validate and build isolated APK

- Objective: prove the integrated code and artifact, not merely compile a plausible branch.
- Depends on: W3.
- Mode: sequential evidence gate.
- Write surface: generated build outputs and `outputs/` evidence only.
- Produces: same-head Legacy+Modern result, isolated Modern APK, signature/package/hash evidence, and CI/artifact URLs.
- Acceptance evidence: all applicable gates in `.agents/validation.md`.
- Rollback/recovery: no publication; failed gates loop to W3 fixes.

### W5 — Y700 hardware gate

- Objective: verify controller and device behavior on Lenovo Legion Y700 Gen 3.
- Depends on: W4 installable artifact.
- Mode: manual device gate.
- Write surface: `outputs/y700-hardware-validation.md`.
- Produces: build-SHA-bound observations.
- Acceptance evidence: paired Joy-Con input, touch coexistence, reconnect, player-slot persistence, and affected selected-PR journeys.
- Rollback/recovery: uninstall candidate/reinstall prior isolated APK; preserve app identity/signing continuity.

### W6 — Handoff

- Objective: deliver exact changes, per-PR decisions, residual risks, CI links, and artifact link without unauthorized external actions.
- Depends on: W4 and W5, or an explicit statement that hardware verification remains pending.
- Mode: sequential.
- Write surface: `STATE.md` and final decision/evidence outputs.
- Produces: resumable final state and concise user report.
- Acceptance evidence: branch/remote status, current head SHA, clean worktree, live CI/artifact URLs, and explicit skipped checks.

## Decision criteria for upstream PRs

Prioritize concrete Y700 value: controller/Joy-Con correctness, Vulkan/display/resolution behavior, thermal/performance efficiency, storage/package-path correctness, stability, and install/runtime compatibility. Deprioritize broad UI, store, VR/XR, account, cosmetic, or unrelated content features unless they fix a reproduced Y700 problem.

Reject or defer when a PR is stale against current upstream, duplicates upstream, overlaps Joy-Con ownership/slot logic without stronger tests, weakens app-private storage or suffixed-package compatibility, changes signing/release identity, lacks a falsifiable benefit, or adds disproportionate maintenance surface.

## Acceptance matrix

- Upstream current: `origin/master` is an ancestor of final `y700/stable`.
- Joy-Con: targeted regression plus Legacy+Modern suites pass.
- Package isolation: `app.gamenative.joycontest` and `-joycon-test` remain intact.
- Native bridge: app-private `evshim` builds and exports the expected JNI symbol.
- Artifact: Modern release APK exists, stable signature verifies, SHA-256 recorded, CI head matches branch head.
- External safety: no upstream push, release publication, or external PR mutation occurred.
- Continuity: `STATE.md`, PR evaluation, decisions, and evidence match final reality.
