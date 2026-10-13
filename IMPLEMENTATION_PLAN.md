# Personal Expense Tracker implementation plan

Date: 2026-10-13. Baseline: `personal_expense_tracker_prd_v3_1-1.md`, version 3.1.

## Current state and scope

- Initial inspection found only the PRD. The workspace now contains the JVM parser, initial Android capture-check probe, tests, build configuration, and implementation documentation.
- Engineering sources reviewed: `C:/Users/dinakar.jc/Desktop/ENGINEERING/AGENTS.md`, `ENGINEERING_STANDARDS.md`, `ARCHITECTURE.md`, and `IMPLEMENTATION_NOTES.md`. The architecture and notes are generic templates. Technology-specific documents cover Django and FastAPI, neither applicable to this native offline application.
- User device: realme P3 Ultra, Android 15. The phone requires a Wi-Fi connection to enable wireless debugging, as confirmed by the user. Use the second phone as hotspot and connect both target and laptop. USB is restricted and will not be used.
- Deployment preference updated: the user chose manual APK installation. Do not wait for wireless pairing to deliver the capture-check APK. Automated device tests remain pending without a connected target.
- Device validation result: the user reports Play Protect blocked installation of the SMS-permission APK. Automatic capture is not validated. A separate manual parser-test flavor, without SMS permissions or a receiver, allows continued phone UI/parser testing; this does not resolve or satisfy the automatic-capture release gate.
- First bank: HDFC. Reviewed sender format supplied by the user: `<XX>-HDFCBN-S`. Match the normalized `HDFCBN-S` label, stripping only a two-letter carrier prefix, never an arbitrary sender prefix. Appendix A supplies initial redacted fixtures; representative device-format coverage remains to be validated.
- Implementation has begun with the JVM parser foundation, built-in data templates, and 16 passing regression tests. Android capture, local storage, and all release gates remain incomplete; implementation checkboxes reflect acceptance gates, not just source-file presence.
- Complete the four MVP milestones, including the optional dashboard widgets requested in the PRD. Keep explicitly deferred features in a separate backlog.

## Proposed architecture

Use Kotlin, Jetpack Compose, and Material 3 as specified in PRD 7.17. Use Room/SQLite for durable local records and versioned migrations, coroutines/Flow for background processing and observable queries, and DataStore for ordinary preferences. Android notification, biometric, Keystore, and document-picker APIs handle their respective platform boundaries. Introduce a database encryption dependency only after documenting its compatibility, maintenance, and background-capture implications.

Start with an Android `app` module and a small JVM `core` module. Keep parsing, validation, classification, duplicate decisions, money arithmetic, and reporting contracts independently testable in `core`. Organize Android code by feature with shared storage and platform integration packages. Avoid a backend, web framework, analytics SDK, network client, or large dependency-injection framework. Supply dependencies through a small application-owned container initially.

Data flow: SMS receiver or inbox scan -> reviewed sender allowlist -> privacy classification -> versioned JSON template -> strict field validation -> transactional duplicate check and save/review -> observable local queries -> dashboard/history/reports/notification actions.

Incoming capture, on-open catch-up, Sync Now, initial backfill, and manual historical re-scan use this same pipeline. Historical scans have a separate preview/apply boundary. No raw SMS is logged. No financial network transmission is built into the application; inspect the merged manifest and dependencies before making this claim.

### Data and correctness contracts

