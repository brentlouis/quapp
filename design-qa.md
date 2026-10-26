# Native proposal design QA

final result: passed

Scope: compatible Java/XML translation of the approved layout specification, with existing Quapp typography, platform controls and architecture authoritative. This is not a web/Sites build or pixel-identical device-shell clone.

Source visual truth: `C:/Users/JUSKWAA/.codex/visualizations/2026/10/21/01a15e45-6d25-72a0-8b5e-d5a95b8f6b4b/quapp-onboarding-proposal/proposal-layouts.png`, 2480 × 3420 pixels. Six drawn phone panels are approximately 723 × 1448 pixels, not screenshots of a specified Android density.

Implementation: `C:/dev/quapp/build/proposal-implementation/verified-normal-screens/`, `final-register-default.png` and `final-family-row-normal.png`. Native portrait screenshots are 1080 × 2400 pixels, density 420dpi, approximately 411 × 914dp, system fontScale 1.0. Compact checks use 840 × 1680 pixels at 420dpi (320 × 640dp), fontScale 2.0. Landscape uses 1680 × 945 pixels (640 × 360dp), fontScale 1.0. CSS size/deviceScaleFactor do not apply.

Normalization: board phone panels were cropped to their frame bounds and both sources uniformly downsampled to 360px width without stretching. Heights differ because the board's drawn viewport differs from the native device. Compare composition/content and native resilience; do not infer exact pixel/line-height parity from that mismatch. System status/navigation chrome is excluded from fidelity judgments.

Full-view comparison: `build/proposal-implementation/proposal-native-comparison.png`. Six source/native pairs were opened together: `compare-welcome.png`, `compare-sign-in.png`, `compare-register.png`, `compare-preferences.png`, `compare-tickets.png`, `compare-family.png`. Registration was recaptured empty to match the board's empty state. Tickets uses the empty state; other screens show corresponding default states, with the native auth phone cursor focused. These pairs are readable at their saved size; separate region crops were unnecessary. Native full-resolution keyboard, errors, recreated, draft-review and 200% screenshots supplement them.

## Required fidelity surfaces

| Surface | Result |
| --- | --- |
| Fonts / typography | Existing native condensed headings, body roles, sp scaling and Material eye icons retained as explicitly required. Board Windows font approximations and Show text do not replace Android resources. Google action alone uses official Google Sans Medium. Hierarchy and full labels remain readable; enlarged fields scroll natively. |
| Spacing / rhythm | 24dp welcome/auth gutters and clear primary/secondary actions. Native Back remains separate from auth headings, and existing settings/Phase4 header/navigation structures remain. Compact field/action reachability and combined family disclosure were repaired. No extra slide or filler setting remains. |
| Colors / tokens | Existing Slate & Mint resources remain authoritative rather than board white approximations. Google uses the official white/ink/stroke colors. No second app theme or palette was introduced. |
| Images / assets | Existing supplied Quapp mark retained; no generated replacement. Google's current official G pixels remain untinted, proportionate and on white. No placeholder illustration, custom logo drawing or device chrome asset was added. |
| Copy / content | Core welcome wording and three choices match intent. Auth explicitly says demo; recovery/Google remain unavailable. Preferences exposes only connected choices. Family's exact planning-only sentence precedes draft entry, and its two-line ticket row says no group issued. |

## Comparison history

| Earlier finding | Fix | Post-fix evidence |
| --- | --- | --- |
| P1: compact keyboard left zero-width registration field | Existing action docks moved into native form scroll | `verified-compact200-screens/register-keyboard-confirm.png`; matching keyboard tests pass |
| P1: single-line setup removed initial password masking | Preserve TransformationMethod; disable secret and toggle saved state | Native mask/toggle/parcel/recreation assertions; `final-register-default.png` |
| P2: welcome orientation accidentally used pill geometry | Reuse existing card resource with sunken surface tint | `compare-welcome.png` |
| P2: draft-only subtitle could sit outside visible entry target | Reuse one settings-style row with title, subtitle and chevron | `final-family-row-compact200.png`, `final-family-row-landscape.png`; full-row bounds pass |

No actionable P0/P1/P2 difference remains within this adapted native scope. Expected differences: native type and chrome; existing compact settings/Phase4 structure; Google official branding absent from the wireframe but required in implementation; no phone placeholder sample; legacy normal phone height differs from the drawn board. These preserve explicit user requirements and existing work rather than imply a pixel match.

P3 follow-up: the Phase4 disclosure is clear native text rather than the board's tinted callout; existing stage buttons remain native outlined controls. No additional visual iteration is required for the requested behavior/disclosure scope.

## Interaction and evidence limits

Native tests exercise direct welcome/auth/guest navigation, completed-user migration, invalid/edited/recreated forms, real installed IME Next/Done and injected typing, password visibility, guarded failures/cancellation, Preferences/notification persistence, family review/recreation and full-row reachability. Normal, compact 200%, landscape and reduced-motion evidence is listed in `docs/onboarding-proposal-implementation.md`.

No browser console/runtime check applies to this native application. No physical hardware, spoken TalkBack, older Android, dark/RTL/localization, genuine OAuth, backend or every-route certification is claimed.

Implementation checklist: required local fixes applied; revised screenshots captured and inspected; relevant tests passed; remaining service and physical-acceptance gates documented.
