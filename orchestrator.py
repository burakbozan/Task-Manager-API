#!/usr/bin/env python3
"""Simple orchestration runner for the project templates in this repository."""

from __future__ import annotations

import argparse
from pathlib import Path
from typing import Dict, List


ROOT = Path(__file__).resolve().parent
PROMPTS_DIR = ROOT / "prompts"


PROJECTS: Dict[str, Dict[str, object]] = {
    "task-manager": {
        "title": "Task Manager API",
        "summary": "Spring Boot REST API for managing tasks with JWT auth and Swagger docs.",
        "tech": [
            "Spring Boot",
            "Spring Data JPA",
            "Spring Security",
            "JWT",
            "springdoc-openapi",
            "Docker",
            "Actuator",
        ],
        "workflow": [
            "Planner defines CRUD and auth tasks.",
            "Researcher recommends Spring Boot, JWT, OpenAPI, and Docker best practices.",
            "Coder implements task APIs, JWT filter, Swagger config, and container setup.",
            "Reviewer validates correctness, security, and performance.",
            "Orchestrator integrates the final solution.",
        ],
    },
    "chat-app": {
        "title": "Chat App",
        "summary": "Real-time messaging platform with auth, persistence, and Redis.",
        "tech": [
            "Node.js",
            "Express",
            "Socket.IO",
            "MongoDB",
            "Redis",
            "React",
        ],
        "workflow": [
            "Planner defines messaging and auth requirements.",
            "Researcher recommends Socket.IO, MongoDB, Redis, and React patterns.",
            "Coder implements the API, chat flows, and frontend UX.",
            "Reviewer checks real-time reliability and security.",
            "Orchestrator integrates the final system.",
        ],
    },
    "note-app": {
        "title": "Note App",
        "summary": "Collaborative notes platform with AI summarization and semantic search.",
        "tech": [
            "React",
            "Tailwind CSS",
            "Node.js",
            "Express",
            "PostgreSQL",
            "OpenAI API",
        ],
        "workflow": [
            "Planner defines note CRUD and collaboration tasks.",
            "Researcher recommends a React + Node stack with AI summarization and search.",
            "Coder implements the note APIs and frontend workflows.",
            "Reviewer checks correctness, AI quality, and security.",
            "Orchestrator integrates the final solution.",
        ],
    },
}


def _read_prompt(name: str) -> str:
    prompt_file = PROMPTS_DIR / f"{name}.md"
    if prompt_file.exists():
        return prompt_file.read_text(encoding="utf-8")
    return "Prompt file not found."


def _project_aliases() -> List[str]:
    return sorted(PROJECTS)


def _describe_project(project_name: str) -> str:
    project = PROJECTS.get(project_name)
    if not project:
        raise ValueError(f"Unsupported project: {project_name}")

    prompt_names = ["planner", "researcher", "coder", "reviewer", "orchestrator"]
    prompt_details = [
        f"- {name.title()}: {_read_prompt(name).splitlines()[0].strip() if _read_prompt(name).strip() else 'Prompt with no header'}"
        for name in prompt_names
    ]

    lines = [
        f"Project: {project['title']}",
        f"Alias: {project_name}",
        f"Summary: {project['summary']}",
        "Tech stack: " + ", ".join(project["tech"]),
        "",
        "Workflow:",
    ]
    for index, step in enumerate(project["workflow"], 1):
        lines.append(f"{index}. {step}")
    lines.extend(["", "Agent prompts available:"])
    lines.extend(prompt_details)
    return "\n".join(lines)


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(
        description="Identify a project and orchestrate the planning/research/coding/review workflow."
    )
    parser.add_argument(
        "project",
        nargs="?",
        default="task-manager",
        help="Project alias to orchestrate (default: task-manager). Supported values: "
        + ", ".join(_project_aliases()),
    )
    return parser


def main() -> int:
    parser = build_parser()
    args = parser.parse_args()

    project_name = args.project.strip().lower()
    if project_name not in PROJECTS:
        parser.error(
            f"Unknown project '{project_name}'. Supported projects: {', '.join(_project_aliases())}"
        )

    print(_describe_project(project_name))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