- Store currency values as integer minor units, with checked arithmetic and strict decimal parsing; never binary floating point. Start with INR and Indian formatting, retaining an explicit currency field and prohibiting mixed-currency totals.
- Separate direction, message type, financial classification, expense category, and income type. Expense/fee debits enter expenses; income credits enter income. Transfers, refunds, reversals, and unclassified credits remain separate. Records marked not a transaction contribute nowhere. Fees also have a separate report slice without double counting.
- Persist transaction ID, currency/amount, date/time and approximation, bank/account, payee/UPI ID, method/reference, category/type/note, source, parse/review state, created/updated timestamps, parser and template versions, and field provenance. Explicitly represent user-set, parsed, unknown, and system-default/approximate values; the last case needs contract clarification below.
- Proposed tables: transactions; accounts; categories/income types; source identities; field provenance; review items/outcomes; transaction links; scan cursor/status. Use foreign keys, checks, and indexed date/account/classification queries. Store optional raw financial text separately so expiry never deletes the transaction or its source identity.
- Deduplicate exact source deliveries and verified bank/account/reference identities with database-enforced uniqueness, not only an in-memory check. Qualify reference identity by movement/type when debit and reversal can share a reference. Missing account or conflicting evidence must not trigger an automatic merge.
- Preserve both plausible transactions when fallback evidence is ambiguous. Identical amount/payee/day with different references means two transactions. A manual/SMS match raises a warning and requires a user decision.
- Incoming broadcasts may lack the inbox row ID. Reconcile source identities with catch-up using trustworthy metadata and fingerprints; a message-body hash alone must not collapse two genuine repeated payments. Test this against actual device delivery metadata.
- Commit processed outcomes and scan progress atomically. Use stable paging by received time plus source ID, a bounded overlap for late/provider changes, and idempotent retry. Receiver processing must not advance the inbox scan cursor past unscanned messages.
- Parse dates strictly. Missing transaction time can use received time with an approximation label; ambiguous dates go to review. Missing facts remain empty. Overlapping templates or conflicting extractions go to review.
- Scans may fill empty non-user fields, but never overwrite user decisions. Revalidate an applied preview if records changed after preview; do not apply stale decisions silently.
- Dashboard figures and their drill-downs share the same filter/aggregation contract and period/account snapshot. Avoid different totals logic in each widget.

## Feature checklist and acceptance mapping

| PRD | Implementation work | Required evidence | Milestone |
|---|---|---|---|
| 7.1 | Privacy/coverage onboarding; SMS and notification permission handling; manual-only mode; restricted-permission guidance; optional battery guidance without repeated prompts | Grant, deny, revoke, reinstall/upgrade flows on the target phone; manual records remain usable | 1 |
| 7.2 | Reviewed sender normalization and allowlist; classify before retention; reject unrelated and insufficiently financial messages | No storage/log retention for unknown senders, OTPs, offers, unrelated or ambiguous messages | 1 |
| 7.3 | Debit, credit, completed mandate, refund/reversal recognition, notices, unknown financial, non-financial handling | Advance notice creates no record and stores no raw text; unsupported financial formats enter review | 1 |
| 7.4, 8, App. A/B | Runtime JSON templates; schema validation; positive/negative fixtures; strict amounts/dates; unknown-field behavior; parser provenance | Appendix A plus representative device fixtures; uncertain amount/direction never accepted | 1 |
| 7.5 | Full record contract and field provenance; versioned Room schema and migrations | Persist/reopen round trip; migration retains user data; scan cannot overwrite edited fields | 1 |
| 7.6 | Debit defaults to expense; credit defaults to unclassified; user reclassification; transfer/refund/reversal exclusions and manual linking | Truth-table tests for direction/classification; no amount-only transfer inference | 1–2 |
| 7.7 | Create/rename/reorder categories and income types; confirmed-payee suggestions; immediate/grouped/off notifications; suggested-category actions and More | Suggestions never silently applied; one-tap notification or two-tap in-app categorization; locked-screen privacy | 2 |
| 7.8 | Manual expense/income forms; inline validation; date/time/method defaults; optional payee/note; edit/delete; duplicate warnings | Offline cash entry; invalid amounts rejected; manual/SMS matches never automatically merged | 2 |
| 7.9 | Review reasons for unknown/missing/conflicting/duplicate/unclassified/refund cases; optional uncategorized items; correct/classify/link/dismiss/not-a-transaction actions | Dismissal preserves confirmed transactions; minimal outcomes prevent repeated resurfacing; Undo works | 1–2 |
| 7.10, 8.2 | Receiver, on-open and manual catch-up; cursor/status; date-range backfill and re-scan with preview and confirmation | Repeated/concurrent scans give zero duplicates; crash retry safe; preview reports new/fill/unchanged/duplicates/unparsed | 1 |
| 7.11, 15 | Configurable 30-day unresolved raw retention; deletion of raw text independently; no ordinary-export raw text | Expiry/deletion preserves records and dedup; non-financial raw text never retained | 1, 4 |
| 7.12 | Strong-source/reference matching; ambiguous duplicate review; explicit manual refund/reversal links | Distinct references preserved; debit/reversal sharing a reference not lost; no amount-only linking | 1–2 |
| 7.13 | Detected accounts; safe suffix/unknown labels; rename; capture toggle; last capture time; multiple-account separation | Missing suffix not invented; account toggle behavior and multiple-account capture tested | 2 |
| 7.14 | All dashboard controls, required and optional widgets, states and interactions listed below | Every figure reconciles to its filtered records; dashboard <1 second with 10,000 records on phone | 2–3 |
| 7.15 | Search payee/source/note/reference; all prescribed filters; date/amount sort; transaction detail/edit; source/uncertainty badges; all report views | Combined filters/search work; reports match classification totals and disclose coverage | 2–3 |
| 7.16 | Device biometric/PIN fallback; launch/timeout lock; biometric-change handling; recent-apps/notification privacy; CSV; passphrase-encrypted backup; validated merge/replace restore; delete-all | Wrong password/corrupt/incompatible backup rejected before mutation; exact round trip; replace/delete require confirmation | 4, privacy baseline in 1–2 |
| 7.17 | Material 3 light/dark; five-tab navigation; typography/spacing/icon/color tokens; accessible forms/charts; Undo; haptics; reduced motion | Contrast, 48 dp targets, TalkBack order, 200% text, 60 fps scrolling, <=300 ms motion, <=100 ms feedback | 2, review in 4 |

