---
name: android-ui-design
description: Design native Android UI in Java + XML layouts that follows Material 3 conventions instead of generic AI-default output. Use this skill whenever the user is creating, editing, reviewing, or generating Android XML layouts, Activities, Fragments, RecyclerView item layouts, dialogs, or any res/layout file — even if they only ask for "a screen", "a login page", "a list", or "the UI" without naming XML or Material. Also use it for Android UI critique, redesign, or pre-submission review.
---

# Android UI Design (Java + XML)

Build native Android screens that look deliberately designed rather than machine-generated.

**Scope:** native Android, Java, XML layouts in `res/layout/`. Not Compose, not React Native, not Flutter. If the project uses Compose, say so — this guidance is XML-specific.

**Core principle:** generic output comes from skipping intent. A model asked for "a login screen" with no further thought reaches for its defaults: a vertical LinearLayout, plain widgets, hardcoded values, untouched theme colors. The fix is to force a decision about the screen's job *before* any layout gets written.

---

## Phase 1 — Design contract (before writing any XML)

State these five things explicitly, in the response, before generating a layout. Keep it short — a few lines, not an essay. This phase does the most work and is the easiest to rationalize away.

1. **Job** — what this screen is for, in one sentence.
2. **Primary action** — the single thing the user most needs to do here. There is exactly one. If two feel equally primary, the screen is doing too much.
3. **Content** — what real data appears. Realistic content, not `Lorem ipsum` or `TextView`.
4. **States** — which of `loading / empty / error / populated` this screen needs. Most list and network-backed screens need all four. Naming them here prevents shipping only the happy path.
5. **Hierarchy** — what the eye should hit first, second, third.

When the request is open-ended ("make me a settings screen"), do not silently pick defaults. Either ask one targeted question or state the assumption being made, then proceed.

---

## Phase 2 — Generate

### Non-negotiables

Each is a visible tell to a human reviewer:

- Colors reference `?attr/` roles or `@color/` resources — never raw `#RRGGBB` in a layout.
- Dimensions reference `@dimen/` or sit on the 8dp grid — never `px`, never arbitrary values like `13dp`.
- User-visible text references `@string/` — never a literal in `android:text`.
- Every interactive element is at least 48dp × 48dp in its touchable area.
- Every `ImageView` / `ImageButton` has `contentDescription` (or `@null` deliberately, when decorative).
- Material components over bare framework widgets.
- Text sizes in `sp`, never `dp`.

### Spacing

Everything on an **8dp grid**. 4dp allowed for tight optical adjustments (icon-to-label gaps). Nothing else.

| Token | Value | Use |
|---|---|---|
| `spacing_xs` | 4dp | icon-to-label, chip internals |
| `spacing_sm` | 8dp | between related items in a group |
| `spacing_md` | 16dp | **screen edge margin**, between distinct elements |
| `spacing_lg` | 24dp | between sections |
| `spacing_xl` | 32dp | major separation, above a primary CTA |

16dp is the standard screen horizontal margin. Define these as `@dimen/` so rhythm is one edit to change, not forty.

### Type scale

Use named Material roles via `android:textAppearance`, not hand-set `textSize`.

| Role | Size | Use |
|---|---|---|
| `headlineMedium` | 28sp | screen title, used once |
| `titleLarge` | 22sp | section heading |
| `titleMedium` | 16sp | list item primary text, card title |
| `bodyLarge` | 16sp | main reading text |
| `bodyMedium` | 14sp | secondary text, supporting lines |
| `labelLarge` | 14sp | button text |
| `labelSmall` | 11sp | captions, timestamps, metadata |

```xml
<TextView android:textAppearance="?attr/textAppearanceTitleMedium" />
```

**At most 3–4 distinct sizes per screen.** More and hierarchy stops reading as hierarchy and starts reading as noise.

### Color roles

Reference the semantic role; let the theme resolve it.

| Role | Meaning |
|---|---|
| `?attr/colorPrimary` | primary action, main brand surface |
| `?attr/colorOnPrimary` | content on top of primary |
| `?attr/colorSecondary` | less-prominent accents |
| `?attr/colorSurface` | card and sheet backgrounds |
| `?attr/colorOnSurface` | main text |
| `?attr/colorOnSurfaceVariant` | secondary/supporting text |
| `?attr/colorError` | error states, destructive actions |
| `?attr/colorOutline` | borders, dividers |

Secondary text is `colorOnSurfaceVariant` — not `#757575`, not `android:alpha="0.6"` on a full-strength color.

**Change the default palette.** Stock Material purple (`#6200EE`, `#3700B3`, `#BB86FC`, `#03DAC5`) is instantly recognizable as untouched-template. Define a real palette even if it's only three considered colors.

### Components

Use the Material component, not the framework widget — they get correct states, ripples, elevation, and theming for free.

| Instead of | Use |
|---|---|
| `Button` | `com.google.android.material.button.MaterialButton` |
| `EditText` | `TextInputLayout` + `TextInputEditText` |
| `CardView` | `com.google.android.material.card.MaterialCardView` |
| `Toolbar` | `com.google.android.material.appbar.MaterialToolbar` |
| `Toast` for feedback | `Snackbar` |
| `Spinner` | `MaterialAutoCompleteTextView` in a `TextInputLayout` |
| `Switch` | `com.google.android.material.materialswitch.MaterialSwitch` |

