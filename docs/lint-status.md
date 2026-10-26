# Lint status

Snapshot from the full check on 2026-10-12. `:app:lintDebug` completed successfully with zero errors and 136 warnings. The generated report is `app/build/reports/lint-results-debug.html`.

## Largest warning groups

| Lint ID | Count | Suggested handling |
|---|---:|---|
| `UnusedAttribute` | 51 | Mostly attributes that apply above min SDK 24. Review compatibility intent before changing. |
| `UnusedResources` | 24 | Remove only after checking Java references, design variants, and generated assets. |
| `PluralsCandidate` | 17 | Convert user-facing counts when the wording actually varies by quantity. |
| `UseCompatTextViewDrawableXml` | 6 | Review compound drawable compatibility. |
| `InflateParams` | 6 | Supply the correct parent where RecyclerView/list layout parameters depend on it. |
| `NotifyDataSetChanged` | 6 | Prefer targeted adapter updates after behavior is covered. |
| `Overdraw` | 5 | Inspect paper backgrounds before removing them; the textured window ground is intentional. |
| `RtlSymmetry` | 4 | Add matching start/end handling where the asymmetry is not intentional. |
| `UseCompoundDrawables` | 4 | Consider simplifying icon-plus-text rows without reducing touch targets. |
| `GradleDependency` | 3 | Reconcile declared versions in the version catalog and module build file. |

Single warnings also include `CustomSplashScreen`, `TooManyViews`, `SetTextI18n`, `DiscouragedApi`, and target/plugin/version recommendations.

## Triage order

1. Behavior and accessibility warnings affecting real users.
2. Incorrect inflation or layout-direction handling.
3. Dependency/version duplication.
4. Adapter performance warnings.
5. Unused resources after visual regression checks.
6. Cosmetic and version-availability suggestions.

Do not suppress the whole baseline. Fix warnings in focused batches, run the full check, and verify affected screens on Pixel and Xiaomi.
