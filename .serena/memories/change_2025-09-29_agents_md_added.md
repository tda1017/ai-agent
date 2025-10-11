2025-09-29: Added AGENTS.md (Repository Guidelines) at repo root.
Reason: Contributor guide requested by user; concise 200–400 words, Markdown headings, actionable examples.
Scope: New file `AGENTS.md` only; no code changes.
Summary: Document covers project structure (Spring Boot backend, Vue/Vite frontend), build/test/dev commands (Maven + npm scripts), coding style (Java/Vue conventions), testing guidelines (JUnit 5; suggested coverage), commit/PR guidance (Conventional Commits), and security/config notes (use Spring properties/env; no secrets).
Deviation from root AGENTS standard: Document written in English per explicit user instruction (root guideline prefers Chinese). Rationale recorded; revert path: remove `AGENTS.md` or supply Chinese variant if policy requires.
Evidence: File list confirms presence; `pom.xml` and `frontend/package.json` inspected via Serena search to tailor commands.
Validation: apply_patch succeeded; content aligned to detected structure and scripts.