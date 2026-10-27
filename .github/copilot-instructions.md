# Project-wide documentation workflow

After making project changes, use the workspace **README Maintainer** agent
before completing the task whenever code, API behavior, dependencies,
configuration, build or run instructions, tests, security, CI/CD, containers,
or deployment changes could affect developer or user documentation. Provide
the agent with the change summary and changed files so it can update the root
`README.md` based on the actual implementation.

The README review is part of the change, not a separate follow-up. Keep it
focused: internal changes with no meaningful README impact do not require a
README edit. Do not document planned behavior as implemented or include
secrets. The README Maintainer must limit its edits to `README.md`.
