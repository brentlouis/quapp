# Design Decisions — Quapp (Public Community Queue Management)

A log of the important choices made while building this, and why. Each entry says what was decided, the reason, and what else was considered. The point is to remember *why* things are the way they are — useful for the documentation and for answering "why did you do it this way?" later.

Entries are roughly in the order they happened. Status is one of: **Current**, **Deferred to full app**, **Open**, or **Superseded**.

Where things stand: UI and navigation milestone done, plus the missing screens and features (entries from "Queue status is an enum" onward). 14 screens, in-memory data, no backend yet. The visual direction is settled: **Civic Paper** (see "Civic Paper direction (Sep 28)" below and `DESIGN.md`), and the XML rebuild starts from its foundations. Brent builds the rest; partner polishes the UI after. Remaining work continues in Claude Code.

Last updated: 2026-09-28.

---

## Scope: course project, not thesis

**Decision:** Treat Quapp as an Android course project. The thesis is a separate Hybrid RAG system.

**Why:** Quapp started as a rejected thesis title proposal and got repurposed for the Android subject. That changes what matters — the grader is looking at Android competence, not research contribution. The app has to work and look deliberate, but it doesn't have to prove the proposal's claims.

**Worth flagging:** The written proposal calls Quapp a "mobile-first queue management web application." This is native Android. If that proposal text gets reused for a submission, the wording needs to match what was actually built.

**Status:** Current.

---

## Cut the proposal down to a buildable subset

**Decision:** Build one verification strategy end to end (grace period with "I'm here" confirmation) and one forecasting method (rolling average). Document the other three strategies and the historical regression as future work.

*Superseded Sep 28 by "Scope after the redesign": grace period, cooldown and proximity are built; SMS and the regression stay future work.*

**Why:** The proposal specifies four verification strategies, two forecasting algorithms, and a simulation harness. That's thesis-sized. Grace period is the right one to keep — most visual, demos well, and needs no SMS gateway or location permissions. SMS OTP needs a paid gateway; geo-proximity needs location permissions and field testing.

**Also considered:** Building all four so the comparison could run through the UI — skipped, because the comparison doesn't need the app at all (next entry).

**Status:** Current.

---

## The comparison results come from a simulation, not the app

**Decision:** Produce the verification-strategy and forecasting comparisons with a Python script that hits the backend with generated traffic. The app demonstrates feasibility; the simulation produces the numbers.

**Why:** This is what the proposal's methodology already promised — high-volume queue scenarios with injected malicious entries, measuring prank detection rate, false positive rate, and throughput impact. None of that needs a UI. Keeping them separate avoids building four strategies into the interface just to measure them.

**Status:** Deferred to full app.

---

## No Figma

**Decision:** Skip Figma entirely. Paper sketches for hierarchy, then lock the resource files (colors, dimens, themes) before writing any layout.

**Why:** Figma's value is designer-to-developer handoff, and there's no handoff here — same person does both. The free Starter tier also caps MCP tool calls at about 6 per month, which makes any assisted workflow useless. The education plan (free Professional for verified students) was the workaround, but the case for the tool was weak to begin with. Locking resource tokens first does most of what a design file would: prevents rework, enforces consistency across screens.

**Also considered:** Using Figma as a spec only and hand-writing the XML from it — still costs time learning the tool for marginal benefit. Figma-to-XML plugins were rejected outright; they emit nested `FrameLayout` output with absolute positioning that can't be maintained or explained.

**Status:** Current.

---

## ConstraintLayout written in Code view, never the visual editor

**Decision:** All layout work happens in the XML, not the drag-and-drop designer.

**Why:** The visual editor generates conflicting constraints, zero-width views, and leaks `tools:` design-time attributes into runtime behaviour. The output is hard to read and harder to fix.

**Status:** Current.

---

## Build order: easiest screens first, RecyclerView in the middle

**Decision:** Splash → Login → Register → Role Select → Queue Detail → Active Ticket → Create Queue → Browse → Owner Dashboard → Live Console.

**Why:** The first four are familiar LinearLayout and Intent work, so four screens get done in one sitting and the conventions get established. RecyclerView is the one genuinely new concept, so it lands at Browse — with momentum built up, not as the first thing encountered. By Dashboard and Live Console it's old news and both go fast.

**Also considered:** Building Browse first since it's the main screen — skipped, because meeting RecyclerView cold on screen one would have been the hardest possible start.

**Status:** Current.

---

## Backend over local storage

**Decision:** FastAPI backend with Retrofit on the client, not SQLite or Room.

**Why:** A queue that only exists on one phone can't be demoed properly. With a backend the instructor can join from their own phone while the Live Console runs on another, which is the whole point of the app. FastAPI is also already familiar from the RAG project, making it the least-new part of the remaining work — and the Quapp backend is simpler: no embeddings, no vector store, just endpoints reading and writing a table.

~~**Known constraint:** The server runs on a laptop's LAN IP, so both devices need to be on the same Wi-Fi during a demo.~~ Superseded — see "Backend doesn't need shared Wi-Fi" below.

**Status:** Deferred to full app.

---

## UI before backend

**Decision:** Build all the screens against hardcoded data first, wire up the network later.

**Why:** Screens don't care where data comes from. Building against a `FakeData` class means the source can be swapped without touching a single layout. It also matched the actual deadline, which was navigation only.

**Status:** Current.

---

## Custom teal palette, not stock Material purple