### Dashboard and reports work items

- [ ] Day/week/month/year/custom period, previous/next and swipe; month default; multi-account filtering.
- [ ] Sync chip with last successful scan, paused/stale (>24 hours) warnings, tap-to-sync and pull-to-refresh.
- [ ] Mask all amounts with privacy toggle, including charts and accessibility descriptions.
- [ ] Summary: expenses, income, net difference (never bank balance), previous-period amount/percentage with text/arrows, sparkline, recorded-activity coverage note. Show zero-baseline percentage as unavailable rather than infinity.
- [ ] Attention: review, uncategorized, unclassified-credit counts, filtered actions, hidden when empty.
- [ ] Category chart/list: amount/share/count; expand Other; table alternative and description.
- [ ] Spending bars by day/month with prior-period comparison, tap drill-down and long-press tooltip.
- [ ] Latest ten records: unknown payee fallback, category, signed amount, account/date, uncertainty badges, swipe categorize/edit, View all.
- [ ] Expanding primary action: Add Expense, Add Income, Sync Now.
- [ ] Optional calendar heatmap with day numbers/legend and day drill-down.
- [ ] Optional top payees with counts; unknown payees grouped explicitly.
- [ ] Optional largest five transactions; payment-method/account splits including unknown.
- [ ] Optional quick stats: daily average, count, largest spend, no-spend days; disclose denominator and recorded-activity basis. Scalar statistics open contributing records with an explanation rather than claiming their sum equals an average/count.
- [ ] Hide/reorder optional widgets; required widgets cannot be hidden.
- [ ] Guided first-run empty state without fake data; empty-period shortcut; loading skeletons; persistent capture-paused fix banner; visible uncertainty.
- [ ] One synchronized period/account update, immediate edit refresh, Undo for reversible changes, Indian grouping and tabular numerals; compact values reveal full amount.
- [ ] Income/expenses, daily/weekly/monthly trends, category/method/largest/previous-period reports; refunds/transfers/fees separate.

## Implementation sequence and release gates

### Preparation and device feasibility

