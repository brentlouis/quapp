# DESIGN.md — Quapp visual system: Civic Paper

The one visual reference for building Quapp's screens. It describes **what things look like**. **Why** they look that way is in `DECISIONS.md`, and the screen-by-screen mockups are on the design canvas.

- **Canvas (the screens to build):** "Quapp Civic Paper", https://claude.ai/artifact/8tYrtvGGmiGoguHxqEaudp. There are 64 artboards. Screen numbers below (`08`, `12b`, …) are the artboard numbers there.
- **Design system (tokens and components):** "Civic Paper", https://claude.ai/artifact/Q1Z2kpJEZpDPKCnaH8HMjn
- **Earlier directions, reference only:** "Quapp Now Serving" (dark board) and the monochrome canvas. Don't build from them.

Content, flows, states and copy on the canvas are final. Where this file and the canvas disagree, the canvas wins; fix this file.

Last updated: 2026-09-29 (catalogue cards, vintage ground).

---

## 1. The idea

**The printed ticket.** Quapp looks like the paper queue ticket you pull from a dispenser at the municipal hall. Warm cream stock, dark coffee ink, a serif number, and half-circle holes where the ticket tore off the roll. The screen is the counter. Most of it is plain printed paper, and one ticket sits on it. That ticket is the only dark thing on the screen.

It is still Material 3 underneath: app bar, bottom navigation, extended FAB, snackbar with Undo, bottom sheets, dialogs, switches, segmented buttons. The 48dp touch-target minimum and the docked primary action don't change.

**Light theme only for v1.** No `values-night`. (Espresso, the dark variant, is deferred.)

---

## 2. Colour

### Tokens

Android names go in `res/values/colors.xml`.

| Token (`@color/…`) | Hex | Used for |
|---|---|---|
| `paper` | `#F2EDE3` | Screen ground, with the grain on top. Also the dock background. |
| `paper_raised` | `#FBF8F2` | Cards, sheets, dialogs, fields, search, nav bar. |
| `paper_sunk` | `#E9E2D4` | Tonal buttons, card stat footers, skeletons, Upcoming pill, active nav tab, tiles. |
| `line` | `#D8CFBE` | Every 1dp hairline and card edge. |
| `outline` | `#8C8474` | Borders on things you can touch: fields, chips, outlined buttons, radios (3:1 on paper). |
| `ink` | `#17150F` | Body text, headings, filled buttons, selected chips, FAB. |
| `ink_muted` | `#4F4A40` | Secondary text (7.5:1). |
| `ink_faint` | `#6E6859` | Placeholders, disabled text, tertiary meta (4.75:1). |
| `spotlight` | `#2B211C` | Espresso. The one ticket per screen, the Called screen's ground, the snackbar. |
| `on_spotlight` | `#F6EFE6` | Text on spotlight. |
| `on_spotlight_muted` | `#D2C4B4` | Secondary text on spotlight; the outline of buttons on the Called screen. |
| `spotlight_line` | `#4A3A30` | Dividers and tonal buttons inside the spotlight; countdown ring track. |
| `crema` | `#C8A27A` | Tear lines, the logo's tear line, focus ring, Undo on the snackbar. **Never text on paper.** |
| `roast` | `#6B4A2F` | Text buttons and links (6.8:1). |
| `signal` | `#F2C14E` | Marigold. **Only on the spotlight**, only for a number being called or served and the "I'm here" button. |
| `on_signal` | `#1C140E` | Text and icons on signal. |
| `ok` / `ok_soft` | `#2F6B47` / `#DCEAD9` | Open, Served, Verified, ON. |
| `warn` / `warn_soft` | `#80590C` / `#F1E5C8` | Paused, warning notes, "Closes in 40 min". **Not Upcoming.** |
| `err` / `err_soft` | `#B3261E` / `#F6DCD8` | Errors, No-show, destructive actions. |

Inside the spotlight, `ok`, `warn` and `err` switch to lighter versions so they stay readable on espresso: `#9FD9B0`, `#E9C77E`, `#F2A69C`.

### Material 3 role mapping (`Theme.Quapp`)

