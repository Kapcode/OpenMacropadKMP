# AI Agent Guide

This document is the primary entry point for AI assistants and automated tools working on the OpenMacropadKMP project. It provides a map of documentation and critical instructions for maintaining project health.

## 📚 Documentation Index

Before making changes, consult these documents to understand the project's architecture, design philosophy, and history:

*   **[README.md](README.md)**: General project overview and setup instructions.
*   **[CODE_MAP.md](CODE_MAP.md)**: Deep dive into the project structure, package responsibilities, and core components. **Read this first for technical navigation.**
*   **[DEVELOPMENT_NOTES.md](DEVELOPMENT_NOTES.md)**: A "Technical Memory" of challenges, solutions, and architectural decisions. Reference this to avoid repeating past mistakes.
*   **[DESIGN_LANGUAGE.md](DESIGN_LANGUAGE.md)**: UI/UX principles, Material 3 implementation details, and platform-specific layout rules.
*   **[SECURITY.md](SECURITY.md)**: Overview of the cryptographic handshake, identity management, and network security protocols.
*   **[ISSUES.md](ISSUES.md)**: Known bugs, pending tasks, and tracking for complex features.
*   **[plans.md](plans.md)**: Roadmap and high-level feature planning.

## 🛠️ Maintenance Instructions for Agents

To ensure the project documentation remains a "Source of Truth," all AI agents **MUST** follow these rules:

1.  **Update Documentation with Code**: If you modify core logic, refactor packages, or change UI layouts, you **MUST** update the corresponding `.md` file in the same session.
2.  **Log Technical Challenges**: When solving a non-trivial bug or architectural hurdle, add a new section to `DEVELOPMENT_NOTES.md` explaining the problem and the "Why" behind the solution.
3.  **Keep the Code Map Current**: If you move files or change the responsibility of a module, update `CODE_MAP.md`.
4.  **Verify UI Transitions**: When changing tab orders or screen layouts (especially the Dashboard/Active Pack/Marketplace flow), update `DESIGN_LANGUAGE.md` to reflect the new "Ground Truth."
5.  **Check for Redundancy**: Before adding new documentation, check if a section already exists in one of the files above and update it instead of creating duplicates.

## 🚀 Current Focus
Refer to `plans.md` and the `overall_goal` in the system state snapshot for the current development priorities.
