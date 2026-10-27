# Regeneration Rules — Phoenix Architecture

This document defines how services and agents in the Task Manager API regenerate themselves to ensure resilience, immutability, and continuous evolution.

---

## Core Regeneration Principles

1. **Source of Truth**
   - All services and agents must be rebuilt from GitHub repository commits.
   - No manual patching; regeneration always starts from version-controlled code.

2. **Automated Pipelines**
   - CI/CD pipelines (GitHub Actions) trigger rebuilds on:
     - Code changes (push/merge to main branch).
     - Configuration changes (Helm charts, manifests).
     - Agent prompt updates (`agents/*.md`).

3. **Immutable Deployments**
   - Each regeneration produces a new container image.
   - Old containers are replaced; no in-place modifications.

4. **Agent Prompt Regeneration**
   - Agents (Planner, Researcher, Coder, Reviewer, Orchestrator) regenerate their workflows when:
     - Domain requirements change.
     - New lifecycle stage rules are added in `ADLC.md`.
     - Phoenix architecture principles evolve.

5. **Self-Healing Integration**
   - Kubernetes + Istio restart failing pods automatically.
   - Regeneration rules ensure redeployment after repeated failures.

6. **Observability Feedback Loop**
   - Monitoring dashboards (Prometheus + Grafana) detect anomalies.
   - Alerts trigger regeneration pipelines when thresholds are exceeded.

---

## Regeneration Workflow

1. **Detect Change**
   - GitHub commit or monitoring alert identifies need for regeneration.

2. **Trigger Pipeline**
   - CI/CD rebuilds container images and redeploys via Helm/Kubernetes.

3. **Replace Service**
   - Old pods terminated, new pods deployed with updated configuration.

4. **Validate**
   - Health checks, integration tests, and BDD scenarios run automatically.

5. **Confirm**
   - Observability tools confirm stability before marking regeneration complete.

---

## Benefits
- Predictable deployments.
- Continuous evolution of agents and workflows.
- Strong alignment with ADLC and Phoenix principles.
- Reduced downtime and faster recovery from failures.
