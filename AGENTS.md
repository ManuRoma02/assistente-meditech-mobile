<!-- ota-generated-agent-guidance:start -->
# AGENTS.md

Generated from `./ota.yaml` by `ota agents`.

## Repo

- `project`: `Assistente MeDiTech Mobile`

## Agent Contract

Use only declared `ota run <task>` paths. If the contract does not model the work you need, stop and request a contract update; do not bypass the agent boundary with raw package-manager, compiler, or test commands.

- `safe_tasks`:
  - `setup` (`ota run setup`)
  - `test` (`ota run test`)
  - `lint` (`ota run lint`)
- `safe_task_risk_signals`:
  - `setup`: writes=.gradle; network=dependency_hydration
- `writable_paths`: `app`, `.gradle`, `build`
- `protected_paths`: `ota.yaml`, `settings.gradle.kts`, `gradle`
<!-- ota-generated-agent-guidance:end -->