Button emphasis carries meaning: **Filled** for the one primary action, **Tonal** for important-but-secondary, **Outlined** for secondary, **Text** for cancel/dismiss. One filled button per screen — if there are two, one isn't primary.

### Touch targets

**48dp × 48dp minimum** for anything tappable. A hard accessibility floor, most often violated on small icon buttons.

```xml
<ImageButton
    android:layout_width="48dp"
    android:layout_height="48dp"
    android:padding="12dp"
    android:src="@drawable/ic_close"
    android:background="?attr/selectableItemBackgroundBorderless"
    android:contentDescription="@string/close" />
```

Keep 8dp minimum between adjacent targets.

### The four states

Any screen backed by a list or network call needs all four. Shipping only the populated state is the most common real defect in prototype apps.

- **Loading** — indeterminate `CircularProgressIndicator` or skeleton placeholders. Not a blank screen.
- **Empty** — icon + one line explaining what would be here + an action to fill it. "No results" alone is a dead end.
- **Error** — plain-language failure + a retry control. Never a raw exception or bare "Error".
- **Populated** — the real thing.

Implement with a `ViewFlipper`, or sibling views in a `FrameLayout` with visibility toggled from the Activity.

### Layout containers

- **`ConstraintLayout`** — default for screen-level layouts. Flat, flexible.
- **`LinearLayout`** — fine for genuinely linear runs of 2–4 views. Not as a nesting tool.
- **`FrameLayout`** — overlapping content, state switching.
- **`RecyclerView`** — every list. Never a `ScrollView` containing a `LinearLayout` filled in code.
- **`NestedScrollView`** — scrolling form/detail content that isn't a list.

Keep hierarchies under 4 levels deep. `RecyclerView` row layouts inflate repeatedly — keep those especially flat.

### Accessibility floor

- `contentDescription` on every meaningful `ImageView` / `ImageButton`; `@null` deliberately for decorative.
- `android:labelFor` linking labels to inputs, or `TextInputLayout`'s `android:hint`.
- Text contrast at least 4.5:1.
- `sp` for text sizes, always.

---

## Phase 3 — Self-check

Before presenting a layout, verify against these. They're the mechanical tells a reviewer notices immediately.

**Errors — fix before presenting:**

| Tell | Fix |
|---|---|
| Raw hex color in layout | `?attr/` role or `@color/` |
| Stock template purple | a real palette |
| `px` units | `dp` / `sp` |
| `textSize` in `dp` | `sp` |
| Interactive element under 48dp | pad out to 48dp |
| `ImageView` with no `contentDescription` | add it, or `@null` |
| `ScrollView` + `LinearLayout` as a list | `RecyclerView` |

**Warnings — fix unless there's a stated reason:**

| Tell | Fix |
|---|---|
| Inline `13dp`-style dimension | `@dimen/`, on the 8dp grid |
| Literal `android:text="Submit"` | `@string/submit` |
| Bare `Button` / `EditText` | Material component |
| Hierarchy 5+ levels deep | flatten with `ConstraintLayout` |
| Nested `layout_weight` | `ConstraintLayout` chains |
| Container with a single child | delete the wrapper |
| `TextView` / `Lorem ipsum` placeholder | realistic content |
| Hand-set `textSize` | `?attr/textAppearance*` |
| Input with no hint or `labelFor` | wrap in `TextInputLayout` |
| Fixed height on a text view | `wrap_content` + `minHeight` |

Also avoid: **cards everywhere** (a card means "discrete tappable object" — a form field isn't one; most screens need zero), and **elevation on non-interactive containers** (static content sits flat).

**A layout that passes all of this can still be badly designed.** These checks remove the machine fingerprints; they don't supply judgment about hierarchy, or whether the layout suits the content. That comes from phase 1.

---

## Starter resources

> **Quapp:** don't use these starters. `DESIGN.md` in the repo root defines the real tokens (Civic Paper colours, `space_xs|sm|ms|md|lg|xl`, radii, type styles), and they win wherever they differ from anything in this skill.

`res/values/dimens.xml`
```xml
<resources>
    <dimen name="spacing_xs">4dp</dimen>
    <dimen name="spacing_sm">8dp</dimen>
    <dimen name="spacing_md">16dp</dimen>
    <dimen name="spacing_lg">24dp</dimen>
    <dimen name="spacing_xl">32dp</dimen>
    <dimen name="touch_target_min">48dp</dimen>
    <dimen name="corner_radius">12dp</dimen>
</resources>
```

`res/values/colors.xml` — placeholders; replace with a real palette.
```xml
<resources>
    <color name="brand_primary">#1B5E4A</color>
    <color name="brand_on_primary">#FFFFFF</color>
    <color name="brand_secondary">#4A6572</color>
    <color name="surface">#FCFCF9</color>
    <color name="on_surface">#1A1C1B</color>
    <color name="on_surface_variant">#5C5F5D</color>
    <color name="outline">#C4C7C5</color>
    <color name="error">#BA1A1A</color>
</resources>
```

Build dependency:
```gradle
implementation 'com.google.android.material:material:1.12.0'
```
