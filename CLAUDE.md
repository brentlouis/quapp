# Quapp — instructions for Claude Code

Quapp is a public community queue management app for an Android course at BISU. Queue owners (LGU staff, clinics, organizers) create public queues; queuers browse and join remotely. Wait-time forecasting (rolling average) and one anti-prank verification strategy (grace period with "I'm here" confirmation) are in scope; everything else in the proposal is future work.

Before changing anything structural, read `DECISIONS.md` — it records why things are the way they are. `DESIGN.md` is the visual system (Civic Paper): colours, type, spacing, the ticket details and where each one goes; build every screen from it and the design canvas it links to. `PROGRESS.md` is the task list: tick items in the same commit as the work.

## Stack — don't deviate
- Native Android, **Java + XML views**. Min SDK 24. **No Kotlin, no Compose**, no cross-platform frameworks.
- Material Components (`com.google.android.material`), Material 3 theme. Light theme only (no `values-night`).
- Backend: FastAPI + Retrofit on the client. No Room/SQLite. Data currently comes from `FakeData`.
- Layouts are hand-written XML. Never produce visual-editor-style output (`tools:` leaks, absolute positioning, conflicting constraints).

## UI conventions
Follow `DESIGN.md` for how things look, and `.claude/skills/android-ui-design/SKILL.md` for how layouts are built. Project-specific rules on top of both:
- Colors: `?attr/` roles in layouts. The spotlight (espresso ticket) gets its colours from `ThemeOverlay.Quapp.Spotlight` on the view, not per-child overrides. Exceptions that stay `@color/`: status colours (`ok`, `warn`, `err` and their `_soft` grounds), `crema` tear lines, and `signal` (marigold, only inside the spotlight).
- Spacing: only `@dimen/space_xs|sm|ms|md|lg|xl` (4/8/12/16/24/32dp). 16dp screen edge margin and card padding, 12dp between cards.
- Type: `?attr/textAppearance*` only, including the custom `textAppearanceTicketNumber`, `textAppearanceStat` and `textAppearanceStatHero`. No `textSize`, no text sizes in `dimens.xml`. Ticket numbers are serif; anything that counts or ticks is mono.
- Strings in `strings.xml`, prefixed by screen (`history_empty_title`).
- One filled button per screen. Cards only for discrete tappable objects. At most one spotlight per screen.
- Ticket details (tear line, stub, stamp, slip, barcode, punched selection) only where `DESIGN.md` section 5 lists them.
- ConstraintLayout at screen level; LinearLayout only for short linear runs. `0dp` = match constraints.
- Every screen root gets the `SystemBars` insets helper; form screens use `applyPaddingWithKeyboard`. Never `fitsSystemWindows`.
- Network-backed screens need loading / empty / error / populated states.

## Code conventions
- RecyclerView for every list. Adapters expose a listener interface; click listeners are set in `bind`, not the ViewHolder constructor.
- Pass ids between Activities, not objects. Extra keys are `public static final String` constants on the receiving Activity.
- `finish()` to go back, `startActivity` to go forward.
- Models are immutable (`final` fields, no setters). Don't add fields to `Queue`/`Ticket` casually — they must match the future API contract; propose the change first.
- Status values are enums, never strings.

## How to work with me (Brent)
I'm learning Android — I need to be able to explain every line at a defense.
- **Default (teach mode):** before writing code, explain the approach and why in a few sentences. Build one screen at a time. After each screen, stop so I can build and test on my device before you continue.
- Explain syntax and language mechanics when I ask. No question is too basic.
- Push back when my approach is wrong. Flag trade-offs.
- Keep explanations concise. No preamble.
- If I say **"just give it to me"**, write working code with minimal explanation for that task.
- Don't mark something done in `PROGRESS.md` until I confirm it runs.
- When a decision is made, add an entry to `DECISIONS.md` in its existing format (## heading, **Decision / Why / Also considered / Status**).

## Testing
Physical Xiaomi (HyperOS) plus a Pixel emulator. If something renders wrong on the Xiaomi only, check the emulator before changing code — HyperOS draws its own nav bar background.
