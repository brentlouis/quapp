# Development workflow

## Before changing code

1. Read `AGENTS.md` and the latest Obsidian checkpoint.
2. Read the relevant section of `DESIGN.md`; read `DECISIONS.md` before structural changes.
3. Read `MODELS.md` before changing data models or planned API fields.
4. Run `scripts/doctor.ps1` if the machine or checkout is new.
5. Identify the smallest set of source, resource, and test files that own the behavior.

## While changing code

- Keep Java + XML views and the existing Activity/Fragment navigation structure.
- Preserve IDs and string keys used by Java code unless all consumers are updated together.
- Use shared theme roles, dimensions, text appearances, and strings instead of literals.
- Keep async state handling explicit: loading, content, empty, and error.
- Do not edit generated folders (`build/`, `app/build/`, `.gradle/`) or generated logo/grain files without using their source scripts.
- Keep unrelated cleanup out of focused changes.

## Verification levels

### Resource or styling change

- Parse/resource-process the affected XML.
- Inspect the target screen on a Pixel emulator.
- Check Xiaomi when system bars, paper texture, keyboard, or navigation are involved.

### Java behavior change

- Add or update a focused unit test when the behavior is deterministic.
- Run `scripts/check.ps1`.
- Exercise the affected user flow.

### Shared or release-sensitive change

- Run `scripts/check.ps1 -Full`.
- Install the debug APK and perform a smoke pass through launch, login/session routing, both home modes, queue detail, and ticket state.

## Recording work

At the end of every implemented change, follow `AGENTS.md`: update progress and the daily note automatically, plus overview, design, decisions, and other notes when applicable. Record implementation separately from build and device verification. If the vault cannot be written, stage a named record under `obsidian-starter/` and report it as pending.

## Review checklist

- Existing navigation and data flow still work.
- UI uses resource tokens and accessible touch targets.
- Loading/error/empty states remain reachable.
- Tests and checks are reported accurately.
- No generated output, machine paths, credentials, or local SDK settings are committed.
- Documentation and the Obsidian record match the implemented state.
