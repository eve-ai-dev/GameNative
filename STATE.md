# Project state

Updated: `2026-08-30 13:34 UTC`

## Target

- Full planned outcome: update the maintained Y700 branch to current upstream, assess all open upstream PRs for concrete Lenovo Legion Y700 Gen 3 value, integrate only justified compatible improvements, and deliver a Legacy+Modern-tested, isolated, signed Modern APK without regressing paired Joy-Con behavior.
- Implementation authority: `complete-target` for local/fork branch integration and verification described in `plans/active_plan.md`.
- Explicitly unauthorized: pushing to upstream, publishing a release, merging/closing/commenting on external PRs, changing credentials/scopes, destructive history rewrites, or weakening package/signature/isolation controls.

## Current truth

- Functional version: `0.1.0` — paired Joy-Con Y700 isolated build.
- Status: `functional` for the existing baseline; `0.2.0` is building.
- Branch: `y700/stable` at `5565d60a175acb775707bb41a19d6322f31d0b39`, matching `fork/y700/stable` when last checked.
- Upstream/fork status: `origin/master` is two commits ahead of `fork/master`; `y700/stable` is two upstream commits behind and carries fifteen branch commits over the previous merge base.
- Current dependency wave: synchronize the two current upstream commits before PR integration.
- Completed: paired Joy-Con logical-controller support; claim/reconnect/player-slot regression fixes; isolated package `app.gamenative.joycontest`; app-private `evshim`; stable-signing workflow; full Legacy+Modern PR test gate; portable project-agent governance and validation contract.
- Blockers: local runtime currently has no Java executable and no `JAVA_HOME`/`ANDROID_SDK_ROOT`; local Gradle validation cannot run until the Android toolchain is available. GitHub CI remains the proven build environment.
- Material limitations: hardware verification is from the prior Joy-Con baseline, not any future upstream/PR integration; 60 upstream PRs were open at the start of this work and still require evidence-based triage.

## Next action

- Action: finish and verify this governance retrofit, then merge the two current upstream commits into `y700/stable` while preserving Y700-specific contracts.
- Depends on: project-agent files passing schema/diff checks.
- Write surface: `AGENTS.md`, `STATE.md`, `.agents/`, `plans/`, `decisions/`, `outputs/`; then upstream-touched product files under explicit review.
- Acceptance evidence: commands and observations declared in `.agents/validation.md` plus clean Git state/diff inspection.

## Validation

- Contract: `.agents/validation.md`.
- Latest behavioral result: GitHub Actions run `33312569000` completed successfully for head `5565d60a`; it built, signed, verified, and uploaded the isolated Modern APK.
- Latest full tests: GitHub Actions run `33308448387` completed the Legacy + Modern unit-test gate successfully for Joy-Con head `095d6f02`.
- Evidence artifacts: `https://github.com/eve-ai-dev/GameNative/actions/runs/33312569000`; `https://github.com/eve-ai-dev/GameNative/actions/runs/33308448387`; generated governance quality evidence under `outputs/`.
- Skipped applicable checks: local Gradle tests/build, because Java and Android SDK variables are absent in this runtime; no new product code is part of the governance retrofit.

## Authority gate

- Next gated action: any upstream-facing action or public release.
- Approval required because: those actions are externally visible and explicitly excluded from current authority.

## Runtime adapters

- Canonical portable layer: `.agents/`.
- Generated/native adapters: none.
- Unsupported mappings: none currently identified.

## Resume order

1. `AGENTS.md`
2. this file
3. `plans/active_plan.md`
4. `.agents/validation.md`
5. `decisions/0001-y700-branch-contract.md`
