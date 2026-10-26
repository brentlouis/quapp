# Diagnostics

Keep local diagnostic evidence here instead of at the repository root.

- `jvm-crashes/` stores JVM fatal-error dumps (`hs_err_pid*.log`). These files may include machine-specific paths and runtime details and are ignored by Git.
- Android runtime evidence should normally be captured through `scripts/logcat.ps1`.

Diagnostic output is not source code. Record the conclusion and fix in progress notes; do not depend on a local log remaining available.