- [ ] Confirm installed Android/realme UI version, exact HDFC sender IDs, and available APK-transfer/Wi-Fi route.
- [x] Set up a compatible JDK, Android command-line SDK packages, platform tools, and pinned Gradle wrapper/build dependencies. Record versions and download verification; keep machine-specific SDK paths out of source.
- [ ] Current machine: `java` resolves to Java 8; `javac` resolves to JDK 25. No `gradle` or `adb` command was found, and the default user Android SDK directory does not exist. Configure a consistent supported JDK for the selected Gradle/Android plugin combination.
- [x] Create project architecture and implementation notes, lint/format/build commands, and the requirement-derived fixture suite.
- [ ] Produce a small permission/capture APK and validate sideloaded SMS permissions and actual broadcast delivery before expanding the UI. Record OEM behavior and installation steps.

### Milestone 1 — Capture and trust

- [ ] Build onboarding, safe filtering, JSON parser, storage/migrations, dedup, scan status/cursor, preview/apply backfill and re-scan, initial review UI, and retention boundaries.
- [ ] Pass parser positive/negative, repeated/concurrent scan, crash recovery, provenance, and storage tests.
- [ ] Validate real HDFC capture end to end on the phone; measure receiver reliability with screen locked, idle, background, restart, force-stop/reopen, and revoked permissions. Do not claim force-stopped background delivery.
- [ ] Decide whether periodic catch-up is justified from measurements; otherwise leave it off. Establish minimum supported Android version from the agreed support scope and validate it.

Exit: fixture/concurrency checks pass AND device capture is demonstrated. An APK build alone cannot satisfy this gate.

### Milestone 2 — Daily use

- [ ] Build Material 3 design tokens/navigation, categorization/notifications, manual entry/editing, accounts/categories, required dashboard widgets, history/search/filtering, reports, and manual linking.
- [ ] Add notification privacy from its first implementation; do not postpone sensitive-data protection until the final milestone.
- [ ] Validate offline/no-permission workflows, notification actions, all classifications and filters, Undo, and totals drill-down.
- [ ] Conduct the PRD's two-week daily-use trial with the user. Development may continue while the trial runs; the gate remains unpassed until the trial actually finishes.

### Milestone 3 — Dashboard depth

- [ ] Implement all Should widgets, optional layout settings, chart tables/descriptions, and complete drill-down behavior.
- [ ] Run reconciliation tests across all period/account/classification combinations and measure dashboard performance with 10,000 records on the phone.

### Milestone 4 — Safety and recovery

- [ ] Complete app lock and privacy settings, local-storage protection decision, CSV/export controls, passphrase-encrypted full backup, merge/replace restore, retention cleanup/settings, and delete-all.
- [ ] Use authenticated encryption, a vetted password KDF with per-backup salt, fresh nonces, format/version metadata, and bounded input sizes. Never store the passphrase or write an intermediate plaintext backup.
- [ ] Export through an explicit document destination; disclose that a user-chosen cloud document provider can upload it. Disable unintended Android automatic backup/data transfer for financial files.
- [ ] Restore transaction/account/category/provenance/template/source/review/link/settings metadata with documented collision handling. Do not restore device Keystore material or advance a different device's inbox cursor blindly.
- [ ] Authenticate and validate the entire backup before mutation; stage and commit transactionally. Merge skips trustworthy duplicates, preserves user decisions, and flags conflicts; replacement requires explicit confirmation.
- [ ] Pass backup round-trip, wrong-passphrase/corruption/version/collision/replace tests, migration tests, accessibility review, performance review, and signed-release smoke tests.

Exit: records and their supported metadata survive export/restore exactly; privacy and device checks pass. Preserve a stable signing identity so updates retain installed app data.

## Build, test, and deploy without Android Studio or USB

The official SDK command-line tools plus Gradle can build the native app. Do not install Android Studio or require an emulator. Resolve compatible tool versions during setup rather than guessing them in this plan.

Preferred device route: Android 11+ wireless debugging, if permitted and the PC and phone share a reachable Wi-Fi network. Enable Developer options and Wireless debugging on the phone, choose pairing code, then use `adb pair <address>:<pairing-port>` and, if needed, `adb connect <address>:<connection-port>`. Pairing and connection ports can differ. This route needs no initial USB connection. Do not attempt to bypass office network/device controls.

