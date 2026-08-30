# Decision 0001: Y700 branch and release contract

Status: accepted
Date: 2026-08-30

## Context

The fork has two different responsibilities: a narrow Joy-Con change suitable for upstream review and a maintained Lenovo Legion Y700 Gen 3 build that also owns isolated packaging, app-private native bridge behavior, stable signing, CI delivery, and device-specific integration.

## Decision

- Keep `fix/paired-joycon-input` narrow and upstream-oriented.
- Keep `y700/stable` as the integration and delivery branch.
- Treat `origin` as read-only upstream and `fork` as the writable fork.
- Preserve `app.gamenative.joycontest`, `-joycon-test`, app-private `evshim`, stable certificate continuity, and signature verification on `y700/stable`.
- Integrate upstream and external PR work through auditable commits; prefer targeted patches when broad merges carry unrelated changes.
- Require separate Legacy+Modern test and isolated APK/signature evidence for release candidates.
- Require explicit approval before public release or any action on external/upstream PRs.

## Consequences

The Y700 branch intentionally carries maintenance delta over upstream. Upstream synchronization can conflict with controller, build, and native-bridge work and must preserve branch invariants explicitly. The workflow-dispatch lane must run Legacy+Modern unit tests before building, verifying, and uploading the isolated APK so all release evidence belongs to the same head.

## Recovery

The known-good pre-refresh baseline is `5565d60a175acb775707bb41a19d6322f31d0b39`. Candidate integrations must remain revertible without rewriting the shared stable branch.
