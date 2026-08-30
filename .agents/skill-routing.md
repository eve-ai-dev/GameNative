# Project skill routing

Select one execution-primary skill per lane and normally no more than two supporting skills.

## Repository governance and continuity

- Primary: `agent-managed-projects`
- Support: `writing-plans`, `builder-assurance`
- Use for: `AGENTS.md`, `STATE.md`, plans, decisions, validation, quality evidence, or runtime adapters.

## Upstream and external PR assessment

- Primary: `github-code-review`
- Support: `token-economy`, `codebase-inspection`
- Use for: PR metadata/diffs/checks, compatibility and overlap analysis, and evidence-backed integrate/skip decisions. Reading only unless external action is explicitly approved.

## Y700/Joy-Con implementation

- Primary: `test-driven-development`
- Support: `systematic-debugging`, `token-economy`
- Use for: controller behavior, paired ownership, reconnect/slot state, app-private `evshim`, package isolation, and conflict resolution.

## Build, artifact, and CI delivery

- Primary: `github-pr-workflow`
- Support: `builder-assurance`, `systematic-debugging`
- Use for: branch CI, isolated APK build/signature evidence, artifact retrieval, and CI failure diagnosis. Publication/merge remains separately approval-gated.

Missing skills or unavailable tools are explicit blockers; do not silently replace required evidence with an easier but weaker lane.
