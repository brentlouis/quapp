# DESIGN-experiment.md — Quapp: Modernist Civic

Created 2026-10-09. Status: visual proposal for review.
The original Civic Paper specification remains in `DESIGN.md`. This experiment has not been implemented in the Android app.

## Direction

Swiss modernist civic UI: a strict grid, white surfaces, bold sans-serif numbers, generous whitespace, thin separators, and a single cobalt accent. Queue numbers and the next action lead each screen.

Companion visual: [Modernist UI study](design-experiment/modernist-ui.png).
The visual shows Browse, My ticket, and Live console. Sample data is illustrative. The specification governs intended behavior when generated details differ.

Keep existing content, flows, states, trust checks, permissions, and data contracts. The original canvas remains a reference for those flows; its paper styling does not govern this experiment.

## Color system

| Experimental token | Hex | Use |
|---|---|---|
| modern_ground | #F5F6F8 | Screen background |
| modern_surface | #FFFFFF | Rows, forms, app bars, sheets, docks |
| modern_ink | #171C26 | Main text |
| modern_muted | #596273 | Supporting text |
| modern_line | #DCE1E8 | Decorative dividers |
| modern_outline | #778190 | Interactive control boundaries |
| modern_primary | #2447D8 | Primary buttons, active navigation, number panel |
| modern_primary_soft | #EEF2FF | Active-ticket banner, selected filters |
| modern_on_primary | #FFFFFF | Text on cobalt |
| modern_ok / modern_ok_soft | #21633E / #E9F5ED | Open, verified, served, confirmed |
| modern_warn / modern_warn_soft | #805300 / #FFF3DA | Paused, closure warnings |
| modern_error / modern_error_soft | #B42318 / #FDECEA | Errors, no-show, destructive actions |

Waiting and Upcoming are neutral. Called uses cobalt and an explicit state label, countdown, and instruction. Always pair status color with words or icons.
Essential text on cobalt stays white; do not lower its opacity.
Use outline for controls; line is for decorative separators.
Map Material primary to cobalt, onPrimary to white, surface to white, onSurface to ink, outline to outline, and outlineVariant to line.
Keep these experimental tokens isolated from production resources until the direction is selected.

## Typography

Use the already bundled IBM Plex Sans and IBM Plex Mono. Ticket numbers become sans-serif.

| Role | Font | Size / line height |
|---|---|---|
| Page headline | Plex Sans 600 | 32sp / 38sp |
| App-bar and queue title | Plex Sans 600 | 20sp / 26sp |
| Section heading | Plex Sans 600 | 16sp / 22sp |
| Body and fields | Plex Sans 400 | 16sp / 24sp |
| Buttons | Plex Sans 600 | 16sp / 20sp |
| Supporting copy | Plex Sans 400 | 14sp / 20sp |
| Category and status | Plex Sans 500 | 12sp / 16sp |
| Ticket number | Plex Sans 600 | 72sp / 80sp |
| Called number | Plex Sans 600 | 88sp / 96sp |
| Numeric stat | Plex Mono 500 | 28sp / 34sp |
| Small counts and times | Plex Mono 500 | 14sp / 20sp |

Sentence case except compact category labels. Use tabular figures on counts and times.
Define theme text appearances rather than literal sizes in layouts.
Allow long titles to wrap. Never truncate a ticket identifier. At large font settings, let stats stack and rows grow.

## Layout and surfaces

- Base grid: 4dp. Spacing: 4, 8, 12, 16, 24, 32, 48dp.
- Screen margins: 20dp, reduced to 16dp on narrow displays.
- Panel padding: 16dp. Section gap: 24dp. Related-element gap: 12dp.
- Left-align text; align numeric values to a common column.
- Corner radius: 8dp for controls and panels, 16dp for sheet top corners.
- Flat opaque surfaces and thin dividers. Only modals may use subtle Material elevation.
- Plain fills replace grain, rotated catalogue cards, ticket cutouts, stamps, receipt leaders, barcodes, and perforations.
- Experimental wordmark: Plex Sans 600; a simple geometric Q may be explored separately.

## Components

**Queue rows:** white tappable surfaces, 16dp padding, category and explicit status above the queue title, verified badge when applicable, venue below, then wait/count/serving metadata. Entire row opens Queue detail. No filled Join button in list rows; joining stays in the existing detail and confirmation flow.

**Current-ticket banner:** pale blue, compact queue name, own number, ahead count, and estimated wait. Opens the current ticket. A called state uses an icon, stronger border, and the existing confirmation instruction.