Fallback: build a signed APK locally and transfer it using an available permitted channel; install from the phone's file manager after allowing that installer. Run a guided device acceptance checklist and return non-sensitive results. Without ADB or another device connection, laptop-driven instrumentation, logs, and performance measurement remain unavailable; a debug-only on-device diagnostics screen can expose aggregate test/status results without exporting financial messages. Establish an actual transfer channel before promising deployment.

Planned commands after project/toolchain setup:

```powershell
.\gradlew.bat :core:test :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
.\gradlew.bat :app:connectedDebugAndroidTest
.\gradlew.bat :app:installDebug
```

The last two require a connected wireless device. Add the selected formatter's check task when configuring the project. Release preparation includes a release build, signature verification, release lint/tests, upgrade-with-data preservation, and an offline smoke test. Signing secrets stay outside version control and must remain available for future updates.

Reference documentation:

- SDK setup: https://developer.android.com/tools/sdkmanager
- Command-line builds/signing: https://developer.android.com/build/building-cmdline
- Wireless pairing: https://developer.android.com/tools/adb
- SMS restrictions: https://developer.android.com/reference/android/Manifest.permission#READ_SMS

SMS permissions are hard restricted and depend on installer allowlisting as well as runtime consent. The PRD's restricted-settings advice is not a guarantee or a universally sufficient fix. Validate the actual supported installation route on this phone before relying on SMS access.

## Contract clarifications before affected implementation

1. PRD 7.5 uses `unknown`, while 7.6/Appendix A use `unclassified`. Proposed canonical classification: `unclassified`, with explicit unknown provenance for absent facts.
2. PRD 7.3 includes refund/reversal message types; Appendix B's allowed types omit them. Extend the documented built-in schema before adding those templates; unsupported formats safely enter review meanwhile.
3. PRD field provenance lists parsed/user/unknown, but debit defaults and approximate timestamps are system-derived. Add an explicit system-derived/default state or orthogonal origin metadata rather than labeling an inference as parsed.
4. The success criterion that every figure equals the sum of its transactions applies directly to monetary totals, not averages, counts, percentages, or no-spend days. Show contributing records and the calculation basis for those metrics.
5. Define account-disable behavior for known suffixes versus messages with no account suffix; do not silently discard unknown-account financial messages.
6. Define week start, timezone behavior, elapsed versus full prior-period comparison, no-spend-day denominator, and link/unlink behavior. Keep these explicit in reporting tests before building charts.

The user approved resolutions 1–4 on 2026-10-13. Items 5–6 remain design details to settle before the affected behavior is implemented. Week start/period boundaries and account-disable semantics must be documented and tested, with clarification if a business-rule conflict remains.

## Deferred backlog

- [ ] Additional banks; custom template import/export; guided builder; version rollback/advanced priority controls.
- [ ] Automatic historical reprocessing after template change; automatic refund/reversal linking; advanced mandate matching/forecasting.
- [ ] AI-assisted template generation or on-device fallback, with validation/review and explicit external-sharing disclosure where applicable.
- [ ] Budgets/alerts; recurring-payment insights; advanced analytics.
- [ ] Cloud/multi-device synchronization only after an explicit product-direction change.

## Tracking and evidence

For each implemented feature, record code location, tests/check results, and outstanding device evidence in implementation notes. Mark a checkbox complete only when its corresponding behavior is demonstrated. Report parsing accuracy separately from available-SMS coverage, duplicate rate, false positives, and manual-review rate. Targets: accepted amount/direction 100% correct; present optional fields >=95%; repeated/concurrent duplicates zero; negative-case non-financial retention zero.

Planning validation: all PRD functional sections 7.1–7.17, template rules, workflow/edge cases, non-functional targets, release gates, and deferred lists were reviewed and mapped above. The JVM core compiled and all 16 parser tests passed on 2026-10-13. Android/device and remaining feature validation are still pending; see implementation notes for current evidence.
