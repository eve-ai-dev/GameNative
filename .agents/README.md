# Portable agent layer

This directory is the runtime-neutral source of truth for project validation, quality scope, and skill routing.

- `validation.md`: executable command authority and evidence contract.
- `quality.yaml`: deterministic changed-code policy; it never executes commands.
- `skill-routing.md`: recurring work lanes and the smallest relevant skill set.
- `schemas/quality-policy-v1.schema.json`: vendored policy schema.

Runtime-specific adapters must point here rather than duplicating these rules.
