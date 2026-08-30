# Project state

Updated: `2026-08-30 17:15 UTC`

## Target

- Full planned outcome: update the maintained Y700 branch to current upstream, assess all open upstream PRs for concrete Lenovo Legion Y700 Gen 3 value, integrate only justified compatible improvements, and deliver a Legacy+Modern-tested, isolated, signed Modern APK without regressing paired Joy-Con behavior.
- Implementation authority: `complete-target` for local/fork branch integration and verification described in `plans/active_plan.md`.
- Explicitly unauthorized: pushing to upstream, publishing a release, merging/closing/commenting on external PRs, changing credentials/scopes, destructive history rewrites, or weakening package/signature/isolation controls.

## Current truth

- Functional version: `0.1.0` — paired Joy-Con Y700 isolated build.
- Status: `functional` for the existing baseline; `0.2.0` passed same-head remote verification and remains pending Y700 hardware observation.
- Branch: `y700/stable`; resolve the exact candidate with `git rev-parse HEAD` rather than copying a stale SHA into this mutable file.
- Upstream/fork status: `origin/master` at `c76d3ddc6dec902cb55c4d2d720718f4db03e492` is an ancestor of the branch through merge commit `e99e4105`.
- PR evaluation: all 60 open upstream PRs were classified in `outputs/upstream-pr-evaluation.md`. Only #1599 was selected now; its focused Epic tablet-overhead improvements were adapted in `a5491ddf` without replacing newer resume/external-storage behavior.
- Completed: paired Joy-Con logical-controller support; claim/reconnect/player-slot regression fixes; isolated package `app.gamenative.joycontest`; app-private `evshim`; stable-signing workflow; upstream synchronization; complete open-PR inventory; selected Epic optimization; portable project-agent governance and validation contract; code-grounded Markdown documentation under `docs/`.
- CI contract: workflow dispatch now runs the native bridge gate and Legacy+Modern tests before building, signing, verifying, and uploading the isolated Modern APK (`9e826881`).
- Blockers: local runtime currently has no Java executable and no `JAVA_HOME`/`ANDROID_SDK_ROOT`; local Gradle validation cannot run until the Android toolchain is available. GitHub CI remains the proven build environment.
- Material limitations: hardware verification remains from the prior Joy-Con baseline, not this upstream/PR candidate. The Y700 thermal improvement claimed by #1599 still needs a real device measurement.

## Next action

- Action: install the latest same-head artifact on the Lenovo Legion Y700 Gen 3 and run the manual controller/touch/game-launch gate.
- Depends on: device access and the successful GitHub Actions artifact.
- Write surface: hardware observations and generated `outputs/ci/` evidence only; no upstream mutation or release publication.
- Acceptance evidence: manual observations declared in `.agents/validation.md`, tied to the tested build SHA.

## Validation

- Contract: `.agents/validation.md`.
- Previous behavioral result: GitHub Actions run `33312569000` completed successfully for baseline head `5565d60a`; it built, signed, verified, and uploaded the isolated Modern APK.
- Previous full tests: GitHub Actions run `33308448387` completed the Legacy + Modern unit-test gate successfully for Joy-Con head `095d6f02`.
- Candidate evidence rule: use the latest successful `workflow_dispatch` run whose `headSha` exactly equals `git rev-parse HEAD`; older URLs are baseline evidence only.
- Latest successful delivery evidence before the documentation-only commit: GitHub Actions run `33317043206` passed the native bridge, Legacy+Modern unit tests, isolated Modern APK build, package/signature verification, and upload for head `32789ec4c8f8542f4a127d1c274f52928766b17e`.
- Deterministic report: `outputs/quality-report.json` currently reports `QUALITY_INCOMPLETE` solely because the source adapter cannot inspect upstream's prebuilt ARM64 `libredirect-bionic-wx-minimal.so`; the file matches the exact `origin/master` Git blob and has SHA-256 `557aacc4a8b9e0dccb5e6d43aecca7b1723c0095f9bcb24fc5efe0db5390ff41`.
- Skipped applicable checks: local Gradle tests/build, because Java and Android SDK variables are absent in this runtime. Remote CI is mandatory for the candidate.

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
