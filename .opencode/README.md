# OpenCode project config

This project keeps a project-level OpenCode config in `.opencode/opencode.json`.

## Kotlin LSP

This repo uses the official JetBrains Kotlin LSP launcher:

- `/root/.local/share/opencode/bin/kotlin-ls/official/kotlin-lsp.sh`
- launched with `--stdio`

Configured extensions:

- `.kt`
- `.kts`

## Config priority

OpenCode loads config from multiple places. For this project, the important rule is:

- project config in `.opencode/opencode.json` overrides user config in `~/.config/opencode/opencode.json`

So the global config gives Kotlin LSP support to new projects by default, while this project can still override it locally if needed.

## Quick verification commands

Run from the Android project directory:

```bash
opencode debug lsp diagnostics "/data/workspace/thunder_note/thunder-note-android/app/src/main/java/com/flashnote/MainActivity.kt"
opencode debug lsp document-symbols "file:///data/workspace/thunder_note/thunder-note-android/app/src/main/java/com/flashnote/MainActivity.kt"
```

For deeper debugging:

```bash
opencode --print-logs --log-level DEBUG debug lsp diagnostics "/data/workspace/thunder_note/thunder-note-android/app/src/main/java/com/flashnote/MainActivity.kt"
```

## Important note

The official `kotlin-lsp.sh` must be started with `--stdio` for OpenCode. Without it, the server defaults to TCP mode and OpenCode initialization times out.
