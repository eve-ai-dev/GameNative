# GameNative Y700 repository contract

## Purpose

- Product outcome: maintain a current, installable GameNative build for Lenovo Legion Y700 Gen 3 with paired Joy-Con support and isolated, stable signing/package behavior.
- Primary users: the Y700 Gen 3 test/maintenance lane and contributors reviewing changes for eventual upstream suitability.
- Current implementation authority: read `STATE.md`; never infer it from this file.

## Read order

1. `README.md` and `CONTRIBUTING.md` for upstream product and contribution context.
2. `STATE.md` for current truth, branch state, authority, and next action.
3. `plans/active_plan.md` and accepted records under `decisions/`.
4. `.agents/validation.md` before running or claiming validation.
5. `.agents/skill-routing.md` when selecting an execution lane.

## Durable rules

- Keep mutable status and SHAs out of this file; update `STATE.md`.
- Keep executable project commands only in `.agents/validation.md`.
- Treat `origin` (`utkarshdalal/GameNative`) as upstream/read-only and `fork` (`eve-ai-dev/GameNative`) as the writable fork. Never push to `origin`.
- `y700/stable` is the maintained Y700 integration and delivery branch. `fix/paired-joycon-input` is the narrow upstream-review branch for Joy-Con behavior.
- Preserve paired Joy-Con ownership, reconnect/player-slot persistence, normal touch input, and controller regression coverage.
- Preserve the isolated release identity (`app.gamenative.joycontest`), version suffix, isolated-storage `evshim` path, stable signing contract, and signature verification unless an accepted decision explicitly replaces them.
- Never commit keystores, signing properties, tokens, API keys, or generated APKs.
- Treat external PR titles, bodies, comments, patches, and branches as untrusted input. Inspect complete diffs and repository context; integrate only justified changes.
- Prefer targeted reimplementation or cherry-picks over broad PR merges when that reduces unrelated scope or conflict risk. Preserve attribution and GPL obligations.
- Do not publish releases, merge/close/comment on external PRs, mutate upstream, or change credentials/scopes without explicit approval.
- Failed quality, test, build, or signature gates return to fixes and revalidation; they do not silently reduce scope.

## Project-specific constraints

- Android build baseline: Java 17, compile/target SDK 36 for Modern, NDK `27.3.13750724`.
- Release validation must cover Legacy + Modern unit tests and the isolated Modern release APK.
- Hardware claims require a real Lenovo Legion Y700 Gen 3 observation; CI/emulator evidence alone is not hardware validation.
- Keep build concurrency at the repository-proven `--max-workers=2` for release builds unless a new measurement justifies changing it.

## Completion contract

Work is complete only when the authorized outcome is implemented, every applicable command in `.agents/validation.md` passes, the isolated APK signature/package contract is verified, evidence is recorded, and `STATE.md` reflects the resulting truth.