**Number panel:** one cobalt rectangular panel when the main task concerns a ticket or serving number. Large white number, concise state, a thin separator, and supporting stats. A progress line must reflect real queue data; never invent a percent complete.

**Buttons:** primary cobalt/white, 56dp high; ordinary buttons at least 48dp; called "I'm here" action 64dp. One dominant primary action per screen. Secondary white buttons use outline boundaries. Destructive actions use error text and retain confirmation.

**Search and fields:** white, 52dp minimum height, 8dp corners. Persistent labels above fields; helper/error copy below. Search has a search icon and existing placeholder.

**Filters:** rounded rectangles, 40dp visual height with at least 48dp hit areas. Selected state is pale blue, cobalt text, and a check icon.

**Home navigation:** existing three destinations, white surface, thin top rule, 80dp content height plus insets. Selected icon/label cobalt with a small indicator or pale blue background. Detail screens retain back navigation.

**Action dock:** white, thin top rule, 16dp padding plus system inset; primary action spans the content width.

**Sheets/dialogs:** white surfaces, clear heading and action hierarchy. Preserve current confirmation, error, and Undo behavior. Snackbar is dark ink with white text and a clearly visible action.

**Icons:** existing Lucide outlines at 20–24dp. Actionable icons have descriptions and 48dp targets; decorative icons are excluded from accessibility traversal.

## Visual study screens

### Browse

Quapp/location header; "Find a queue"; search; categories; optional active-ticket banner; town section; queue rows; home navigation.
Sample location: Tagbilaran City.
Sample queues: Barangay clearance at Poblacion Barangay Hall, Health consultation at City Health Center.

### My ticket — Waiting

Back app bar, queue title and venue, explicit Waiting state.
Cobalt panel: Your number #43, 3 ahead, estimated wait 15 min.
Below: Now serving #39, arrival guidance, existing Bring requirements, Directions.
Leave queue remains subordinate and retains confirmation.

Sample numbers are coherent: #39 serving, #40–42 ahead, #43 yours, at 5 minutes per person. Example arrival times are illustrative.

### Live console — Confirmed

Back app bar, queue title, "Open · accepting joins", waiting/served counts.
Cobalt panel: Now serving #39, Maria Santos, confirmed state.
Served and No-show preserve existing behavior.
Up next: #40 Jose Rivera, #41 Ana Lopez, #42 Pedro Cruz; remaining rows scroll.
Bottom primary action: "Call next · #40 Jose Rivera" when allowed by the existing transition rules.

## Remaining screens and states

| Screen / state | Treatment |
|---|---|
| Called | White background, cobalt number, clear countdown, dominant "I'm here", subordinate more-time action |
| Owner home | Clean queue rows and compact cobalt live panel |
| Queue detail | White sections, aligned label/value rows, existing join preview |
| Login / register | Plain form, explicit labels, compact wordmark |
| History / insights | Aligned stats, dividers, status words; only data-backed charts |
| Outcomes | Large state icon, straightforward heading, ticket number, next action |
| Counter display | High-contrast number, cobalt accent, QR on plain white |
| Admin | Shared tokens, clean lists, explicit moderation states |
| Loading | Neutral skeletons; respect reduced motion |
| Empty | Clear reason and existing next action |
| Error / offline | Icon, message, retry when available, last-update information |
| Closed / paused | Explicit state and existing allowed actions |

## Android experiment and rollback

The document and PNG are design references; Android Studio continues to run the current Civic Paper implementation.
An implementation should be isolated on an experimental Git branch or in a separate checkout, with a clean baseline commit before UI resource changes.

Use native Java + XML views, Material 3, min SDK 24, existing data contracts and navigation.
Use resource colors/dimensions/strings and theme text appearances. Retain SystemBars insets and keyboard-aware forms.
Disable paper grain and ticket-specific shapes only within an isolated implementation of this direction.
Verify large text, TalkBack, touch targets, contrast, long titles, and Xiaomi/Pixel rendering before adopting it.

To return to the original visual specification, use `DESIGN.md`.
To return an implemented app to the original, switch to the baseline branch or revert the experimental commits; restoring a Markdown file alone does not undo app code.

## Review criteria

1. Can a queuer immediately find their number, wait, and next action?
2. Can an organizer distinguish serving, waiting, and completed states?
3. Do large numbers, long queue names, and large text fit?
4. Are location and trust information still easy to find?

## Generation record

Visual generated using the built-in image generation tool.
Full prompt: [modernist-ui-prompt.txt](design-experiment/modernist-ui-prompt.txt).
Generated text and spacing are illustrative; this document specifies the intended design.