| M3 role | Token |
|---|---|
| `colorPrimary` / `colorOnPrimary` | `ink` / `paper_raised` |
| `colorSecondaryContainer` / `colorOnSecondaryContainer` | `paper_sunk` / `ink` (chips, nav indicator) |
| `colorSurface` / `colorOnSurface` | `paper` / `ink` |
| `colorSurfaceContainerLow` / `colorSurfaceContainer` | `paper_raised` |
| `colorSurfaceContainerHigh` / `colorSurfaceVariant` | `paper_sunk` |
| `colorOnSurfaceVariant` | `ink_muted` |
| `colorOutline` / `colorOutlineVariant` | `outline` / `line` |
| `colorError` / `colorOnError` / `colorErrorContainer` | `err` / white / `err_soft` |
| `colorTertiary` | `roast` |
| `android:windowBackground` | `@drawable/bg_paper` (paper + grain, section 7) |

`spotlight`, `signal` and `crema` have no M3 role. Use them through `ThemeOverlay.Quapp.Spotlight` (surface = `spotlight`, onSurface = `on_spotlight`, onSurfaceVariant = `on_spotlight_muted`, outline = `on_spotlight_muted`, outlineVariant = `spotlight_line`). Apply it to each spotlight view and to the Called Activity's root, so every Material component inside picks up the dark colours by itself.

### Status colours

Status always shows as a word plus a colour, never colour alone.

| Status | Ground | Text |
|---|---|---|
| Open / Served / Live now | `ok_soft` | `ok` |
| Upcoming / Waiting | `paper_sunk` | `ink_muted` (neutral: good news, not a warning) |
| Paused | `warn_soft` | `warn` |
| No-show / Suspended | `err_soft` | `err` |
| Closed / Queue closed | none, 1dp `outline` ring | `ink_muted` |
| Called | `spotlight` | `signal` |

### Colour rules

- **One spotlight per screen.** When two things want it, the one the person acts on wins, and the other becomes a normal card.
- **Signal only on dark.** Marigold never touches paper. On paper, a "Called" label is an espresso pill with marigold text.
- **Crema is decoration.** It fails text contrast on paper.

---

## 3. Type

Three families, bundled in `res/font/` (all SIL OFL):

- **DM Serif Display:** `dm_serif_display.ttf`, regular only.
- **IBM Plex Sans:** 400, 500, 600 and 700.
- **IBM Plex Mono:** 500 and 600.

| Style (`TextAppearance.Quapp.…`) | Font | Size / line | Used for |
|---|---|---|---|
| `Display` | DM Serif 400 | 32 / 35 | Screen headlines ("Find a queue", "Your queues", "You're all set") |
| `Title` | DM Serif 400 | 22 / 26 | Sheet, dialog and empty-state titles; form section titles; card titles inside the spotlight |
| `TicketNumber` | DM Serif 400 | 56 / 53 | A number that names a ticket: #43 on the ticket, console, live card |
| `TicketNumber.Called` | DM Serif 400 | 132 / 119 | The Called screen (160 on the counter display) |
| `HeadingSmall` | Plex Sans 600 | 17 / 21 | App bar titles; card titles on paper (16); list primaries (15) |
| `Body` | Plex Sans 400 | 15 / 22 | Reading text |
| `BodyStrong` | Plex Sans 600 | 16 / 21 | Emphasised lines |
| `Caption` | Plex Sans 400 | 13 / 18, `ink_muted` | Supporting lines, helper text |
| `Button` | Plex Sans 600 | 15 (16 at 56dp, 18/700 at 64dp) | Buttons |
| `StatHero` | Plex Mono 600 | 26 / 29 | Hero column of a card footer |
| `Stat` | Plex Mono 600 | 22 / 24 | Other stats, list trailing numbers, times, countdowns |
| `Label` | Plex Mono 500 | 12 / 16, caps, 0.08em | Status pills, section labels ("IN TAGBILARAN CITY · 4"), stat labels (11) |

**Point these at theme attributes; don't set them per view.** Map the Material roles onto them in `Theme.Quapp`:

| Material role | Style |
|---|---|
| `textAppearanceHeadlineMedium` | `Display` |
| `textAppearanceTitleLarge` | `Title` |
| `textAppearanceTitleMedium` | `HeadingSmall` |
| `textAppearanceBodyLarge` | `Body` |
| `textAppearanceBodySmall` | `Caption` |
| `textAppearanceLabelLarge` | `Button` |
| `textAppearanceLabelSmall` | `Label` |

`TicketNumber`, `Stat` and `StatHero` get custom theme attributes (`textAppearanceTicketNumber`, `textAppearanceStat`, `textAppearanceStatHero`) declared in `attrs.xml`. Layouts still only say `?attr/textAppearance…`, and there is still no `textSize` anywhere.

**Rules**

- **Ticket vs tally.** A number that names a ticket (#43, "now serving #23") is serif. Anything that counts or ticks (11 min, 8 in line, 41 ahead, 2:14) is mono. In lists and card footers, everything is mono so the columns line up.
- **Serif floor.** DM Serif is never below 20sp and never bold (it has no bold, so Android would fake one). Avatar initials are therefore Plex Sans 600.
- Sentence case everywhere. Only `Label` is uppercase.

---

## 4. Spacing, shape and depth

**Spacing (`dimens.xml`):**

| Token | Value |
|---|---|
| `space_xs` | 4 |
| `space_sm` | 8 |
| `space_ms` | 12 |
| `space_md` | 16 |
| `space_lg` | 24 |
| `space_xl` | 32 |

- **16dp** is the screen edge and the card padding.
- **12dp** is the gap between cards and the padding inside small controls.

**Radii:**

| Token | Value | Used for |
|---|---|---|
| `radius_sm` | 6dp | Fields, notes, snackbar, tiles |
| `radius_md` | 10dp | Cards, buttons, FAB, dialogs, the spotlight |
| `radius_sheet` | 16dp | Top corners of bottom sheets |
| Pill | fully rounded | Unselected chips, search, status pills, switch track |

**Depth:** flat. Cards are `paper_raised` with a 1dp `line` edge and no shadow. Only floating things get a shadow: the FAB, dialogs and the heads-up notification. The scrim behind sheets and dialogs is `ink` at 40%. **Modals cover the whole screen, bottom nav included.**

---

## 5. The ticket vocabulary

These details are what make Civic Paper read as a ticket. Each has one job. Apart from the spotlight, a screen gets at most one of them (Queue detail is the agreed exception: slip + serial + stub).

| Detail | What it is | Where |
|---|---|---|
| **Spotlight** | Espresso card with half-circle **punches** (9dp radius) cut into its sides. The punches are real holes, so the paper and grain show through. | One per screen (section 6) |
| **Tear line** | 1.5dp dashed `crema` line, 4dp on, 4dp off | Down the ticket stub; across the top of the bottom nav; above **every** bottom button bar (dock) |
| **Tear-off stub** | Dock on `paper_raised` with a tear line and notched top corners | Where you're handed a number: the "You'll be #43" join bar (06, 38, 44, 52, 53), Confirm and join (07, 43) |
| **Kept ticket** | Small paper ticket, perforated down the middle with holes top and bottom | Outcomes: Served, Slot released, Queue closed (22, 23, 24) |
| **Rubber stamp** | Tilted (−6°), double-ruled outline, mono caps | One headline status on an outcome only: SERVED (ok), SLOT RELEASED (err), QUEUE CLOSED (ink_muted) |
| **Receipt slip** | `LABEL ······ value` rows with dotted leaders | Stats on queue detail screens, Insights (16), Queue history (11) |
| **Serial line** | Category as a mono caps serial ("RELIEF") | Queue detail screens |
| **Barcode** | Thin decorative barcode under your number | Your own ticket stub only (08, 25, 26, 28, 12b) |
| **Punched selection** | Small notches (5dp) on the sides of the active nav tab and a selected filter chip; 2.5dp on the nav badge | Bottom nav, filter chips, admin sidebar |

Forms, the owner console, lists and settings stay plain paper, apart from the tear line on their bottom bar.

---

## 6. Where the spotlight goes

At most one per screen:

| Screen | Spotlight |
|---|---|
| **My ticket (08, 25, 26, 28)** | Ticket with a **stub** ("YOUR NUMBER", #43 in `TicketNumber`, barcode), a vertical tear line (punches at its top and bottom), and a **body** ("IN LINE", 41 mono 34 "ahead of you", progress ticks, "Now serving #1"). Queue info rows sit below on paper. |
| **Called (09)** | The whole screen ground turns `spotlight` (with the ticket texture, light status-bar icons). The number is `TicketNumber.Called` in `signal`. The countdown ring is `signal` on a `spotlight_line` track. "I'm here" is 64dp `signal`; "Move me back" is outlined in `on_spotlight_muted`, with the helper line below it. |
| **Live console (15, 29, 30, 57)** | The Now serving panel: #23 in `signal` while being served (dimmed when timed out), then a tear line, then the name in `Title`. The wait line is light ok / err. |
| **Owner home (13, 58)** | The live queue card: number in `signal`, tear line, then a `Title`-style name, then the Console tonal button. |
| **Home, in line (05, 39–42)** | Slim "You're in line" banner, about 72dp, 6dp punches. |
| **My tickets (12, 12b)** | The called card (marigold number and "I'm here"), or a compact in-line ticket when nobody is called. |
| **Called notification (21), notification permission (20)** | The notification itself; the home banner behind drops to a normal card. |
| **Counter display (32)** | Full spotlight ground, the serving number at 160 in `signal`, "UP NEXT" in `Label`, the QR on a `paper_raised` plate. |
| **Splash (Main)** | The logo mark (section 8). |
| Everything else | No spotlight. |

---

## 7. Texture

**Everything is paper, so everything has texture.** The ground is soft vintage paper, and each component with a fill has the texture of what it's made of.

| Tile (`res/drawable-nodpi/`) | Looks like | On |
|---|---|---|
| `paper_vintage.png` | Large soft warm blotches, fibres, small age spots and a few faint stains, `#744F2D`, about 5% | The screen ground (`bg_paper`, the window background, and the pinned headers) |
| `grain_stock.png` | Short dark fibres and fine tooth, about 3.5% | Light fills: queue cards, other cards, the receipt slip, fields, notes, chips, tonal buttons, tiles, the nav bar, sheets, the stub dock |
| `grain_ink.png` | Pale specks and an uneven lay-down, about 5.5% | Dark fills: filled buttons, the selected chip, the FAB, Danger |
| `grain_ticket.png` | Pale fibres, soft mottle, a few specks, about 6% | The espresso spotlight, and the Called screen's ground (`bg_spotlight`) |

- All four are seamless PNGs, mostly transparent, drawn at one texture pixel per screen pixel: 512×512 for the ground (so its blotches don't visibly repeat), 256×256 for the rest. `tools/make_grain.py` draws them; its `STRENGTH` (2.4) sets how loud the component tiles are.
- No darkened edges on the ground: the pinned headers on Browse and Your queues draw the same ground, and a vignette would show as bands where they meet.
- The tile follows the fill (`Grain.tileFor`): no fill gets none (the ground shows through text and outlined buttons), `spotlight` gets ticket, a dark fill gets ink, and a light fill gets stock. It's read every frame, so a chip that becomes selected changes texture by itself.
- The texture is painted inside the component's own shape, so rounded corners, pills and punches stay clean.
- `QuappViewInflater` (the theme's `viewInflaterClass`) attaches it as layouts inflate: to buttons, cards, chips, fields, the FAB and the nav bar, and to any layout, TextView or ImageView whose background is a paper shape (`bg_card`, `bg_note`, `bg_tile` and the rest listed there). Backgrounds built in code go through `TicketShapes.background`, which adds it. Bottom sheets call `Grain.attach(sheet)`.
- Not textured: status pills (too small to show it), the QR plate (it has to scan), and the snackbar.
- Check the textures on the Xiaomi: low-end screens can make them look darker.

---

## 8. Logo

One mark, drawn once and only scaled (canvas board "Brand · the Quapp mark"):

- **Shape:** an espresso ticket on a 40 × 50 grid (4:5), corner radius 5.
- **Punches:** radius 3.4 on both side edges at y 36 (72%).
- **Tear line:** crema, x 6.5–33.5 at y 36, stroke 1.3, dash 2 / gap 2.
- **The Q:** DM Serif Display 28, cream `#F6EFE6`, centred, baseline 26.5. Its tail clears the tear line.

Where it's used:

| Where | Form |
|---|---|
| Splash | The mark alone, about 112dp wide |
| Log in, Role select, Admin | Lockup: mark 34dp + "Quapp" in DM Serif 26 |
| Launcher icon | Adaptive icon with a `paper` background layer and the ticket in the 66dp safe zone. Replaces the teal wordmark in `ic_launcher_foreground.xml`. |
| Notification small icon | One-colour cut-out: the ticket silhouette with the Q and holes punched through |

Never in marigold or teal, never outlined, no shadow, no bold Q.

---

## 9. Components

**Buttons.** 48dp tall (56 in the dock, 64 for "I'm here"), `radius_md`, `Button` type.

| Kind | Look |
|---|---|
| Filled | `ink` with `paper_raised` text. One per screen. |
| Tonal | `paper_sunk` with `ink` text. |
| Outlined | 1dp `outline` with `ink` text. |
| Text | `roast` (crema on the spotlight). |
| Danger | `err` text, or an `err` fill with white text for a confirmed destructive act. |
| Call | `signal` fill. On the spotlight only. |
| Disabled | `paper_sunk` with `ink_faint` text. |

**Chips.** 40dp (48dp hit area), 1dp `outline`, pill shape, Plex Sans 500 14.
- **Selected:** `ink` fill, `paper_raised` text, a check, and the punched ticket shape (radius 8, 5dp side notches).
- **Town dropdown:** stays a `paper_sunk` pill and is sticky.

**Queue card (v6): a catalogue card.** Like an old library catalogue card, laid on the table by hand. Same card on Browse and on the organizer's Queues tab.
- **Card:** `index_card` (whiter than `paper_raised`, so it lifts off the vintage ground), a 1dp `index_card_edge`, `radius_index_card` (3dp), 2dp elevation, 16dp between cards. The card stock texture on top. Each card is turned a fraction of a degree (−0.5°, 0.4°, −0.3°, 0.5° in turn; `QueueAdapter`).
- **Top line:** the category and the queue's number in `Label`, `ink_faint` ("RELIEF · NO. 001"). At the end, the status as a small rubber stamp (`Widget.Quapp.Stamp.Small`, tilted −5°, double-ruled outline): OPEN `ok`, PAUSED `warn` (with the time, for organizers), UPCOMING and CLOSED `ink_muted`.
- **Name:** `Title` (DM Serif 22), `ink_muted` when paused or closed, with the verified badge after it. Under it a thin double `crema` rule (`bg_double_rule`).
- **Details, one per ruled line** (`view_card_row`: the label in `ink_muted` on the left, the value on the right, a faint roast hairline under it):
  - Where: the venue, plus "Within 1 km" when a radius applies.
  - What: the one-line description (hidden when there's none).
  - When: the schedule in mono ("Day 1 of 2 · 8 AM – 8 PM"), or "Closes in 40 min" in `warn`.
  - Wait (queuers): "55 min · 42 in line · serving #21" in mono. Paused: "Paused since 10:05 AM · 12 in line". Closed: "Closed at 4 PM". Upcoming: the label is Opens, "1 PM · 5 joined early".
  - Today (organizers): "42 waiting · 18 served · 3 no-shows". Closed: "Closed at 4 PM · 45 served".
- **Hole:** a 10dp punched hole at the bottom centre (`bg_card_hole`).

**Status pills.** 24dp, pill shape, `Label` type, with a 6dp dot or a 14dp icon. Colours as in section 2.

**Fields.**
- The label sits above (Plex Sans 500 14).
- The box is 52dp, `paper_raised`, 1dp `outline`, `radius_sm`, with 16sp input text.
- Helper text is 13.
- The error state is a 2dp `err` outline plus an alert icon and `err` helper text.

**Search.** 48dp pill, `paper_raised`, 1dp `line`.

**Bottom nav.** 80dp, `paper_raised`, a tear line along the top.
- **Active tab:** a 64×32 `paper_sunk` punched ticket, with the label in `ink` 700.
- **Inactive tabs:** labels in `ink_muted` 12.5.
- **Badge:** an `ink` stub with mono `paper_raised` text. When a ticket is being called, it becomes a `spotlight` stub with `signal` text.

**Dock (bottom button bar).** `paper` ground, 12dp/16dp padding, a tear line on top. On the join screens it becomes the tear-off stub.

**Snackbar.** `spotlight` ground, `on_spotlight` text, `radius_sm`. The Undo action is `crema`, bold and underlined.

**Sheets.** `paper_raised`, 16dp top corners, a 32×4 `outline` grab handle, a `Title` heading and a `Caption` lead line.

**Dialogs.** `paper_raised`, `radius_md`, a `Title` heading, and right-aligned `roast` text actions (`err` for destructive ones).

**Switches, segmented buttons, radios.**
- **Switch:** ON is an `ink` track with a `paper_raised` thumb. OFF is a `paper_sunk` track with a 2dp `outline` stroke.
- **Segmented button:** 44dp, pill outer corners. The selected segment is `ink` with a check.
- **Radio rows:** 56dp.

**Verification checks.** Rows are ruled with `line`. The trailing state is a `Label` pill: ON in `ok` on `ok_soft`, PLANNED in `ink_muted` on `paper_sunk`. The names and order of the four checks don't change.

**Icons.** Lucide at 20dp (22 in the app bar, 16–18 in chips), 1.75 stroke, round caps and joins, `ink_muted` (`on_spotlight_muted` inside the spotlight). No emoji, no filled icons.

---

## 10. Android implementation notes

- **Fonts:** bundled (see section 3), not Downloadable Fonts. Bundling avoids a flash of the fallback font on slow data. Remove `barlow_semibold.ttf`.
- **Punches:** a `MaterialShapeDrawable` whose `ShapeAppearanceModel` uses a small custom `EdgeTreatment` that draws an inward arc at a given offset (about 15 lines, like `TriangleEdgeTreatment`). The same class, smaller, draws the nav tab, the badge and the selected chip.
- **Tear line:** a `<shape android:shape="line">` with `android:dashWidth="4dp"` and `android:dashGap="4dp"`, stroke 1.5dp `crema`. The vertical one on the ticket is a rotated line or a 1.5dp-wide view with a repeating dash drawable.
- **Spotlight colours:** `ThemeOverlay.Quapp.Spotlight` via `android:theme` on the view. Never recolour children one by one.
- **Called screen:** the Activity uses a `Theme.Quapp.Called` theme with a `spotlight` window background and light status-bar icons (`windowLightStatusBar=false`).
- **Numbers:** one plain `TextView` each, with `android:fontFeatureSettings="tnum"` on anything that ticks. No ghost digits, glow or custom numeral view.
- **Insets:** unchanged. The `SystemBars` helper goes on every root; forms use `applyPaddingWithKeyboard`.

---

## 11. Do and don't

**Do**
- Keep each screen's content, states and copy exactly as the canvas shows.
- Use at most one spotlight per screen, and make it the thing the person acts on.
- Set ticket numbers in serif and anything that ticks in mono.
- Give every filled component its texture (section 7), and keep it faint.
- Make punches real holes.
- Pair every status colour with a word.

**Don't**
- Put marigold on paper, or use it for anything but called / now serving.
- Use crema for text on paper.
- Set DM Serif below 20sp or bold.
- Put gradients on cards, or shadows on anything but the queue cards (they're cards laid on a table).
- Add torn edges or sepia. The ground is old paper, but the things on it (cards, tickets, buttons) are clean new stock.
- Give Upcoming and Paused the same colour.
- Add a ticket detail where section 5 doesn't list one.