**Decision:** Primary is `teal_40` (#0E5A52), with a full set of supporting colors.

**Why:** Stock Material purple (#6200EE and friends) reads instantly as an untouched template. Defining a real palette — even a small considered one — is the difference between a screen that looks designed and one that looks generated.

**Status:** Superseded (design) by "Monochrome visual direction replaces the teal palette". Code unchanged until the rebuild.

---

## Semantic color roles instead of direct color references

**Decision:** Use `?attr/colorPrimary`, `?attr/colorOnSurface` and so on in layouts, not `@color/teal_40`.

**Why:** A direct reference says "this specific color." A role says "whatever this theme uses for this purpose." Roles travel — change the palette or add dark mode and every view using a role updates itself. This turned out to be why dark mode mostly worked with no extra effort once the theme was fixed.

**Two deliberate exceptions:**

- The four queue status colors (waiting, called, served, expired) stay direct `@color/` references, because they carry app meaning, not theme meaning — Material has no role for "this queuer is being called right now." Consequence: they don't adapt between light and dark, so their contrast on dark surfaces still needs checking.
- The Splash screen hardcodes `teal_40` and white, because a splash is a fixed brand moment with no content to make readable. Under the tonal system it flips to pale teal in dark mode, which breaks the brand.

**Status:** Current, amended by Civic Paper: the exceptions are now the status colours (`ok`, `warn`, `err`), `crema` and `signal`, and the spotlight gets its colours from a theme overlay. The Splash exception is gone (see "One logo: the ticket mark").

---

## Type through textAppearance roles, no hand-set text sizes

**Decision:** Use `?attr/textAppearanceTitleMedium` and friends. No `textSize` anywhere, and no text sizes in `dimens.xml` at all.

**Why:** The named Material roles bundle size, weight and letter-spacing as one considered set. Hand-setting `textSize` is one of the clearest tells that a layout was assembled rather than designed, and it drifts across screens.

**Status:** Current. Civic Paper adds three custom theme attributes (`textAppearanceTicketNumber`, `textAppearanceStat`, `textAppearanceStatHero`), so the rule still holds: layouts only reference `?attr/textAppearance…`.

---

## 8dp spacing grid

**Decision:** Five tokens — `space_xs` 4, `space_sm` 8, `space_md` 16, `space_lg` 24, `space_xl` 32. 4dp only for tight optical gaps like icon-to-label. 16dp is the standard screen edge margin.

**Why:** If only these five get used, screens line up consistently without thinking about it. Defining them as dimens means changing the rhythm is one edit, not forty.

**Status:** Current, amended by "Spacing gets a 12dp step" (Civic Paper adds `space_ms`).

---

## One filled button per screen

**Decision:** Filled = the single primary action. Tonal = important but secondary. Outlined = secondary. Text = cancel, dismiss, or navigate away.

**Why:** Button emphasis carries meaning. Two filled buttons on a screen means neither reads as primary.

**Deliberate exception:** Role Select has two equal-weight cards, neither filled. That screen exists *to* present a fork — making one filled would imply a default that doesn't exist.

**Status:** Current.

---

## Cards only for discrete tappable objects

**Decision:** Cards on Browse rows, Dashboard rows, and Role Select options. No cards on form fields or Live Console waiting rows. Outlined by default; filled only for the Live Console "now serving" panel.

**Why:** A card means "this is a thing you tap into." A form field isn't one. The Live Console waiting list is a working list an owner stares at for hours — wrapping each person in a card would be pure noise. The now-serving panel is the one place a filled card earns it: a status readout that needs separating from the list below without a border.

**Status:** Current.

---

## ConstraintLayout for screens, LinearLayout for short linear runs

**Decision:** ConstraintLayout at screen level. LinearLayout for genuinely linear runs of two to four views, including `layout_weight` for equal columns.

**Why:** The rule isn't "never LinearLayout" — it's "not as a nesting tool." Two stat columns splitting the width with `weight="1"` is exactly what weight is for. Nested weights are the thing to avoid.

**Note:** In ConstraintLayout, `layout_width="0dp"` means "match constraints," not zero. Using `match_parent` there ignores the constraint system and overflows padding.

**Status:** Current.

---

## Handle window insets manually with a SystemBars helper

**Decision:** A small helper that reads window insets and adds them to whatever padding the layout already declared. Applied to every screen's root view.

**Why:** From Android 15, apps draw edge-to-edge by default and `android:statusBarColor` is ignored. Content draws behind the status and navigation bars unless insets are handled — the first Login build had the register link jammed against the nav bar because of exactly this.

The base padding gets captured once, before the listener runs. Reading `getPaddingTop()` inside the listener would return the already-adjusted value and compound on every rotation.

**Also considered:** `fitsSystemWindows="true"`, the one-line fix that turns up first — it *sets* padding rather than adding to it, so it would wipe the layout's own 16dp and push the form flush to the screen edges.

**Status:** Current.

---

## Use systemBars() insets, not tappableElement()

**Decision:** Pad content clear of the full system bar height.

**Why:** `tappableElement()` returns 0 at the bottom under gesture navigation, letting content sit behind the pill. That's right for immersive screens — a feed, a photo viewer. Quapp's screens are forms and lists ending in buttons, and a "Log in" button partly behind a gesture pill looks like a bug rather than a choice.

**Also considered:** Detecting whether the phone uses buttons or gestures and branching on it — unnecessary, because the insets already encode the answer (~48dp for buttons, ~24dp for gesture, side insets in landscape). Branching would mean hardcoding numbers the system already reports, and getting them wrong on some OEM skin.

**Status:** Current.

---

## A second helper method for keyboard insets

**Decision:** `applyPaddingWithKeyboard` ORs `Type.ime()` together with `Type.systemBars()`, used on form screens.

**Why:** Without it, tapping the last field on Register puts the keyboard over the Create Account button. The bitwise OR returns the union — the larger of the two per edge, never the sum — so an open keyboard that already covers the nav bar doesn't get counted twice.

**Status:** Current.

---

## Back stack: finish() to go back, startActivity to go forward

**Decision:** Even when both would land on the same screen. Register's "Already have an account" link calls `finish()`, revealing the Login instance already underneath.

**Why:** Activities stack rather than swap. Using `startActivity` to "return" to Login pushes a second instance — Login, Register, Login — and the user walks backward through their own history pressing Back. Splash calls `finish()` for the same reason, so it's never reachable by Back.

**Status:** Current.

---

## Pass ids between Activities, not objects

**Decision:** Queue Detail receives `EXTRA_QUEUE_ID` and looks the queue up itself.

**Why:** Sending the whole object needs `Parcelable` boilerplate. Passing an id is the better pattern anyway — with a backend, the destination fetches fresh data instead of trusting a possibly-stale snapshot from the list.

The extra keys are `public static final String` constants on the *receiving* Activity, package-prefixed. The receiver owns the contract of what it needs. A typo'd string key fails silently at runtime; a constant fails at compile time.

**Status:** Current.

---

## Join is one screen with toggled sections, not a chain

**Decision:** One `JoinQueueActivity` that shows whichever verification requirements the queue has enabled, rather than chaining separate screens per strategy.

**Why:** The proposal's contribution is *configurable combinations*. A chain would mean the flow length changes per queue, which is confusing, and it would need four more Activities. One screen saying "here's everything this queue requires" matches the idea better.

**Status:** Current.

---

## RecyclerView for every list

**Decision:** No `ScrollView` containing a `LinearLayout` filled from code, even for lists of three.

**Why:** Only about eight rows fit on screen at once, so RecyclerView builds eight views and recycles them as they scroll. The ViewHolder caches `findViewById` results once at creation — running those lookups six times per row at 60fps is the difference between smooth and stuttering.

**Also considered:** Faking the Browse list with stacked LinearLayouts using only already-familiar components — skipped, because the backend would force a rebuild in a few weeks anyway, and learning RecyclerView with no deadline pressure was the better trade.

**Notes worth keeping:** `onCreateViewHolder` runs rarely, `onBindViewHolder` runs constantly and must stay cheap. Click listeners get set in `bind`, not in the ViewHolder constructor — ViewHolders are reused, so capturing the item at creation means row 1's view opens row 1's detail after being recycled as row 20.

**Status:** Current.

---

## Adapter click handling through an interface

**Decision:** Each adapter declares a listener interface; the Activity implements it and decides what a tap does.

**Why:** The adapter shouldn't know that tapping a queue opens Queue Detail. Same separation as a FastAPI endpoint not knowing what the Android client does with its response.

**Status:** Current.

---

## notifyDataSetChanged for now

**Decision:** Refresh the whole list on any change. `DiffUtil` deferred.

**Why:** Blunt but correct, and fine while data is hardcoded. `DiffUtil` earns its keep when lists update incrementally from the network, which is a backend-phase problem.

**Status:** Deferred to full app.

---

## Queue model: 16 immutable fields

**Decision:** All fields `final`, no setters. Grouped as identity/location, descriptive, live state, verification config.

**Why:** A `Queue` is a snapshot of server state. If it can't be mutated after construction, nothing can quietly change it mid-scroll. With a backend, the whole object gets replaced rather than edited in place — simpler to reason about, and it removes a class of bug.

**Known hazard:** The constructor ends with four consecutive booleans, so swapping two compiles silently. Acceptable while `FakeData` is the only caller, but before the backend this wants either a Builder or a small `VerificationConfig` object holding the four.

**Status:** Superseded — see "Queue status is an enum, and Queue gets a Builder."

---

## Municipality is a String, but picked from a fixed list

**Decision:** String in the model, populated from a fixed array in `res/values/arrays.xml` via a dropdown in Create Queue. Owners pick, they don't type.

**Why:** Free-text venue names can't be filtered — two municipalities both have a Poblacion. The fixed picker is what makes location filtering possible at all. Same reasoning as PetMatch using structured fields instead of free-text descriptions.

**Also considered:** An enum, which is stricter — skipped because enums are awkward to serialize with Retrofit and it would hardcode Bohol's 47 municipalities into Java.

**Status:** Current.

---

## Coordinates as double, separate from the proximity toggle

**Decision:** `latitude` and `longitude` are non-nullable doubles. Whether the proximity rule is enforced is a separate boolean, `proximityCheckEnabled`.

**Why:** Every queue has a location; enforcement is the owner's choice. The coordinates describe *where the queue is*, the boolean describes *a rule* — different concerns, different fields.

On `double` specifically: `float` gives about 7 significant digits, and a Philippine latitude like 9.6496543 already uses 8, putting precision loss at roughly metre scale. Irrelevant for a 2km radius test, but `double` costs 4 extra bytes and removes the question entirely.

**Also considered:** `BigDecimal` (that's for money, where representation errors compound through arithmetic) and `android.location.Location` (carries accuracy, altitude, speed, provider — none of which a fixed venue has).

**Status:** Current.

---

## Ticket as a separate model with a Status enum

**Decision:** `Ticket.Status` is an enum with `WAITING`, `CALLED`, `SERVED`, `NO_SHOW` — mapping exactly onto the four status colors.

**Why:** A String status would let "waiting", "Waiting" and "WAITNG" all compile. An enum makes the valid set a compile-time fact. Nested inside `Ticket` because a status has no meaning apart from a ticket.

Also worth noting: `ticketNumber` and `position` are different fields on purpose. The number is fixed — you're #47 forever. The position counts down as the queue advances. Active Ticket shows the number large and the position as the live figure.

**Status:** Current.

---

## Two-person team: Brent builds, partner polishes the UI

**Decision:** Brent finishes the remaining screens and the FastAPI backend, and wires everything himself. The partner then takes over UI improvement only — visual polish on screens that already work.

**Why:** Brent now has enough time to build the rest himself, so there's no need to split screen-building across two people. The partner inherits finished, wired screens and can improve them without inventing data shapes or touching the network layer. Replaces the earlier plan (partner builds the frontend, Brent wires it), which needed shared rules and a repository interface just to stop wiring from turning into rewriting.

**Also considered:** Keeping the original frontend/backend split — more coordination overhead for the same result.

**Watch:** Polish means editing existing layouts and the shared theme files, which is where merge conflicts happen. The partner starts after the screens are merged, or owns `themes.xml`, `colors.xml` and `dimens.xml` outright. Also worth confirming the instructor considers UI improvement a sufficient individual contribution.

**Status:** Current.

---

## Backend doesn't need shared Wi-Fi

**Decision:** Drop the "both devices on the same Wi-Fi" constraint from the backend entry.

**Why:** The RAG project's FastAPI server was reachable over mobile data from a different network, so the original constraint was wrong. The server also runs on a desktop, not a laptop.

**Status:** Current. Supersedes the known constraint under "Backend over local storage."

---

## Stick with XML views

**Decision:** Keep Java + XML views. No switch to Compose or a cross-platform framework.

**Why:** For a list-and-form app like Quapp, XML views reach the same visual quality as Compose — the look comes from design decisions (palette, type scale, spacing, states), not the toolkit. XML costs more effort for animations and custom drawing, and the newest Material components land in Compose first, but nothing Quapp needs is blocked.

**Also considered:** Compose — easier animation and state-driven UI, but Kotlin and outside the course stack. Flutter or React Native — not native views.

**Status:** Current.

---

## Queue status is an enum, and Queue gets a Builder

**Decision:** Replace `boolean open` with `Queue.Status` (`OPEN`, `PAUSED`, `CLOSED`). Replace the 16-argument constructor with `Queue.Builder` and `toBuilder()`.

**Why:** Pause and close aren't the same thing. Paused means "no new joins, still serving the line"; closed means "done, line released." One boolean can't hold three states. It's the same reasoning as `Ticket.Status`: an enum makes the valid set a compile-time fact. `isOpen()` stays, so code that only asks "can I join?" didn't change.

The Builder came at the same time because Edit Queue has to rebuild a Queue from form values, which would have meant calling the constructor with four booleans in a row — the exact hazard the model entry warned about. `.setNoShowPenaltyEnabled(true)` can't be swapped by accident. `toBuilder()` makes "same queue, one thing changed" a one-liner.

**Worth flagging:** This changes the future API contract (`status` string instead of `open` boolean). CLAUDE.md says to propose model changes first; this one was made during the "do all" pass and needs Brent's sign-off.

**Also considered:** A second boolean `paused` — two booleans allow the impossible "closed and paused," which the enum rules out.

**Status:** Current. Approved with MODELS.md (Sep 28), which adds `UPCOMING`; `isOpen()` became `acceptsJoins()`.

---

## FakeData becomes an in-memory store

**Decision:** `FakeData` keeps queues, each queue's waiting list, served/no-show counts, and the queuer's history in static fields for as long as the process lives. Screens read and write through methods like `callNext`, `markServed`, `addWalkIn`, `saveQueue`, `setQueueStatus`.

**Why:** Edit, pause, walk-ins and analytics all need changes to stick. Before, every call rebuilt the data from scratch, so an edit would vanish the moment the screen closed. Each method is shaped like the endpoint it'll become, so the Retrofit swap is method-for-method.

Side effects worth knowing: waiting counts on Browse now come from the real waiting list (Barangay Relief has 42 actual tickets, not a hardcoded 42), and removal is by ticket id, which retires known compromise #7.

**Also considered:** The `QueueRepository` interface from Open questions — still worth doing before the backend; this is a step toward it, not a replacement.

**Status:** Current. Deferred to full app for the backend swap.

---

## Rolling-average forecast runs client-side for now

**Decision:** Each queue keeps a `RollingAverage` of the last 5 service times. ETA = people ahead × average. It drives Browse, Queue Detail and Insights.

**Why:** Rolling average is the forecasting method in scope, and it's small enough to show working now. A service time is the gap between two consecutive completions — the first completion of a session only starts the clock, since there's no earlier event to measure from. Seed samples are derived from each queue's original fake ETA so the numbers don't jump on first launch. With no data at all, a 5-minute default stands in, and Insights says so.

`RollingAverage` is plain Java with unit tests, so it moves to the FastAPI side as a straight port.

**Status:** Current. Moves server-side with the backend.

---

## No-show cooldown: 2 strikes, 30 minutes, penalty queues only

**Decision:** Two no-shows on queues with the no-show penalty turned on start a 30-minute cooldown. During it, those queues refuse joins; queues without the penalty ignore it.

**Why:** The penalty is a per-queue setting, so only queues that opted in should count strikes or enforce the block. Two strikes means one missed call — a dead phone, a bathroom break — only earns a warning. 30 minutes is long enough to deter prank joining and short enough not to lock someone out of a whole relief distribution.

The grace deadline moved from the Activity's timer into `ActiveTicketStore`. Otherwise leaving Active Ticket would pause the clock, and a queuer could dodge the window by never looking at the screen.

**Known limit:** In-memory, so force-closing the app clears it. Enforcement belongs on the server, keyed by phone number.

**Also considered:** Counting every no-show regardless of queue — would punish people on queues whose owners chose not to penalise.

**Status:** Current. Numbers are placeholders for the evaluation to tune.

---

## Session in SharedPreferences, role remembered

**Decision:** `Session` stores logged-in flag, name, phone and role in SharedPreferences. Splash routes straight to Browse or Dashboard when a session exists. Login, register, role pick, role switch and logout all clear the back stack (`NEW_TASK | CLEAR_TASK`).

**Why:** SharedPreferences is the standard place for a few small key-value settings, and doesn't conflict with the no-Room/SQLite rule. Clearing the stack is the same principle as Splash calling `finish()`: after logging in, Back shouldn't return to Login; after logging out, Back shouldn't return to the account.

**Worth flagging:** No backend means login accepts any valid phone and non-empty password. Passwords are never stored. Role switching moved from Role Select into Profile, since Role Select now only appears once per login.

**Status:** Current. The auth check moves to the backend.

---

## Browse filters: chips for category, a picker for location

**Decision:** A search field plus a chip row: one location chip that opens a single-choice dialog, then single-select category chips generated from `@array/queue_categories`. Filtering lives in `QueueFilter`, not the Activity.

**Why:** Five categories fit as chips; ten municipalities (47 eventually) don't, so location gets a picker behind one chip. Generating chips from the same array Create Queue uses keeps one source of truth. `QueueFilter` is plain Java so it's unit tested. The empty state offers "Clear filters" instead of a dead end.

**Status:** Current.

---

## Active-ticket card on Browse

**Decision:** When the queuer holds a ticket, an outlined card sits above the search field showing number, queue and status with a status-color dot. Tapping it opens Active Ticket. Queue Detail also shows "View your ticket" for that queue, and blocks joining a second queue while one is live.

**Why:** It fixes the biggest functional gap — leaving the ticket screen used to strand the ticket. One active ticket at a time matches `ActiveTicketStore` holding one ticket, and stops one person holding places in several lines.

**Status:** Current.

---

## Walk-in add, pause/close, edit live in the Live Console menu

**Decision:** An overflow menu on the Live Console holds Insights, Edit details, Pause/Resume, and Close/Reopen, showing only the moves valid from the current status. "Add walk-in" is a text button beside the waiting count. Closing asks for confirmation; pausing doesn't.

**Why:** The console is where an owner already is when they need these. Call Next stays the one filled button. Closing releases everyone in line, which can't be undone, so it confirms; pausing is one tap to reverse. Walk-in tickets have an empty phone and show "Walk-in" in its place.

**Status:** Current.

---

## Design pass: from "correct Material" to a point of view

A critique found the screens rule-clean but generic: every screen was a title, label-over-value pairs, and a full-width button. The entries below are the fixes. Partner polishes further from here.

---

## Barlow SemiBold for display and headline roles

**Decision:** Bundle `res/font/barlow_semibold.ttf` (Google Fonts, OFL) and point `textAppearanceDisplay*` and `textAppearanceHeadline*` at it in `Base.Theme.Quapp`. Body, title and label roles stay Roboto. Numbers that change in place (countdown, counts, stats) also set `android:fontFeatureSettings="tnum"`.

**Why:** A queue app lives on numbers, and the stock Roboto numerals were the main reason every screen looked like a template. Barlow's slightly condensed shapes read like transit and counter displays. Changing the roles in the theme keeps the "`?attr/textAppearance*` only" rule intact, so no layout sets a font directly. Barlow's default figures are proportional (a "1" is narrower than a "0"), so a ticking `02:47` would jitter without `tnum`, which switches to fixed-width digits.

**Also considered:** Downloadable fonts through Google Play services, which adds a provider certificate and a failure path for an 86 KB file. Space Grotesk, a common default that reads as generic now. A variable font, which only renders its default weight below API 26 (min SDK is 24).

**Note:** `Base.Theme.Quapp` now exists on purpose, in `values/` only. Both the light and night `Theme.Quapp` use it as their parent. This isn't a repeat of the earlier dark-mode bug, because the manifest still points at `Theme.Quapp`.

**Status:** Superseded (design) by "Monochrome visual direction replaces the teal palette". Code unchanged until the rebuild.

---

## Active Ticket is one status-colored panel

**Decision:** The ticket screen is a large rounded panel between the header and the buttons. Waiting uses `colorPrimaryContainer` and leads with "N ahead of you" plus the ETA. Called fills the panel with `status_called`, shows a draining ring around the countdown, and switches to the filled "I'm here" button. Served and expired fill it with their status color and a headline. The ticket number sits at the bottom like a stub. "Leave queue" is an outlined button that only shows while waiting.

**Why:** This is the screen the app exists for, and the grace period is its anti-prank feature. The old layout gave the ticket number (43) and the position (42) the same weight, so nobody knew which to watch, and "called" looked almost the same as "waiting". Filling the panel with the status color makes the state change visible across the room. Leaving a queue was the filled primary action while waiting, which is backwards for a destructive action.

**Also considered:** A "Now serving #31 → you #43" track, which would be the clearest display. It needs a now-serving number on the queuer's side that agrees with their ticket number. `JoinQueueActivity` currently numbers the ticket `peopleWaiting + 1`, which doesn't match `FakeData`'s own numbering. It's a model/API change to propose, not a UI one.

**Status:** Superseded (design) by "Ticket outcome screens: white card, color only on small elements". Code unchanged until the rebuild.

---

## Queue status is a pill in theme colors, not ticket colors

**Decision:** On Browse cards, Open is a `colorPrimaryContainer` pill. Paused and Closed are neutral `colorSurfaceVariant` pills. The ETA is the largest text on the card (HeadlineMedium, "min wait" beside it). Category moves into the venue line.

**Why:** The card used `status_waiting` blue for "OPEN" and `status_called` orange for "PAUSED". Those colors mean things about *your ticket*, so reusing them for a *queue's* state blurred both. Wait time is the number people pick a queue by, and it was the same size as the venue.

**Status:** Superseded (design) by "Monochrome visual direction replaces the teal palette". Code unchanged until the rebuild.

---

## `status_called` darkened to #BF360C

**Decision:** From #E65100 to #BF360C (Material deep orange 900).

**Why:** Active Ticket now puts white text on the status colors. White on #E65100 is about 3.8:1, below the 4.5:1 floor. #BF360C gives about 5.6:1 and still reads as orange. The other three status colors already pass with white.

**Status:** Superseded (design) by "Status colors: waiting has no color, the others are muted". Code unchanged until the rebuild.

---

## Palette fixes: secondary roles and status bar

**Decision:** Give the secondary roles their own slate tones (`slate_90/10` light, `slate_80/20/30` dark) instead of reusing teal. Set the status bar to the surface color in both themes. Splash keeps `teal_40`.

**Why:** Three bugs. In dark mode, `colorSecondary` was `slate_40` on a near-black surface, about 3:1 contrast. `colorSecondaryContainer` equaled `colorPrimaryContainer`, so tonal elements couldn't be told apart from primary ones. And in light mode, the status bar was dark teal with `windowLightStatusBar=true`, giving dark icons on dark teal. Android 15+ ignores `statusBarColor` because it forces edge-to-edge, so this only showed on older phones.

**Status:** Superseded (design) by "Monochrome visual direction replaces the teal palette". Code unchanged until the rebuild.

---

## Live Console rows: one overflow menu instead of two buttons

**Decision:** Each waiting row has a single ⋮ button opening a popup with Served / No-show.

**Why:** Eight people waiting meant sixteen identical text buttons, and the list read as a wall of actions. The owner's main loop is Call next. Per-row actions are occasional, so they belong one tap deeper.

**Status:** Superseded (design) by "Live Console: the outcome lives on the Now Serving card". Code unchanged until the rebuild.

---

## Design overhaul on the Design canvas (Sep 26–27)

The screens were redesigned as mockups on a Claude Design canvas ("Quapp UI overhaul"), across two chats. Nothing below is built yet. The entries record what was decided so the XML rebuild follows one plan. Where an entry replaces an earlier one, the old entry is marked Superseded, but the code keeps the old behaviour until it's rebuilt.

---

## Monochrome visual direction replaces the teal palette

**Decision:** Black, white and grays, based on the DESIGN2 style reference (shadcn/ui-like). Ink `#0A0A0A`, filled buttons `#171717`, muted text `#737373` (`#666666` on gray), hairlines `#E5E5E5`, page background `#EEEEEE`, cards `#FFFFFF`. Font is Geist (400/500/600) instead of Barlow. Cards use 24dp corners, inputs 18dp, and buttons are pills. Buttons stay 48dp tall, not DESIGN2's 36px.

**Why:** The teal version looked correct but generic. A strict monochrome base makes the few status colors carry meaning, and it reads as deliberate.

**Also considered:** Keeping teal as the brand color alongside the status colors. Rejected: two chromatic systems competed.

**Status:** Superseded (design) by "Now Serving replaces the monochrome direction" (Sep 28). It had superseded "Custom teal palette", "Barlow SemiBold", "Palette fixes" and "Queue status is a pill in theme colors".

---

## Depth: one black card per screen, three card levels

**Decision:** Each screen gets at most one black card holding the thing to look at first: your ticket on Browse, the live queue on the owner home, Now Serving on the console, the stats on Queue Detail. Other cards come in three levels: raised (white with a soft shadow, for tappable items), flat (white with no border, for information), and faded (outline only, for closed or paused items). Units and labels use weight 400; numbers use 600. Sections are 32dp apart and items inside a section 8–12dp apart.

**Why:** The first monochrome pass was clean but flat, because every card had the same weight and the page and card colors were nearly identical.

**Status:** Superseded (design) by "Now Serving replaces the monochrome direction" (Sep 28).

---

## Status colors: waiting has no color, the others are muted

**Decision:**
- Waiting: ink, no color. Waiting is the normal state.
- Called: `#C2410C`, with tint `#FFF7ED`.
- Served: `#2E7D32`.
- Slot released and destructive actions: `#B71C1C`. One red covers both, instead of the old separate expired red and destructive red.

On black cards, the called orange is only used for dots and bars; text stays white.

**Why:** The old Material colors fought the black-and-white base. Merging the two reds removes a near-duplicate.

**Note:** Every canvas screen now uses the final shades. The cooldown screen lost its full red panel: a white card with a red icon and a thin red progress bar.

**Status:** Superseded (design) by "Now Serving replaces the monochrome direction" (Sep 28). The app is dark-only now, so no night variants are needed.

---

## Ticket outcome screens: white card, color only on small elements

**Decision:** Called, Served, Slot released and Queue closed all use a white ticket card sized to its content on the gray page. Color is limited to an outlined icon ring, a small status label, one warning line and, where it's the main action, the button. That keeps color under about 10% of the screen. The called screen puts `#43` first (the number staff shout), shows the countdown in a thin orange ring, and uses an orange "I'm here" button. Queue closed has no status color at all.

**Why:** Full-screen colored panels felt like a visual hammer against the monochrome base.

**Trade-off:** The called state is no longer visible from across a room. It relies on the notification and the orange button. Test on a phone outdoors; if it's too quiet, add a slow pulse on the ring rather than bringing back the full panel.

**Status:** Current (design) for served, slot released and queue closed. The called screen is amended by "The called screen is the one loud moment" (Sep 28). Supersedes "Active Ticket is one status-colored panel".

---

## A fifth ticket status for queues the owner closes

**Decision:** Add `QUEUE_CLOSED` to `Ticket.Status`. When an owner closes a queue, every ticket still `WAITING` or `CALLED` becomes `QUEUE_CLOSED`. It never calls `Cooldown.recordNoShow()`. History shows it as "Queue closed" in ink, and the ticket screen says "This doesn't count as a no-show."

**Why:** With only four statuses, owner-closed tickets would have to be `NO_SHOW`, which feeds the cooldown and punishes people for the owner's decision, or be deleted, which loses the history row. It also keeps owner closures out of the no-show rate the evaluation measures. `RELEASED` was rejected because the UI already says "Slot released" for `NO_SHOW`. `CANCELLED` was rejected because it sounds like the queuer cancelled.

**Knock-on:** `HistoryAdapter.bind` treats anything that isn't `NO_SHOW` as served. It needs a `switch` over every status, or closed tickets show as "Served".

**Status:** Current. Not built.

---

## Live Console: the outcome lives on the Now Serving card

**Decision:** Served / No-show move off the waiting rows and onto the Now Serving card, since you can only serve or no-show the person you called. The card shows the grace countdown and has three states: awaiting "I'm here", confirmed (Served · call next), and timed out (slot released, recorded as a no-show, Call next). The Call next button names who is next. Queue options (Extend closing time, Edit details, Insights, Pause new joins, Close queue now) open as a bottom sheet.

**Why:** Actions on waiting rows let the owner no-show someone who was never called, and nothing showed whether the grace period did anything.

**Status:** Current (design). Supersedes "Live Console rows: one overflow menu instead of two buttons".

---

## Queue statuses gain UPCOMING; queues open and close on schedule

**Decision:** Four queue statuses:
- Upcoming: scheduled but not open yet. People can join early and hold a number.
- Open: within hours.
- Paused: the owner stopped new joins; people already in line keep their spot.
- Closed: ended on schedule or closed early.

Queues open and close automatically from their schedule. Queuer Browse has no status filter: it sorts Open, then Upcoming, then Paused, and hides Closed (they still appear in History). The owner dashboard has status filter chips with counts.

**Why:** Once queues have real dates, "not started yet" is a state people need to see and join.

**Status:** Current (design). Adds a value to `Queue.Status`, so it amends "Queue status is an enum, and Queue gets a Builder". Automatic open and close needs the backend.

---

## Schedule replaces free-text service hours

**Decision:** Create Queue has a Schedule section with a One day / Multiple days switch, start and end dates, and opening and closing times, plus a summary line ("2 days, 8:00 AM – 4:00 PM each day"). Extending the closing time is a separate sheet (+30 min, +1 h, +2 h or a custom time) and notifies everyone waiting.

**Why:** Dates on cards, "Ends in 5 h", "Day 1 of 2", automatic open and close and Upcoming all need real times. This resolves known compromise #4.

**Status:** Current (design). Model change: `serviceHours` becomes start date, end date, opening time and closing time. Decide before the backend.

---

## Categories: eight, with "Other" instead of "Community"

**Decision:** Relief, Medical, Government, Education, Bills & Payments, IDs & Registration, Jobs, Other. They're picked from a bottom sheet with an icon and a one-line hint for each.

**Why:** Resolves the open question about Community: it was a catch-all, and "Other" says so honestly. The new categories cover common Bohol queues that had no clear home.

**Status:** Current (design). Update `arrays.xml`.

---

## Verification in Create Queue matches what's built, plus proximity

**Decision:**
- Presence confirmation and No-show cooldown are switches.
- SMS confirmation shows "Planned".
- Proximity check is a switch with a per-queue join radius (500 m / 1 km / 2 km / 5 km) and a venue pin.

**Why:** Showing unbuilt checks as working invites "show me" at the defense. Proximity was requested back into scope, and per-queue radius resolves the open question about the radius.

**Status:** Current (Sep 28, see "Scope after the redesign"). It needs location permission, a map picker and field testing. Mocked since: Queue Detail lists the rule ("Join within 1 km"); tapping Join opens a sheet explaining the check before Android's location prompt; outside the radius, Queue Detail shows "You're 4.2 km away" with Directions and Check again. Location is read once at join time, with no background tracking.

---

## Queue cards carry dates, description and live numbers

**Decision:**
- Queuer card: category icon, name, status, category and venue, a description cut to 2 lines, a date and time row with an "Ends in…" note, and estimated wait / in line / now serving.
- Owner card: the same, with a 1-line description, plus waiting / served / no-shows / minutes per person.

**Why:** The instructor asked for the description on cards. Dates and live numbers let people decide before opening the queue.

**Status:** Current (design). "Now serving" needs the current ticket number in the Browse data.

---

## Bottom navigation, landing states and first visit

**Decision:**
- Queuer tabs: Browse · My tickets · Profile.
- Owner tabs: Queues · Today · Profile.
- The queuer home greets by name. On first visit it asks "Which town are you in?"; then it shows the town and the shortest wait nearby. In line, your ticket takes that slot. Search and filters are always present.
- The owner home puts the live queue first. On first visit it shows a three-step card (Create, Share, Call people forward). All-queue totals moved to the Today tab.
- Role Select leads to these first-visit states.

**Why:** The old landing screens looked like any sub-screen and dropped users into a list with no orientation.

**Navigation structure (decided Sep 27): hybrid.** Only the tab screens become Fragments. `QueuerHomeActivity` hosts Browse, My tickets and Profile; `OwnerHomeActivity` hosts Queues, Today and Profile. Each host has one `BottomNavigationView` and a `FragmentContainerView`. Every other screen (Queue Detail, Join, ticket screens, Create Queue, Live Console) stays an Activity, opened with `startActivity()` and Intent extras as now. Tabs switch with `FragmentManager` `show()`/`hide()`, so Browse keeps its list and scroll position. No Navigation Component.

**Why hybrid:** Separate Activities per tab rebuild the nav bar and reload the tab on every tap. A full single-Activity app would be a rewrite. The hybrid limits Fragments to three screens per role.

**Trade-off:** Fragments bring a second lifecycle (`onCreateView`/`onViewCreated`, `requireContext()`, checking `isAdded()` before touching views in a late Retrofit callback). Brent is time-constrained, so this is built from a guide rather than learned in depth first.

**Status:** Current. Both homes built Sep 28 (`QueuerHomeActivity`, `OwnerHomeActivity`); the old `BrowseActivity`, `OwnerDashboardActivity` and the temporary `ProfileActivity` are gone. The tab contents are ported as-is; their Civic Paper redesign comes screen by screen.

---

## More than one ticket, if the hours don't overlap

**Decision:** A queuer can hold tickets in more than one queue only if their hours don't overlap. With one ticket, the My tickets tab opens straight to it. With two or more, it opens a list: a ticket being called sorts to the top with the orange accent, and the rest sort by start time. The tab badge shows the count.

**Why:** Once queues have real schedules, a morning and an afternoon queue on the same day is legitimate.

**Status:** Current. Built Sep 29 (My tickets list, overlap check on Queue detail, the count on the tab badge); the old "already in a queue" rule is gone. Overlap is checked per day, see "A ticket is for one day".

---

## New screens from the redesign

Notification permission (explained before Android asks), the "you're being called" notification with an "I'm here" button, the Served / Slot released / Queue closed / Offline ticket states, Leave queue confirmation, Queue detail on cooldown, Share queue (link and QR code), and the counter display (landscape, numbers only, no names).

**Status:** Current (design).

---

## Guest browsing, and four states for Browse

**Decision:**
- Login gets a "Browse queues first" button. Guests see the full Browse list, with Log in at the top and a Sign up bar in place of the tab bar.
- Join is the gate. Tapping it opens a sheet ("Sign up to join this queue") that says why an account is needed: notifications, and one account per phone to keep fake joins out. After signing up, the user returns to that queue's Join screen, not to Browse. Register skips Role Select in this case and the Join screen shows "Account created. Confirm below to take your spot." The queue ID travels through Intent extras.
- Browse has loading (skeleton cards), empty town (offers the nearest town with queues), no results (names the query and filter, one Clear filters button) and error (keeps the cached ticket card, Retry, last-updated time).
- The tab is "My tickets" everywhere.

**Why:** Asking for an account before showing anything loses first-timers. Keeping the gate at Join still ties every ticket to an account, which the prank-queuing checks depend on.

**Status:** Open (design). Guest browsing needs the Browse endpoint to work without a login token. Decide before the backend auth work.

---

## "I need more time": move back instead of no-show

**Decision:** The waiting ticket has an "I need more time" button, and the called screen has "Not there yet? Move me back". Either moves the ticket back 5 places (to the end of the line if fewer than 5 are behind). The number stays the same; only the call order changes. Once per ticket, and it doesn't count as a no-show. The owner console marks moved tickets so staff know why the order changed.

**Why:** It turns some no-shows into late arrivals, which is the metric the verification work is judged on. Log how often it's used for the evaluation.

**Status:** Current, amended Sep 28: the queuer says how much time they need, and the estimator decides how many places that is (see "Scope after the redesign"). Fields in MODELS.md. Built Sep 29: the sheet (5–45 min, a before → after preview, the minutes per person it used), from the ticket and from Called.

---

## Arrival info: directions, "Bring" and "Be there by"

**Decision:**
- Queue Detail gets a Directions button that opens Google Maps at the venue through an Intent (no Maps SDK).
- A new optional "Bring" field on the queue shows on Queue Detail and on the ticket.
- The ticket shows "Be there by", which is the estimated call time minus 10 minutes. It needs no location permission.

**Why:** The review found Queue Detail showed only the venue name. Most late arrivals are about not knowing when to leave or what to bring.

**Status:** Current. Model change: `Queue` gets `bring` (text) and venue coordinates. Amended Sep 29: "Be there by" on a short wait.

---

## Verified organizers: a hand-set flag

**Decision:** Organizer accounts get a `verified` flag that you set by hand after checking they're a real LGU office, clinic or school. Verified queues show a check badge after the name on cards and "Organized by … · Verified" on Queue Detail. Create Queue shows who you're posting as. Unverified organizers can still post; they just get no badge.

**Why:** Anyone can post a public queue, so a fake "Relief Distribution" is a real scam risk that the prank-queuer checks don't cover. A manual flag closes that gap without building a verification process.

**Status:** Current (Sep 28). `verification_status` on the User, `organizer_verified` on each Queue (MODELS.md).

---

## Forgot password goes through support

**Decision:** Login has "Forgot password?". It opens a screen that says reset codes by text aren't available and asks the user to email support with their name and registered phone number. Support replies with a temporary password. The screen also says a ticket already held stays valid at the counter.

**Why:** There's no SMS gateway, so an OTP flow would be fake. An honest manual process is enough for a pilot.

**Status:** Current (design). Needs a support email and an admin way to set a temporary password.

---

## Now Serving direction (Sep 28)

*Superseded as the visual direction by "Civic Paper direction (Sep 28)" below. The structural and content decisions in this section still apply.*

The "Quapp Now Serving" canvas (49 screens plus Help and support and Location declined) replaces the monochrome canvas as the design to build. Its design system is DESIGN.md in `Desktop\quapp`. The structural decisions from Sep 26–27 (statuses, schedule, categories, navigation, tickets, guest browsing, arrival info, move back, verification checks) still apply; only the visual system and the entries marked below change.

---

## Now Serving replaces the monochrome direction

**Decision:** Build the "Now Serving board" design: dark only (ink `#0B1020`, bone `#EDE6D8`), Barlow for reading and Barlow Condensed uppercase for display and every number, 3–8px corners, depth from hairlines instead of shadows, a bronze crosshatch texture on every screen, and queue numbers set as "struck" numerals over a faint "88" ghost. Status colours: called `#FF7A1A`, open/served `#86D69A`, paused `#E8C27A`, no-show and destructive `#FF8E80`.

**Why:** It gives the app a clear identity tied to what it replaces (the number board over a barangay hall counter) instead of a generic card layout, and it was reviewed against the same fixes as the monochrome set.

**Trade-off:** It costs more to build: a tiled background drawable, a custom view for struck numerals, and a glow that Android text can only approximate (one `shadowRadius`, not three layers). The monochrome canvas stays as a reference and fallback.

**Status:** Superseded (design) by "Civic Paper replaces Now Serving" (Sep 28). It had superseded "Monochrome visual direction replaces the teal palette", "Depth: one black card per screen, three card levels" and "Status colors: waiting has no color, the others are muted".

---

## The called screen is the one loud moment

**Decision:** The called screen keeps its 176px glowing orange number and orange "I'm here" button. The other outcome screens (served, slot released, queue closed) keep colour to small elements, as before. Numbers inside lists and cards stay bone even when they name the number being served.

**Why:** Being called is the one moment a queuer must not miss, and it's the heart of the "Now Serving" idea. The 10% colour cap from the monochrome review still makes sense everywhere else.

**Status:** Current (principle). In Civic Paper the loud moment is the whole screen turning espresso, with the number at 132sp in marigold and a marigold "I'm here" button; no glow. Replaces the colour limit in "Ticket outcome screens: white card, color only on small elements" for the called screen only.

---

## Called screen says "Move me back"

**Decision:** On the called screen the secondary button is "Move me back", with "Not there yet? You can move back 5 places once." as helper text below it. The waiting ticket keeps "I need more time", which opens the same sheet.

**Why:** Brent's comment on the canvas (Sep 27): the helper text explains, the button acts. The Now Serving draft had used "I need more time" on both.

**Status:** Current (design).

---

## Queue card v2

**Decision:** The wait is the one large figure on a queuer card (organizer cards: waiting), with in line and now serving small beside it. The join radius appears on the venue line only when a queue has one ("Within 2 km"). "Ends in…" is removed; a queue's last hour shows "Closes in 40 min" in amber. The category word is dropped from the venue line because the icon tile shows it. The status pill moves to the schedule row so names don't wrap. Paused and Closed cards are faded. Organizer cards drop "min each" (it lives in Insights).

**Why:** The first version gave four figures equal weight, said the category twice, and wrapped long names around the status pill.

**Status:** Current (design). Supersedes the card layout in "Queue cards carry dates, description and live numbers" (the content rules still hold). Browse needs each queue's join radius and closing time.

---

## Help and support, and a location-declined state

**Decision:** Profile's "Help and support" row opens a screen with how queues work, the checks some queues use, moving back and leaving, email support and "Report a queue". Tapping Join after declining location shows "Location is off for Quapp" on Queue Detail, with "Turn on location" (opens Android settings) and "Browse other queues".

**Why:** Both were gaps: a row with no screen, and a Join button that would just reopen the permission sheet.

**Status:** Current (design).

---

## Data models are settled before coding

**Decision:** No Retrofit models or layouts get written until the FastAPI schema covers everything the screens show. Pending: queue schedule (start/end date, open/close time) instead of `serviceHours`; `Queue.Status.UPCOMING`; `Ticket.Status.QUEUE_CLOSED`; `Ticket.movedBack`; `bring`; venue coordinates and join radius; closing time on Browse; server-masked phone numbers; an undo for Served; and organizer verification, now settled under "Trust and safety (Sep 28)": `User.status`, `phone_verified`, `device_install_id`, organizer `verification_status`, and `VerificationRequest`, `Report` and `TicketRemoval` records.

**Why:** The redesign added fields to nearly every screen. Changing models after the Android side is written means rewriting adapters and layouts twice.

**Status:** Current. Settled Sep 28 in `MODELS.md` (see "Scope after the redesign").

---

## Trust and safety (Sep 28)

Screens 50–58 and the two admin boards on the "Quapp Now Serving" canvas.

---

## Organizer verification: optional, checked by phone, by hand

**Decision:** There is one account type; nobody picks "individual" or "organization" at signup. Anyone managing queues can ask to be verified from Profile by giving the organization name, type, their position and the office's public phone number. Brent (admin) calls that number and approves or rejects on a small web admin page. No ID photos are collected. The request's answers are deleted after a decision; only the result and an admin note are kept.

**Why:** A call to a publicly listed office number is harder to fake than an uploaded ID, and collecting no documents means the most sensitive data never exists.

**What the badge promises:** Quapp confirmed who runs the account. It does not vouch for each queue's details; the badge sheet says so and offers "Report this queue".

**Status:** Current (design). Model: `verification_status` NONE / PENDING / VERIFIED / REJECTED / REVOKED on the organizer, plus a `VerificationRequest` record.

---

## Unverified organizers can post, with limits

**Decision:** Unverified organizers get no badge, an "Unverified organizer" note on Queue Detail telling people to check the organizer's own announcement, one live queue at a time, and no listing in "Open now across Bohol".

**Why:** Requiring verification would make Brent a bottleneck, lock out small community groups and complicate the demo. The limits keep a fake queue from looking as trustworthy as a real one.

**Also considered:** Posting freely with only the badge missing (fake queues look almost as trustworthy); verification required before posting (too slow).

**Status:** Current (design).

---

## Suspended accounts are blocked at login

**Decision:** Users get a `status` of ACTIVE or SUSPENDED, with a reason and date. A suspended user sees "Account suspended" at login with the reason and an email-support button. Their live queues close as `QUEUE_CLOSED` (never a no-show for the people in them) and their tickets are released. Revoking verification is separate: the badge disappears but queues keep running.

**Status:** Current (design).

---

## Anti-prank measures for queuers

**Decision:** No ID checks for queuers. Add a limit of 2 accounts per device (app install ID), organizers removing a ticket with a reason (prank counts as a no-show and goes to the admin; duplicate and asked-to-leave don't), and "Report a queue" with anonymous reporters.

**Why:** Phone numbers aren't confirmed without an SMS gateway, so one person could make many accounts and reset the cooldown. These measures close most of that gap cheaply. The four checks plus these raise the cost of pranking; they don't make it impossible, and the write-up should say "reduces", not "prevents".

**Status:** Current (design). Needs `device_install_id` on signup, a `TicketRemoval` record and a `Report` record.

---

## Design leans on how people already find queues

**Decision:** Assume people usually know which queue they want because the organizer announced it (barangay page, posters, word of mouth), and they queue occasionally (relief now and then, bills monthly). So the app doesn't try to catch every fake queue itself. It makes the organizer's name and verification visible, tells people to match an unverified queue against its announcement, and makes the organizer's Share link and QR code the main way in.

**Caveat:** Fake relief queues target exactly the people who most need relief, and occasional use means people don't learn what normal looks like. That's why reporting and suspension still exist.

**Status:** Current (principle).

---

## Personal data handling

**Decision:** Passwords hashed with bcrypt; HTTPS only; phone numbers masked on the server before organizers see them; location never stored (only pass/fail and the distance at join); verification answers deleted after review; reporters never shown to organizers. The documentation should note the Data Privacy Act of 2012 (RA 10173).

**Status:** Current (design).

---

## Civic Paper direction (Sep 28)

The "Quapp Civic Paper" canvas (64 artboards: every Now Serving screen restyled, plus 12b and a logo board) is the design to build. `DESIGN.md` in the repo root is its written system; the "Civic Paper" design-system artifact holds the same tokens. Content, flows and copy are unchanged from Now Serving; only the look, layout and organization changed.

---

## Civic Paper replaces Now Serving

**Decision:** Build the "printed ticket" look. The screens are warm paper (#F2EDE3) with a faint grain, flat lighter cards, and one espresso ticket per screen for the thing that matters now. Type is DM Serif Display for headlines and ticket numbers, IBM Plex Sans for reading, and IBM Plex Mono for data. Light theme only.

**Why:** A queue app lives next to a real paper ticket at the municipal hall. Echoing that ticket gives the app an identity tied to what it replaces, reads well in daylight, where it's mostly used, and costs less to build than Now Serving. There's no crosshatch texture, no ghost digits and no multi-layer glow: numbers are plain TextViews again.

**Also considered:** Now Serving (dark board). Distinctive, but dark-only, uppercase condensed type, and the three most expensive custom pieces in the app. It's kept as a reference canvas.

**Status:** Current (design). Supersedes "Now Serving replaces the monochrome direction".

---

## Espresso and marigold instead of olive

**Decision:** The spotlight is espresso `#2B211C` and the "called / now serving" signal is marigold `#F2C14E`, replacing the Civic Paper source's olive-tinted charcoal and olive green.

**Why:** Brent didn't like the olive pair. Espresso keeps the coffee palette, and marigold on espresso reads like ink on a printed ticket. Marigold sits near the amber used for Paused, but marigold only ever appears on the dark spotlight and Paused only on paper, so they don't meet.

**Also considered:** Espresso + teal (ties to the old icon), navy + marigold (leaves the coffee palette), charcoal + coral (too close to the error red).

**Status:** Current.

---

## One spotlight per screen

**Decision:** At most one espresso ticket per screen, holding the thing the person acts on:

- My ticket
- the console's Now serving panel
- the live queue on Owner home
- the "You're in line" banner
- the called card or notification

The Called screen and the counter display turn their whole ground espresso. When two things compete, the one the person acts on wins and the other becomes a normal card (for example, the home banner behind the called notification).

**Why:** One dark shape on a light screen is instantly findable, and keeping it to one keeps it meaningful.

**Status:** Current. `DESIGN.md` section 6 lists every screen.

---

## Ticket numbers in serif, tallies in mono

**Decision:** A number that names a ticket (#43) is DM Serif Display. Anything that counts or ticks (11 min, 41 ahead, 2:14) is IBM Plex Mono. DM Serif is never below 20sp and never bold. All three fonts are bundled in `res/font` and replace `barlow_semibold.ttf`.

**Why:** Serif digits are proportional, so a ticking count would shift sideways; mono digits are all the same width. Thin serifs blur below 20sp on cheap screens, and the face has no bold, so Android would fake one. Bundling avoids a flash of the fallback font on slow data.

**Status:** Current. Supersedes "Barlow SemiBold for display and headline roles".

---

## Printed-ticket details, each with one job

**Decision:** Six small details make the screens feel like tickets. Each has one place:

- **Tear-off stub:** the "You'll be #43" join bar and the kept ticket on outcome screens.
- **Rubber stamp:** one headline status on Served, Slot released and Queue closed only.
- **Receipt slip:** stats with dotted leaders on queue detail, Insights and History.
- **Serial line:** the category on queue detail.
- **Barcode:** your own ticket stub only.
- **Punched selection:** the active nav tab, the nav badge and a selected chip are small notched tickets.

A crema tear line runs above every bottom button bar and along the top of the bottom nav.

**Why:** Brent asked for a more papery, tickety feel. Giving each detail one job keeps it from turning into a costume, and they all reuse two drawables (the notched shape and the dashed line).

**Also considered:** Notching every card (would make the real ticket stop standing out); heavier grain on cards (hurts readability on cheap phones).

**Status:** Current. `DESIGN.md` section 5.

---

## One logo: the ticket mark

**Decision:** One mark: an espresso 4:5 ticket with a cream serif Q, side punches and a crema tear line. It's used alone on the splash, with a serif "Quapp" wordmark on Log in, Role select and Admin, on a paper background for the launcher icon, and as a one-colour cut-out for the notification small icon. The splash becomes paper with the mark, not `teal_40`.

**Why:** The canvas had five different marks, and the launcher icon was still the teal wordmark from the first build.

**Status:** Current. Replaces the Splash exception in "Semantic color roles instead of direct color references". Needs `ic_launcher_foreground.xml`, the launcher background colour and a notification icon.

---

## Light theme only; `values-night` goes

**Decision:** Ship one light theme. Delete `values-night/themes.xml`. The Espresso dark theme is deferred.

**Why:** One theme is half the colour QA, and a queue app is mostly used outdoors in daylight. The earlier dark-mode bug shows how quietly a second theme can go wrong.

**Status:** Current.

---

## Spacing gets a 12dp step

**Decision:** Add `space_ms` = 12dp between `space_sm` (8) and `space_md` (16).

**Why:** Civic Paper spaces cards 12dp apart and uses 12dp inside small controls. Without a token, 12dp would get typed by hand or rounded to 8 or 16.

**Status:** Current. Amends "8dp spacing grid".

---

## Upcoming is neutral; Called is espresso and marigold

**Decision:** Upcoming and Waiting are neutral (`ink_muted` on `paper_sunk`). Paused keeps `warn`. A Called label is an espresso pill with marigold text. The other statuses: Open/Served `ok`, No-show `err`, Closed an outline.

**Why:** Upcoming is good news and Paused is a stop; sharing one amber made them look alike.

**Status:** Current.

---

## Modals cover the bottom nav

**Decision:** Dialogs and bottom sheets dim the whole screen, bottom nav included.

**Why:** That's how Android modals behave, and the mockups for 26, 28 and 58 left the nav bright, which made it look tappable during a decision.

**Status:** Current (design). In code this is the default for `MaterialAlertDialogBuilder` and `BottomSheetDialog`; don't host modals inside a Fragment's own layout.

---

## My tickets while still in line (12b)

**Decision:** With two tickets and neither called, My tickets shows "In line now" with a compact spotlight ticket (stub with number and barcode, then "In line · Open", people ahead, queue and venue, est. wait and now serving) above "Later today".

**Why:** Brent's canvas comment: the list only showed the called case.

**Status:** Current (design). The 4 ahead / 6 min / #8 figures are placeholders.

---

## Called is its own Activity

**Decision:** Being called is `CalledActivity`, with `Theme.Quapp.Called` set in the manifest. `ActiveTicketActivity` keeps the other states (waiting, served, slot released). When the ticket is called, the ticket screen opens Called and closes itself. "I'm here" or the timer running out opens the ticket screen again, which shows the outcome.

**Why:** The Called screen changes the whole window: espresso ground with no grain, light status-bar icons, marigold number. Those are theme values, and an Activity's theme can't change while it's running. Swapping colours by hand in code (the old approach) missed the window background and the system bars. A separate Activity also gives the "you're called" notification one clear screen to open.

**Also considered:** Recreating `ActiveTicketActivity` with a different theme when the status changes. That's more code and it flickers.

**Status:** Current. Built Sep 28.

---

## Scope after the redesign (Sep 28)

**Decision:** The models in `MODELS.md` are the contract, and they cover this scope:
- **Proximity check is built.** Per-queue join radius (500 m, 1 km, 2 km, 5 km), location read once at Join, no background tracking. `Queue.joinRadiusMeters`.
- **Several tickets per queuer**, as long as the queues' hours don't overlap on the same day. Replaces the one-queue-at-a-time rule. The server checks overlap at join.
- **"I need more time" is built, and the estimator decides the distance.** The queuer picks how much time they need (5–45 min). Places moved back = that time ÷ the rolling-average minutes per person, rounded up, capped at the end of the line. Once per ticket, keeps the number, never a no-show.
- **Trust and safety is built in full:** verified badge, Report a queue, organizer removals with a reason, suspension, 2 accounts per device, and the admin web page on FastAPI.
- Also settled: schedule (start/end date, opening/closing time) replaces `serviceHours`; `Queue.Status.UPCOMING`; `Ticket.Status.QUEUE_CLOSED` and `REMOVED`; eight categories as an enum; `bring`, `shortDescription` + `details`; `joinedAt`, `calledAt`, `finishedAt` on tickets; `smsOtpEnabled` dropped (SMS stays future work, shown as PLANNED); minimum password length 8.

**Why:** Brent's call. Moving back by time instead of a fixed 5 places fits how people are late (a jeepney ride, not "5 people"), and it puts the course's AI/ML requirement to work twice: the same rolling-average estimate drives both the ETA and the move-back distance.

**Trade-off:** This supersedes "Cut the proposal down to a buildable subset". Three checks instead of one, a multi-ticket My tickets, location permission and field testing, and a backend roughly twice the size (accounts, reports, removals, admin page). Build order keeps the risk down: queuer and owner flows first, then proximity, then trust and safety, then the admin page.

**Also considered:** Keeping proximity and trust and safety as future work, one ticket at a time, and a fixed 5-place move back (the recommended smaller scope).

**Status:** Current.

---

## Wait-time estimation learns online (the ML part)

**Decision:** Server-side, in FastAPI. A linear model (scikit-learn `SGDRegressor`) predicts **minutes per person** for a queue right now; every wait is that × people ahead, and the move-back distance is minutes needed ÷ that.
- **Features, live:** rolling average of the last 5 service times, people served in the last 15 min, people in line, no-shows in the last hour, moved-backs waiting.
- **Features, context:** hour of day, day of week, category, minutes since the queue opened, whether grace period and proximity are on.
- **Warm start:** trained on simulated days before launch.
- **Online:** after each person is served, the model and its scaler take one `partial_fit` step with the real service time. Saved to disk after each update so a restart keeps what it learned.
- **Guards:** a small learning rate; service times over 30 min are treated as a break and skipped; a queue with fewer than 10 served people today uses the rolling average instead. `QueueStats.estimate_source` says which one was used.
- **Evaluation:** prequential ("predict, then learn") on simulated days. The error in minutes for the rolling average, a model trained once, and the online model, plus how the online model's error falls as it learns.

**Why:** The course requires ML, and Brent's lives in wait-time estimation. A rolling average alone is statistics and can't learn that lunch hour is slow or relief queues run longer. Learning from each served person uses the live data the app already produces, and a linear model's coefficients can be read out at the defense. There is no real usage data yet, so simulated days train and test it; the write-up says so and explains that real data keeps training it.

**Also considered:**
- Batch retraining only (nightly): simpler, but misses changes during the day.
- Gradient boosting: usually more accurate, much harder to explain; possible as a second model to compare against.
- On-device TensorFlow Lite: not needed, Quapp is an online service.
- A no-show prediction model: future work; it would double the ML and evaluation work.

**Status:** Current (design). Built with the backend. Supersedes the forecasting half of "Cut the proposal down to a buildable subset".

---

## A ticket is for one day; overlap is checked on that day

**Decision:** When checking whether two tickets' hours overlap, each queue is compared on the day you'd actually be in its line: today once the queue has started, otherwise its first day. Two queues clash only if that day is the same and their hours overlap. Touching ends (one closes at 12:00, the next opens at 12:00) don't clash. `TicketRules.hoursOverlap`, with unit tests.

**Why:** Built first as "any shared day", it blocked the canvas 12 case: someone in line today for a two-day relief distribution couldn't hold a ticket for tomorrow's job fair, although they'll never be in both lines at once. A ticket holds one place on one day.

**Also considered:** Comparing every day both queues run (too strict, as above). Letting the queuer pick which day of a multi-day queue they're joining for (more honest for long queues, but a new step at Join; revisit with the backend).

**Status:** Current. Built Sep 29. MODELS.md "Several tickets" updated to say so.

---

## The queuer's tickets live in the line

**Decision:** Joining puts the ticket into the queue's real line (FakeData now, the server later), and the queuer's app keeps only ticket ids (`ActiveTicketStore`), asking the line where each one stands on every read. Everything the owner does reaches the queuer: Call next and No-show, Remove from line (with the reason in the message), closing the queue, and "I'm here" shows on the console as "Here · confirmed at 10:13 AM" (canvas 29). The ticket number is the queue's next number, not people waiting + 1.

To make "I need more time" demonstrable, seeded open queues simulate one new arrival a minute (up to 20 per session), standing in for other people's phones.

**Why:** Brent found that joining didn't add you to the line (the card said 42 waiting while you held #43), and move-back can't work without a real line to move in. It also matches how the server will work: one line, many screens reading it.

**Also considered:** Keeping a local copy of the ticket and nudging it (what it did before). It can't show the owner's actions, and every new rule would need two copies kept in step.

**Status:** Current. Built Sep 29. Replaces the "Replace ActiveTicketStore static field" item's first half: the store holds ids; fetching them by id is the Retrofit step.

---

## "Be there by" on a short wait

**Decision:** "Be there by" is the estimated call time minus 10 minutes, or minus half the wait when the wait is under 20 minutes (`TicketRules.beThereInMinutes`). The ticket and the move-back sheet use the same rule.

**Why:** With a flat 10 minutes, a 10-minute wait said "be there by now", and the move-back preview showed "8:47 AM → 8:47 AM", which read as a bug.

**Status:** Current. Amends "Arrival info: directions, "Bring" and "Be there by"".

---

## Proximity check: framework LocationManager, read once at Join

**Decision:** Join on a queue with a radius first shows "Check that you're nearby" (44), then Android's permission dialog. `ProximityCheck` reads one location with `LocationManagerCompat.getCurrentLocation` (fused provider on Android 12+, else network, else GPS), or uses a fix from the last 2 minutes straight away, and gives up after 15 s with the last known fix. Inside the radius goes on to Join; outside shows "You're 4.2 km away" with Check again (45); refused permission or the phone's Location switch off shows "Location is off for Quapp" (48) with a button to fix whichever it is. Precise or approximate location is accepted.

**Why:** It's the whole check, with no library: Google Play services' FusedLocationProviderClient would add a dependency and needs Play services on the phone. The smallest radius is 500 m, so approximate location is good enough.

**Also considered:** Play services location (more accurate indoors, one more dependency); checking again at "I'm here" (that's tracking, which the design promises not to do).

**Status:** Current. Built Sep 29. The server should check the radius again with the lat/lng sent at join (MODELS.md `POST /queues/{id}/tickets`); today the app's check is the only one.

---

## Offline shows the last numbers, not a guess

**Decision:** My ticket watches connectivity (`Connectivity`, a `ConnectivityManager` callback; "online" means Android validated the internet). Offline, the waiting ticket keeps the numbers it last had, says "as of 10:12 AM", drops the ticks and Now serving, says the wait can't update, and adds "Your spot is kept. Show #43 to staff at the counter" (25). Retry, Leave and I need more time wait for the connection. It catches up by itself when the phone is back.

**Why:** A queue app is most likely to lose signal inside a crowded venue. Stale numbers labelled as stale are more useful than a spinner, and the one thing the queuer needs to know is that their place is safe.

**Status:** Current. Built Sep 29. With FakeData nothing actually needs the network, so offline is simulated by freezing; with Retrofit it becomes real.

---

## Notifications: an Application class stands in for the push

**Decision:** `QuappApplication` creates the "You're being called" channel (high importance) and listens to FakeData's `CounterListener`. When the counter calls one of the queuer's tickets, `CalledNotifier` posts a heads-up notification with a live countdown, "I'm here" (a `BroadcastReceiver`, so the app doesn't open) and "Open ticket"; it's cleared when the ticket leaves the counter and times out with the grace period. The permission screen (20) shows once, over the new ticket, after the first join on Android 13+.

**Why:** "Notifications aren't optional for the grace-period strategy" (Open questions): a queuer with the phone in a pocket would otherwise miss 3 minutes and get a no-show they didn't earn. Building the notification now means the backend only has to replace what triggers it (a Firebase message instead of FakeData's listener).

**Also considered:** Posting from each screen that notices the call (misses the case the notification exists for: no screen open).

**Status:** Current. Built Sep 29. On one phone the demo is: join as queuer, switch to managing queues, Call next until you're called.

---

## Share link and QR with ZXing; quapp.app is a placeholder

**Decision:** Share queue (31) and the counter display (32) draw a real, scannable QR with ZXing core (`com.google.zxing:core`, encoding only, pure Java). The link is `https://quapp.app/q/<name-slug>`.

**Why:** The design leans on the organizer's link and QR as the main way in, so a picture of a QR that doesn't scan would be dishonest in the demo. ZXing core is small, has no Android or camera parts, and drawing QR codes by hand is not worth the code.

**Also considered:** A decorative QR image (doesn't scan); a QR drawing library with more features (not needed).

**Status:** Current. Built Sep 29. The domain isn't owned: the link opens nothing yet. Needs a real domain (or the FastAPI server's address) and an App Link intent filter so it opens Queue detail.

---

## Organizer verification lives on the organizer, and the demo plays the admin

**Decision:** FakeData keeps the logged-in organizer's `VerificationStatus`; every queue they own reads its badge from it, so a revoke shows everywhere at once (the canvas note "Queue: no badge field"). Get verified (50) makes it PENDING; Profile shows the pending card (51). Until the admin page exists, long-pressing the Profile row or card plays the admin: verified → back to unverified, pending → approved. Unverified organizers get the note on Queue detail (53), no listing in "Open now across Bohol", and one live (open or paused) queue: creating a queue that opens now, or reopening a closed one, while another is live shows the limit sheet (58). An upcoming one is allowed.

The organizer line on Queue detail opens the badge sheet (52) for both verified and unverified organizers, with Report this queue. Report (54) comes from there and from the flag on the join flow screens (see "Report a queue from the join flow").

**Why:** It follows "Unverified organizers can post, with limits" without building the admin page first. Starting verified keeps the seeded queues looking like the canvas.

**Status:** Current. Built Sep 29. Demo hooks are listed under Known compromises.

---

## Report a queue from the join flow, not Help

**Decision:** A flag button in the app bar of Queue detail, Join and My ticket opens Report a queue for that queue (the organizer badge sheet keeps its "Report this queue" too). Help and support no longer has a Report row, and the Report screen no longer has a queue picker: it's always opened with a queue.

**Why:** Brent (Sep 29): reporting belongs where you're looking at the queue, not in Profile, and picking the queue from a list of every queue was awkward. The canvas (49) had the row in Help; this replaces it.

**Status:** Current. Sep 29.

---

## Browse: the header scrolls away, search and chips stay

**Decision:** Browse is a `CoordinatorLayout` with an `AppBarLayout`. The title, greeting and the state card (shortest wait, in-line banner or town question) scroll away with the list and come back at its top; search, the town and category chips and the count stay pinned on paper. The list and the empty state use `appbar_scrolling_view_behavior`.

**Why:** Brent (Sep 29): only one and a half queue cards were visible. The fixed header took about 45% of a Pixel 8 screen for good, and more on the Xiaomi. Now a small scroll shows about three cards, and filtering is still one tap away. The cards keep their stat footer, which is the reason to look at Browse.

**Also considered:** Smaller cards (loses the wait / in line / now serving numbers); the whole header scrolling away (then changing the filter means scrolling back up); `enterAlways` so the header returns on any upward scroll (a tall block jumping back in steals the space again).

**Status:** Current. Sep 29. The owner's Queues tab got the same treatment the same day (Brent): the title, greeting and live-queue spotlight scroll away, the status chips stay. The Today tab keeps its fixed header: it's a short report with compact rows and no filters.

---

## Suspension and the device limit, locally

**Decision:** Login checks `FakeData.suspendedAccount(phone)` and shows Account suspended (55) instead of logging in; 0918 000 0000 is the seeded suspended account. Create account counts the phones registered on this install in a separate SharedPreferences file that logging out doesn't clear; at 2 it shows the device limit (56) and its one button goes back to Log in.

**Why:** Both are server rules (403 at `/auth/login`, `device_install_id` at `/auth/register`); these stand-ins let the screens be built and shown now, and are deleted with FakeData.

**Status:** Current. Built Sep 29. Clearing the app's data resets the local count; the server's install id won't have that hole.

---

## Minimum password length is 8

**Decision:** `Validation.MIN_PASSWORD_LENGTH` goes from 6 to 8.

**Why:** MODELS.md settled 8 on Sep 28 and the canvas says "At least 8 characters"; the code still said 6.

**Status:** Current. Sep 29.

---

## Components get their own paper texture

**Decision:** Every component that has a fill carries a texture, not just the screen ground. There are three tiles, and which one a component gets follows its fill colour: `grain_stock` (dark fibres, card stock) on light fills like cards, fields, chips, tonal buttons, the nav bar and sheets; `grain_ink` (pale specks where the ink didn't take) on dark fills like filled buttons, the selected chip and the FAB; `grain_ticket` (pale fibres and a mottle) on the espresso spotlight and the Called screen. Text and outlined buttons have no fill, so the grained paper shows through them. `Grain` paints the tile inside the component's own Material shape, so corners, pills and punched holes stay clean. `QuappViewInflater`, named in the theme's `viewInflaterClass`, attaches it to every Material component and every view on a paper `<shape>` background as layouts inflate, so no screen code changes.

**Why:** Smooth, flat components on a grained ground looked pasted on, as if they weren't made of the same paper. Choosing the tile from the fill, and reading the fill every frame, means a chip switches from stock to ink by itself when it's selected, and new screens get it for free.

**Also considered:** A `layer-list` background per drawable (the bitmap can't be clipped to rounded corners or punches, and MaterialButton and MaterialCardView don't take a custom background). `android:foreground` in the styles (MaterialCardView already uses its foreground for the ripple and stroke). Calling `Grain.attach` in every Activity and adapter (dozens of call sites, easy to forget one). Keeping the old rule of grain on the ground only (it's what made the components look off).

**Status:** Built, awaiting Brent's device test. The earlier colour-pass audit was dropped: Brent kept the original colours. It's in `git stash` in case it's wanted later.

---

## Queue cards are catalogue cards on aged paper

**Decision:** Queue cards (Browse and the organizer's Queues tab) are old library catalogue cards: the category and the queue's number in the corner, the status as a rubber stamp, the name in serif over a double rule, then Where / What / When / Wait written on ruled lines, and a punched hole at the bottom. They're whiter than the paper, with a 2dp shadow and a slight tilt each. The screen ground changes from fine grain to aged paper (`paper_aged.png`: soft blotches, fibres, a few tiny age spots), and the component textures go to the loudest strength from the texture canvas (2.4 times the first version).

**Why:** Brent wanted the queue cards to read as notes, and picked this over classic sticky notes, taped paper notes, plain index cards and a two-column sticky wall on the "Quapp textures and sticky notes" canvas. The ruled Where / What / When / Wait lines were his pick over running text. The card is whiter than the paper because on the aged ground a cream card blended in. Aged won over vintage (stains, darker edges) once Brent saw vintage on a phone-size screen: too much. Serving stays on the Wait line so the three numbers read together.

**Also considered:** Keeping the v5 card with its status strip and number footer (fine, but it looked like a table on the textured ground). A vignette on the ground (it showed as bands at the pinned headers, so it came out). "Louder" (1.7×) textures (Brent chose loudest).

**Also:** the bottom nav became a tear-off stub like the join dock (notched top corners, inset tear line), the tilted stamp no longer clips (its rows let it draw past its box), and the system bars went transparent with the headers' texture lined up to the window, so the paper runs edge to edge with no seams.

**Status:** Built, awaiting Brent's device test. Supersedes the "no stains" and "no shadows on cards" rules in DESIGN.md for the ground and the queue cards only.

---

## Backend: PostgreSQL and SQLAlchemy, in backend/

**Decision:** The FastAPI server uses PostgreSQL (already installed on Brent's desktop, where the server runs) through SQLAlchemy models. The code lives in `backend/` in this repo. The schema is in MODELS.md, "Database (PostgreSQL)". Claude writes the backend one step at a time; Brent reviews each step before the next.

**Why:** Postgres is already on the machine, so there's no setup cost, and the server has to be reachable from the instructor's phone anyway. SQLAlchemy is the usual FastAPI pairing and the easiest to explain. One repo keeps MODELS.md, the app and the server changing in the same commit. Numbers the server can count (positions, waits, stats, the cooldown) aren't stored, so they can't go stale.

**Also considered:** SQLite (simpler, but Postgres was already there). SQLModel (less code, thinner docs). Plain SQL with a driver (most transparent, most typing). A separate backend repo (the contract would live in two places). Native Postgres enum types (harder to add a value to than VARCHAR with a CHECK).

**Status:** Current. Hosting is still open: the desktop, reachable over mobile data (see "Backend doesn't need shared Wi-Fi"), is the working assumption.

---

## "You've been called" reaches the phone by polling

**Decision:** The app asks the server again every few seconds while a screen that can change is open: Active ticket and My tickets every 10 seconds (`GET /tickets/{id}`, `GET /me/tickets`), the Live console every 5 (`GET /queues/{id}/line`). The called notification fires when a poll sees the ticket turn CALLED. Nothing new on the server: those endpoints already exist.

**Why:** It's the simplest thing that works for a demo with two phones, and there's nothing new to explain at the defense: a timer and a request. The 3-minute grace window is long compared with a 10-second poll, so nobody loses their slot to the delay.

**Also considered:** FCM push, which reaches a phone even when the app is closed. It needs a Firebase project, `google-services.json` and a server key, and it's worth adding if time allows. A WebSocket or server-sent events: instant, but more moving parts on both ends than polling, for no gain a demo would show.

**Known limit:** With the app closed, nothing polls, so the call only shows when the app is opened again. Say so in the demo, or add FCM.

**Status:** Current (BACKEND.md step 5.4).

---

## Known compromises

Deliberate shortcuts, not oversights. Each has a planned fix.

1. **`ActiveTicketStore` is a static field holding app state.** If Android kills the process in the background it's null while the Activity comes back expecting a ticket — a real crash path. Accepted because the backend replaces it with a fetch by ticket id. `Parcelable` would be the proper local fix, but that's boilerplate for something being deleted anyway.

2. ~~**`servedToday` is hardcoded to 18** in `OwnedQueueAdapter`.~~ Fixed — comes from `FakeData.stats()` now.

3. **The 2km proximity radius is hardcoded in three separate strings** (Queue Detail, Join, Create Queue) with nothing enforcing they match. Becomes a `proximityRadiusMeters` field when it becomes an input.

4. **`serviceHours` is a free-text String.** (Design now replaces it; see "Schedule replaces free-text service hours".) It can't be queried, sorted, or used to auto-close a queue. This is the model decision most expensive to change later — it should become real start/end timestamps before the backend gets written.

5. ~~**No validation anywhere.**~~ Login, Register, Join, Create/Edit Queue and Walk-in now validate. Phone must be 09 plus nine digits.

6. **Active Ticket's "called" state is triggered by long-pressing the "ahead of you" count**, and long-pressing the countdown skips to the end of the grace period. Pure demo scaffolding — in the real system both arrive from the backend.

7. **Ticket removal in Live Console relies on reference equality.** `List.remove(Object)` uses `.equals()`, which `Ticket` doesn't override, so it's identity comparison. Works because the adapter hands back the exact object from the list. Would break if tickets came from separate network fetches — then removal needs to be by id, or `equals` needs overriding. Fixed — `FakeData` removes by id.

8. **Queue history has no dates.** `Ticket` has no `joinedAt` field, so rows show queue, ticket number and outcome only. Adding it is a model change to propose.

9. **New queues get Tagbilaran City's coordinates** until Create Queue has a map picker.

10. **Trust and safety demo hooks.** Long-press the organizer's verification row or pending card in Profile to play the admin (verified → unverified, pending → approved). Logging in as 0918 000 0000 shows a suspended account. The device limit counts registrations in local storage. All three move to the server and the admin page.

11. **The called notification is triggered by FakeData**, which only works while the owner and the queuer are the same app process (one phone). Real push needs Firebase Cloud Messaging and the backend.

**Status:** Deferred to full app.

---

## Bugs worth remembering

Not decisions, but the diagnosis process is worth keeping.

**Two taps needed on the Role Select cards.** Three wrong theories got chased — focus stealing, touch interception, stale hit rectangles — before actually reading the listener body, which turned out to contain another `setOnClickListener` nested inside it. Tap one installed the real listener; tap two fired it. *Lesson: when something "works on the second try," read the listener before investigating the touch system. Instrument before theorising.*

**Dark mode was never active.** `values-night/themes.xml` defined `Base.Theme.Quapp`, but the manifest points at `Theme.Quapp`. Name mismatch, so Android found no night theme and fell back to the light one. Every screen ran light colors in dark mode — invisible, because the surfaces were light too. It only surfaced when the Live Console's filled card pulled `colorSurfaceContainerHighest` (never overridden, so Material's dark baseline) and produced black text on a dark card. *Lesson: a theme override that silently does nothing doesn't crash or look broken. It quietly uses the wrong values.*

**White nav bar band under the Splash screen.** Reproduced on the physical Xiaomi but not on a Pixel emulator — HyperOS draws its own opaque nav bar background regardless of what the app requests. Not fixable app-side. Same category as the OEM SMS confirmation dialog from the course assignment. *Lesson: check device-specific rendering against a stock-Android emulator before changing code.*

**`android:simpleItems` not found.** Library-defined attributes use the `app:` namespace; only framework attributes use `android:`.

**Status:** Documented.

---

## Open questions

**Proximity radius: fixed or per-queue?** Per-queue is more defensible — catchment areas genuinely differ between a covered court and a municipal office serving a whole town — and it costs one `int`. A fixed 2km is arbitrary and invites the question. Leaning per-queue with a sensible default.

**Is "Community" a real category or a leftover bin?** Medical, Government, Education and Relief are concrete — you can look at a queue and know. Community is where owners put things when unsure, which means it swallows everything. The fake data already shows it: `q7` is a tourist assistance desk, which isn't obviously community service. If it's a catch-all, "Other" is more honest, and it stops owners miscategorising a medical mission as Community because it happens to be community-run.

**Duplicate-join detection as a fifth verification strategy.** Distinct from the existing four: they detect *prank queuing* (joining with no intent to show up), this detects *double-dipping* (a real, present, verified person joining twice for two relief packs). It catches something OTP and proximity structurally cannot.

Two things need resolving before it's implementable. First, "related queues" needs a definition — same-queue duplicates are trivial, but cross-queue is the hard case, since joining a medical mission and a clearance queue on the same day is perfectly legitimate. Proposed scope: same category, same municipality, overlapping time window. Second, the false-positive cost is asymmetric and higher than for the other strategies: a wrongly-flagged prank queuer loses a slot, but a wrongly-flagged double-dipper is being accused of fraud — and a family sharing one phone, common in exactly the communities relief distribution serves, would be denied aid. That belongs in the evaluation as a stated limitation, not just a number.

**Notifications aren't optional for the grace-period strategy.** The 3-minute window assumes the queuer is watching their phone. They joined remotely and put it in their pocket. Without a push notification when called, the window expires for people who did nothing wrong — inflating exactly the false-positive rate the proposal sets out to measure. The local notification side is already familiar from the course assignment; the push side needs the backend.

**Repository interface in front of `FakeData`?** Activities would call a `QueueRepository` interface instead of `FakeData` directly, with two implementations: `FakeQueueRepository` now, `ApiQueueRepository` (Retrofit) later. Methods use callbacks, since network results arrive asynchronously. Less pressing under the new split, since Brent does all the wiring himself — but the payoff still holds: switching to the real API is a one-line change instead of rewiring every screen. The cost is refactoring existing screens first. Suggested order: build the interface, convert Browse as the reference example, then hand off.

**Rules for the partner's UI polish.** What "improve the UI" may and may not touch: layouts, styles, theme, drawables, transitions — yes. Model classes, Activities' data logic, network code, string keys other code depends on — no. Without a stated boundary, polish drifts into refactoring and breaks wiring.

**Where the backend runs during development.** Either each person runs FastAPI locally (emulator uses `10.0.2.2:8000`, a phone uses the machine's LAN IP), or it's hosted once (Render or similar) with one shared URL. Leaning hosted, because the core demo is an owner and a queuer on different phones hitting the same queue. Either way, the base URL lives in one constant.

**Evidence of individual contribution.** If grading looks at individual work, the frontend/backend split is easy to defend as long as each person commits their own code — git history is the evidence. Worth confirming with the instructor.

**Review from the second design chat (Sep 27), not yet decided:**
- **Browse without an account.** The app opens on Login, so a first-timer has to register before seeing what Quapp is. Let people browse, and ask them to register only when they tap Join.
- **Verified organizers.** Anyone can switch to managing queues and post a public queue. A fake "Relief Distribution" is a real scam risk. A "Verified" badge for LGUs and clinics, even with manual verification, closes the gap that prank-queuer checks don't cover.
- **Getting to the venue.** Queue Detail shows only the venue name. Add a map or directions link, and a "Leave by 10:20" hint on the ticket based on the estimated wait.
- **Requirements on the ticket.** A short "Bring" line (for example, barangay ID) on the ticket, not only in the description.
- **Forgot password.** Login has no recovery. Without an SMS gateway, that limits the options.
- **Language.** A Filipino/Cebuano setting in Profile, or at least string resources ready for translation.
- **Browse states.** Designs for "no queues in your town yet", "no search results" and "couldn't load". The rules call for loading / empty / error / populated on every network screen.
- **"I need 10 more minutes."** A button on the ticket that moves you back a few places instead of turning into a no-show. It targets the metric the verification idea is built on.
- **Tab name.** The list screen says "My tickets" but other screens still say "My ticket". Go with "My tickets" everywhere.

**Status:** Open.

---

## What's built

**Shared:** Splash, Login, Register, Role Select, Profile
**Queuer:** Browse (search, filters, active-ticket card), Queue Detail, Join, Active Ticket (with cooldown), Queue History
**Owner:** Dashboard, Create/Edit Queue, Live Console (walk-in, pause/close), Insights

## What isn't

Notifications. Loading and error states (nothing loads yet — they arrive with Retrofit). Dates in history. The backend.
