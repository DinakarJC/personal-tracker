# Personal Expense Tracker — Product Requirements Document

**Version:** 3.1 — MVP Revision  
**Status:** Proposed MVP baseline  
**Platform:** Android, distributed privately by sideloading  
**Product scope:** Single user, local-first, no required backend or account

---

## Revision Notes (v3.1)

- Dashboard expanded into a full specification with design principles, widget catalogue, behavior, states, and a totals rule (7.14). New Design System and UX Standards (7.17).
- Reprocessing scope clarified: the MVP covers backfill and manual re-scan; re-reading history after template changes is deferred (6, 8.2, 8.3, 14).
- Measurable success targets restored (12).
- Built-in templates must use the data schema from day one (8.1, Appendix B).
- Advance notices explicitly create no record (7.3).
- Field-level provenance required for every stored value (7.5).
- Default classification rules added (7.6).
- Battery-optimization guidance and restricted-settings handling added (7.1, 7.10).
- App lock and locked-device privacy restored as requirements (7.16).
- Open questions converted to decisions, and build milestones added (14, 15).
- Duplicate closing principle removed. Test fixtures (Appendix A) and template schema (Appendix B) added.

---

## 1. Product Overview

Personal Expense Tracker helps one person maintain a useful record of income and spending with as little manual work as possible.

The MVP captures supported bank transaction SMS messages, extracts the financial details that are actually present, and creates local transaction records. The user can categorize transactions, correct uncertain details, manually record cash and other transactions that do not produce a usable SMS, and review spending through a dashboard and reports.

All transaction records remain on the device. The application does not initiate or process payments and does not depend on PhonePe, Google Pay, or any other payment app.

The product is designed to improve everyday financial awareness, not to serve as an authoritative bank ledger. It cannot guarantee that every bank transaction generates an SMS or that every SMS contains complete information.

## 2. Problem Statement

Spending happens across UPI, cards, bank transfers, automatic debits, and cash. Manually entering every transaction is tedious, so records are often incomplete.

Bank transaction SMS messages can provide an app-independent source of information for many account transactions. However, message formats vary, alerts may be delayed or missing, and recipient details may be incomplete.

The application must automate what it can, make uncertainty visible, and provide a straightforward way to correct or add missing information.

## 3. Product Principles

1. **Never guess a financial fact.** Missing recipient, amount, date, or category information remains unknown until supplied by a reliable source or the user.
2. **Accuracy over automation.** An uncertain record must be reviewed rather than silently accepted as correct.
3. **Local by default.** No account, backend, or automatic transmission of financial data is required.
4. **One transaction, one record.** Repeated message delivery or scans must not create duplicate transactions.
5. **User edits take precedence.** Reprocessing must not overwrite user-corrected information.
6. **Be clear about coverage.** The app must explain that bank SMS capture is not guaranteed to include every transaction.
7. **Do not confuse recorded activity with bank balance.** Calculated totals are based on the records available in the app.
8. **Every number is traceable.** Any total, chart segment, or insight can be opened to the transactions behind it.

## 4. Goals

- Automatically capture and parse supported bank transaction SMS messages.
- Record the amount, direction, date, account, transaction reference, and counterparty when those fields are present.
- Reduce categorization effort with quick actions and optional suggestions.
- Support manual expenses and income for cash and transactions not captured by SMS.
- Provide a review queue for uncertain, unparsed, or potentially duplicated records.
- Show income, spending, category breakdowns, and trends through a rich, accessible dashboard where every figure can be traced to its transactions.
- Keep records locally and provide a user-controlled export and restore process.
- Allow the parser to support additional bank message formats without redesigning the transaction features.

## 5. Non-Goals

The MVP will not:

- Initiate, process, or hold payments.
- Integrate with PhonePe, Google Pay, or other UPI applications.
- Read UPI PINs, banking passwords, or payment credentials.
- Claim to capture every bank transaction.
- Claim that a parsed SMS independently verifies final settlement.
- Connect to bank APIs or account aggregation services.
- Require a cloud account, backend, or subscription.
- Automatically read or retain unrelated personal SMS messages.
- Include investment, lending, tax, or full accounting features.
- Include a general-purpose AI parser in the first release.
- Include a full visual template designer, template marketplace, or complex template conflict editor in the first release.

## 6. MVP Scope

### Included in the first usable release

1. SMS permission and privacy onboarding.
2. Supported-sender filtering and transaction message classification.
3. Built-in parsing templates for the user's initial bank, beginning with HDFC examples, stored as data files in the template schema (Appendix B).
4. Automatic capture, on-open catch-up, and a manual Sync action.
5. Local transaction records and duplicate prevention.
6. Transaction categorization, editing, and manual entry.
7. A review queue.
8. Rich dashboard, transaction history, search, filters, and reports.
9. Multiple detected accounts, identified by bank and account suffix when available.
10. Local export, backup, restore, and delete-all-data controls.
11. App lock and protection of sensitive content.
12. A consistent design system and accessibility standards (Section 7.17).

### Deferred until the capture engine is reliable

- Guided visual template builder.
- Importing and exporting custom parsing templates.
- Template version rollback and advanced template priority management.
- AI-assisted template creation.
- On-device language-model fallback parsing.
- Budgets and spending alerts.
- Advanced recurring-payment detection.
- Advanced mandate forecasting and mandate-notice matching.
- Re-reading historical messages automatically after a template change.
- Automatic refund and reversal linking. Manual linking remains in the MVP.
- Cloud synchronization or multi-device support.

The parsing system should be designed so these features can be added later, but they must not delay a dependable first release.

## 7. Functional Requirements

### 7.1 Permissions and Onboarding

On first launch, the app shall explain:

- Why SMS access is needed.
- Which messages it attempts to process.
- What information is stored locally.
- That SMS alerts may be missing, delayed, or incomplete.
- That cash expenses and other transactions without usable SMS may need manual entry.
- How to revoke permissions, export records, and delete local data.

The app shall request only permissions necessary for the selected MVP features. If permission is denied or later revoked, the app shall remain usable for manual entry and clearly explain that automatic capture is paused.

The app must not claim that sideloading guarantees background capture. Supported Android versions and the required permission flow must be verified on target devices.

On newer Android versions, a sideloaded app may need the user to allow restricted settings before SMS access works. Onboarding shall detect this and guide the user through it.

Where battery management may stop background delivery, the app shall explain this and guide the user to exempt the app from battery optimization. This is the user's choice and shall not be requested repeatedly.

### 7.2 SMS Intake and Filtering

The application shall use a controlled processing pipeline for incoming messages and catch-up scans.

- Only messages from explicitly configured bank sender IDs shall be eligible for financial-message classification.
- Sender matching shall use a normalized, reviewed allowlist rather than trusting a broad user-imported pattern alone.
- Eligible messages shall be classified before any raw text is retained.
- Messages confidently classified as unrelated or non-financial shall be discarded without being stored.
- Messages from a configured bank sender that plausibly concern a financial transaction but cannot be parsed may be retained in the review queue.
- Messages that are ambiguous and do not contain sufficient evidence of a financial transaction shall not be stored automatically.
- The app shall never store all SMS content indiscriminately.
- A sender ID is not, by itself, proof that every message from that sender is a transaction alert.

The app shall disclose that local processing reduces data exposure but does not make SMS content inherently trustworthy. Parsed data must be treated as unverified input until validated.

### 7.3 Supported Message Types

The MVP shall recognize these logical message types:

- `debit`: money deducted from an account.
- `credit`: money credited to an account.
- `mandate_debit`: a completed or reported automatic debit.
- `reversal_or_refund`: money returned or a prior transaction reversed, when identifiable.
- `non_transaction_notice`: an informational message that does not itself create a transaction, such as an advance notice of a scheduled deduction. It is classified, creates no record, and its raw text is not retained.
- `unknown_financial`: a plausible financial message that could not be classified confidently.
- `non_financial`: a message that must not be stored.

A debit is not automatically classified as an expense. A credit is not automatically classified as earned income. Transaction direction and financial meaning must be separate fields.

Mandate advance notices and future scheduled deductions (for example, "will be deducted on …") shall be classified as `non_transaction_notice`. They shall not create a transaction record, shall not be counted, and shall not be stored. The completed deduction is captured from the later `mandate_debit` message. Mandate forecasting and notice-to-debit matching can be added later.

### 7.4 Parsing and Data Validation

- Parsing shall use built-in, versioned templates for supported message formats.
- A bank may have different formats for debit, credit, card, reversal, and mandate messages.
- The parser shall classify the message before selecting the relevant extraction template.
- Missing fields shall remain empty.
- Amounts must be parsed as valid currency values, with commas and decimal separators handled according to the template.
- Dates must be validated and normalized. Ambiguous dates must be flagged instead of guessed.
- If the message lacks a transaction time, the app may use the SMS received time as an approximation and must label it as such.
- Account suffixes shall be normalized when the message format supports reliable extraction.
- Extracted values shall be validated against the source message and expected format.
- A template match alone does not prove that the message represents a completed or settled transaction.
- If the app cannot confidently parse a plausible financial message, it shall place it in the review queue rather than silently discard it.

For the first release, built-in templates shall be tested against representative real message formats supplied by the user, with personal details replaced by safe test values. Templates must not be considered complete based only on illustrative examples.

### 7.5 Transaction Record

Each record shall support these fields where applicable:

- Local unique record ID.
- Transaction direction: debit or credit.
- Financial classification: expense, income, transfer, refund, reversal, fee, or unknown.
- Amount and currency.
- Transaction date and time, including whether the time is approximate.
- Bank and account suffix, if present.
- Counterparty or payee/source name, if present.
- Counterparty UPI ID, if present.
- Payment method, if known: UPI, card, bank transfer, mandate, cash, or other.
- Transaction reference, if present.
- User-selected category and optional note.
- Source: SMS or manual.
- Parsing status and review reason.
- Field-level provenance: for each field, whether the value was parsed, set by the user, or unknown. Reprocessing and template changes must never overwrite user-set values.
- Created and last-updated timestamps.
- Parser template ID and version for SMS-derived records.
- Original SMS text only where retained under the rules in Section 7.11.

Unavailable values must remain unknown. The application shall not fabricate recipient names, categories, references, or balances.

### 7.6 Financial Classification

The app shall distinguish the bank's reported movement of money from the user's spending or earnings.

Examples:

- A debit to a merchant may be an expense.
- A transfer between the user's own accounts is a transfer and must not be counted as spending.
- A credit may be salary, a refund, a transfer, cashback, a gift, or another source. The SMS may not reveal which.
- A refund or reversal should be linked to its original transaction when a reliable reference exists.
- A bank fee may be an expense even if it is not a purchase.
- A message that does not provide enough information shall remain unclassified or be sent to review.

The app shall support user correction of transaction classification. It must not silently infer a transfer between the user's own accounts solely because the account suffixes differ or the amount matches another transaction.

**Default classification:**

- A debit defaults to `expense`, and the user can reclassify it (for example, as a transfer).
- A credit defaults to `unclassified`. It is not counted as income until the user assigns an income type or classification. Unclassified credits are shown separately on the dashboard.

### 7.7 Categorization

- Each expense shall have a category or be marked Uncategorized.
- Income shall have an income type or be marked Uncategorized.
- A notification or in-app quick action shall allow categorization with minimal effort.
- Notifications may be grouped or configured to reduce noise.
- Category suggestions may use previously confirmed classifications for the same counterparty, but suggestions must be clearly presented and never applied silently.
- The user may create, rename, and reorder categories.

Suggested expense categories:

- Food and groceries
- Shopping
- Transport
- Bills and utilities
- Housing
- Health
- Education
- Entertainment
- Travel
- Personal care
- Investments and savings
- Bank fees
- Transfers and lending
- Other

Suggested income types:

- Salary
- Freelance
- Refunds
- Cashback and rewards
- Gifts
- Transfers received
- Other

Refunds and transfers must be reported separately from earned income.

### 7.8 Manual Entry

The user shall be able to add an expense or income without SMS capture.

The form shall support:

- Amount.
- Date and time.
- Category or income type.
- Payment method.
- Payee or source, optional.
- Note, optional.

The user shall be able to edit or delete manually entered records. If a manually entered transaction may duplicate an SMS-derived transaction, the app shall warn the user and offer a review option rather than automatically merging uncertain matches.

### 7.9 Review Queue

The review queue shall include:

- Plausible financial messages that could not be parsed.
- Transactions with missing or inconsistent essential fields.
- Possible duplicates.
- Transactions with uncertain financial classification.
- Refunds or reversals that may relate to existing records.
- Transactions requiring user categorization, if the user chooses to review them there.

For each item, the app shall explain why it needs attention. The user may correct it, classify it, mark it as not a transaction, link it to another record, or dismiss it.

Dismissing an item shall not silently delete a confirmed transaction. Where relevant, the app shall preserve a minimal review outcome so the same message does not repeatedly return.

### 7.10 Capture and Catch-Up

The MVP shall use three ways to process eligible messages:

1. **Incoming-message processing:** Process eligible incoming bank messages when Android delivers them and permissions allow.
2. **On-open catch-up:** When the user opens the app, process eligible messages newer than the last successful scan.
3. **Manual Sync:** The user can request a catch-up scan.

A periodic background scan is optional for the first release. It should be added only after confirming that it materially improves reliability on target devices without excessive battery use.

Requirements:

- Save a processing cursor and advance it safely only after messages have been processed or recorded for review.
- A failed scan must be retryable.
- A process restart must not lose already captured records.
- The app shall display the last successful scan time and explain when automatic capture is paused.
- Because catch-up reads the device inbox from the saved cursor, a missed real-time delivery delays capture and the categorization prompt but does not lose the transaction, provided the message is still in the inbox.
- Milestone 1 shall measure real-time delivery reliability on the target device and decide whether the optional periodic scan is enabled by default.
- The app shall not promise real-time capture if Android permissions, battery controls, device state, or message delivery prevent it.
- Initial backfill shall require explicit user action and a clear date range.
- The app shall show a summary of records to be added and possible duplicates before applying a large backfill.

### 7.11 Raw SMS Retention

Raw SMS text can contain sensitive personal and financial information. It shall not be retained indefinitely by default.

- Successfully parsed records should retain extracted fields and a reference to their source, not necessarily the full raw text forever.
- Raw text may be retained for an unresolved financial message so the user can review and repair the parser.
- The user shall be able to delete raw message text while keeping the transaction record.
- Raw text retention shall be disclosed and, where practical, configurable.
- Raw text must not be included in ordinary exports unless the user explicitly chooses a full diagnostic export.
- Non-financial messages must never be retained for parser training or template building.

If a template update later requires a raw message that has already been removed from app storage, the app may re-read it from the device inbox only if the message is still available and the user has granted the necessary permission.

### 7.12 Duplicate Prevention and Transaction Linking

The app shall attempt to identify duplicate messages and related transactions using, in priority order:

1. Bank/account plus a reliable transaction reference.
2. A strong source-specific identifier, where available.
3. A combination of account, direction, amount, date/time, and counterparty.

Amount, time, and counterparty alone are not sufficient to automatically merge two records when separate real transactions are plausible.

If a match is uncertain, both records shall be preserved and flagged for review.

Refunds and reversals should be linked to an original transaction only when there is reliable evidence. A matching amount alone is not enough.

### 7.13 Accounts

The app shall detect accounts from bank sender and account suffix information when available.

Each account may display:

- Bank name.
- Last four digits or another safe account label.
- User-defined label, such as "Salary account".
- Last successful capture time.
- Capture enabled/disabled state.

The app shall not display full account numbers unless there is a separately justified need. Multiple accounts shall be kept distinct, and the app shall not assume that different account suffixes belong to the same person.

### 7.14 Dashboard

The dashboard is the app's primary screen. It must answer "How am I doing this period, and what needs my attention?" within a few seconds, then let the user drill into any figure. It shall be information-rich but organized through progressive disclosure, so depth never reduces clarity.

**Design principles**

1. **Glanceable first, detailed on demand.** The order is summary, attention items, breakdowns, trends, insights, then recent activity. Each block is a summary that opens its detail.
2. **Every number is traceable.** Tapping a total, chart segment, bar, or insight opens the filtered transaction list that produced it.
3. **Honest by design.** Coverage notes, approximate-time labels, and unclassified items are visible. Net difference is never shown as a bank balance.
4. **Actionable.** Items that need attention offer one-tap actions inline.
5. **Calm and consistent.** One component and color system, restrained motion, and no decoration that carries no information.
6. **Accessible and private by default** (Section 7.17).

**Header controls**

- Period selector: Day, Week, Month, Year, Custom, with previous/next arrows and swipe. Default is Month.
- Account filter: all accounts, or one or more selected accounts.
- Sync status chip: last successful scan time, tap to sync. It turns to a warning state when capture is paused or the last scan is older than 24 hours.
- Privacy toggle: masks all amounts until tapped.

**Widgets**

| Widget | Content | Priority |
|---|---|---|
| Summary card | Expenses (primary figure), income, and net for the period. Change versus the previous period shown as amount and percentage with an arrow and text, not color alone. Sparkline. Coverage note: "Based on recorded transactions." | Must |
| Attention strip | Counts for needs review, uncategorized, and unclassified credits. Each opens a prefiltered list. Hidden when all are zero. | Must |
| Category breakdown | Donut or stacked bar with a ranked list showing amount, share of spending, and transaction count. Small categories group into "Other" and can expand. | Must |
| Spending trend | Bars by day (week and month views) or by month (year view), with the previous period overlaid. Tap a bar to open that day. Long-press for a tooltip. | Must |
| Recent transactions | Latest 10. Each row shows payee (or "Unknown payee"), category chip, signed amount, account, and date, with a badge for review or approximate time. Swipe to categorize or edit. "View all" link. | Must |
| Quick actions | Floating button that expands to Add Expense, Add Income, and Sync Now. | Must |
| Spending calendar | Month grid heatmap of daily spend with a legend and day numbers. Tap a day to open it. | Should |
| Top payees | Highest spending by counterparty with transaction counts. Unknown payees are grouped, never guessed. | Should |
| Largest transactions | Top five in the period. | Should |
| Payment methods and accounts | Split by UPI, card, transfer, mandate, cash, and unknown. Split by account. | Should |
| Quick stats | Average daily spend, transaction count, largest single spend, and number of no-spend days. Each shows its basis. | Should |

Widgets marked Should can be hidden or reordered by the user. Widgets marked Must cannot be hidden.

**Totals rule**

- **Expenses:** debits classified as expense or fee.
- **Income:** credits classified as income.
- **Shown separately and excluded from both:** transfers, refunds and reversals, and unclassified credits.
- Records marked "not a transaction" are excluded everywhere.

**Behavior**

- Pull to refresh runs Sync Now.
- Changing the period or account updates all widgets together with a transition of 300 ms or less, using skeleton placeholders so the layout does not jump.
- Every chart offers a "view as table" option and an accessible text description.
- Editing or deleting a transaction updates the dashboard immediately. Destructive actions offer Undo.
- Currency uses Indian digit grouping (for example, ₹1,23,456) with tabular figures so amounts align. Large values may use a compact form (for example, ₹1.2L) with the full value on tap.

**States**

- **First run or no data:** a guided empty state with steps (grant SMS access, run a backfill, or add a first manual entry). No fake sample data.
- **Capture paused or permission revoked:** a persistent banner with the reason and a fix action. Manual entry stays available.
- **Empty period:** a message with a shortcut to change the period.
- **Loading:** skeleton placeholders, never a blank screen.
- **Uncertain data:** surfaced through the attention strip and row badges, not hidden from totals unless the totals rule excludes them.

### 7.15 History, Search, and Reports

The user shall be able to:

- Search by payee, source, note, or transaction reference.
- Filter by date range, direction, financial classification, category, account, payment method, and review status.
- Sort by date or amount.
- View and edit a transaction.
- See the source and any uncertainty associated with an SMS-derived record.

Reports shall include:

- Income versus expenses.
- Daily, weekly, and monthly trends.
- Category-wise spending.
- Payment-method breakdown where known.
- Largest transactions.
- Comparison with a previous period.
- Refunds, transfers, and fees reported separately from ordinary income or expenses.

Reports must be based on classified records and must clearly disclose that unrecorded activity may be missing.

### 7.16 Local Privacy and Data Recovery

- All transaction records shall be stored locally.
- No financial data shall be transmitted automatically to a remote service.
- Local data shall be protected against casual access, and sensitive stored data should be encrypted where feasible.
- An app lock shall use device biometric authentication with device PIN or passcode fallback, an optional lock on every launch or after a configurable timeout, and clear behavior if the device's biometrics change.
- App content shall be hidden in the recent-apps preview, and notifications shall hide amounts and counterparties while the device is locked. Both shall be user-adjustable settings, on by default.
- Users shall be able to export transactions to CSV and create a full backup.
- Full backups shall be protected appropriately and shall not be silently uploaded to cloud storage.
- Restore shall validate the backup and warn about duplicates or replacement of existing records.
- The app shall explain that uninstalling the app or clearing its data may delete records that have not been exported.
- Users shall be able to delete all local data.
- The app shall explain that copying SMS samples to an external AI or other external tool sends those samples outside the device. This action is not part of automatic processing and must require an explicit user action.

The product may claim "no automatic network transmission" only if no feature silently sends data off-device. It must not claim that data never leaves the device if the user chooses to export or share it.

### 7.17 Design System and UX Standards

- **Platform and theme:** Material Design 3 on Jetpack Compose, with light and dark themes that follow the system setting. Dynamic color is optional.
- **Navigation:** bottom bar with Home, Transactions, Reports, Review (with a count badge), and More (accounts, categories, settings, backup). Primary actions sit within thumb reach for one-handed use.
- **Visual hierarchy:** a defined type scale, large numerals for key figures, tabular figures for amounts, an 8 dp spacing grid, and one icon set.
- **Color:** semantic tokens for expense, income, neutral, warning, and error. Every meaning is also conveyed by an icon, a sign (+/−), or a label. Chart palettes must be distinguishable for color-blind users.
- **Accessibility:** WCAG 2.1 AA contrast (4.5:1 for text, 3:1 for graphics), touch targets of at least 48 dp, TalkBack labels and logical reading order, text scaling up to 200% without clipping, reduced-motion support, and text alternatives for charts.
- **Feedback:** visible response to a tap within 100 ms, haptics for confirmations, and a snackbar with Undo for edits, deletions, and dismissals. Confirmation dialogs are reserved for irreversible actions such as delete-all and restore-replace.
- **Forms:** smart defaults (current date and time, last-used payment method, and last category for the same payee as a suggestion), numeric keypad for amounts, inline validation, and the fewest taps to save.
- **Microcopy:** plain, specific, and non-judgmental. The app explains uncertainty ("The bank message did not include a payee") instead of showing error codes.
- **Notifications:** show the top suggested categories as quick actions plus a "More" option, and follow the locked-device privacy rule.
- **Motion:** purposeful, 300 ms or less, and disabled when the system reduced-motion setting is on.
- **Performance:** 60 fps scrolling, virtualized lists, and no visible jank in charts.

## 8. Parsing Templates — MVP and Future Extension

### 8.1 MVP Template Scope

The first release shall use built-in, versioned templates for the user's initial bank and the transaction types needed for daily tracking.

Built-in templates shall be stored as data files in the schema in Appendix B and loaded by the parser at runtime. They shall not be hard-coded, so template import can be added later without changing the parser.

Each template shall specify:

- Stable template ID and version.
- Bank and verified sender ID.
- Message type.
- Expected field pattern.
- Date format and normalization rules.
- At least one positive example with expected extracted fields.
- Negative examples that must not match.
- A record of the parser version that produced each transaction.

The initial bank templates must be tested against representative messages from the actual device, with sensitive values replaced by safe test data before examples are included in documentation. A fixture suite with positive and negative cases and expected outputs (Appendix A) is required, and every template must pass it.

### 8.2 Safe Reprocessing

Templates must be validated before use. A template must not execute code or make network calls.

In the MVP, historical messages are read only by the initial backfill and by a manual re-scan over a user-selected date range. Automatic re-reading of history after a template change is deferred (Section 8.3).

Before applying a backfill or re-scan, the app shall preview:

- New records to be created.
- Existing records whose empty fields could be filled.
- Records that would remain unchanged.
- Possible duplicates.
- Messages that remain unparsed.

The user must confirm before changes are applied. User-edited fields must never be overwritten automatically.

### 8.3 Deferred Template Builder and Imports

A guided template builder, custom template import/export, AI-assisted template creation, re-reading history after template changes, template priorities, and rollback UI are deferred until the built-in parser is reliable.

The product's internal design should allow these capabilities later without requiring changes to the transaction record model.

If custom templates are introduced later:

- Imported templates must be disabled until validated and reviewed.
- The schema and all patterns must be validated.
- Each template must include positive and negative examples.
- Pattern execution must have a time limit.
- A broad sender pattern must not automatically grant permission to retain messages.
- The user must preview historical reprocessing before applying it.
- Template versions and the source template for each record must be retained.

## 9. Core User Flows

### 9.1 Automatic Capture

1. An eligible bank SMS arrives.
2. The app checks the configured sender and classifies the message.
3. A supported template extracts available fields.
4. The app validates the fields and checks for duplicates.
5. The transaction is stored locally, or an unresolved financial message is placed in the review queue.
6. The user categorizes the transaction now or later.
7. The dashboard and reports update based on the saved classification.

### 9.2 Catch-Up Sync

1. The user opens the app or taps Sync Now.
2. The app scans eligible messages newer than the last successful cursor, or the selected historical range.
3. Messages are classified and parsed through the same processing pipeline as incoming messages.
4. Existing records are matched to prevent duplicates.
5. Uncertain matches are flagged rather than automatically merged.
6. The app reports how many records were added, skipped, updated, or placed in review.

### 9.3 Manual Entry

1. The user selects Add Expense or Add Income.
2. The user enters the amount, date, category/type, and optional details.
3. The app warns about plausible duplicates where possible.
4. The user saves the record.
5. The record appears in history and reports.

### 9.4 Review an Unparsed Message

1. The user opens the review queue.
2. The app explains which fields could not be extracted.
3. The user corrects the transaction or marks the message as not a transaction.
4. The app stores the user's decision and prevents the same message from repeatedly returning without reason.

## 10. Edge Cases

The MVP shall handle or clearly communicate:

- SMS is delayed, missing, or never sent.
- The app lacks permission or permission is revoked.
- Android delays or blocks background processing.
- The same message is processed by incoming handling and catch-up.
- A transaction has no reference or counterparty.
- A bank changes its message format.
- Multiple accounts or banks share a device.
- A reversal or refund arrives after the original transaction.
- A transfer between own accounts could be mistaken for spending.
- A manual record may duplicate an SMS-derived record.
- The user edits a transaction before a template update.
- Two templates could match the same message.
- A template change produces different extracted values.
- A message from a bank sender is informational rather than transactional.
- A backup is corrupted or restored over existing data.
- The app is force-stopped, the device restarts, or the inbox no longer contains historical messages.
- The user deletes raw message text but keeps the transaction record.

## 11. Non-Functional Requirements

- The core app must work offline.
- Processing a single eligible message should normally complete within one second.
- App restarts and process death must not lose saved transactions.
- A failed scan must be recoverable without rescanning the entire inbox unnecessarily.
- Data migrations must preserve existing user records.
- The app shall expose last successful sync/capture status.
- Background activity should be limited and should not cause excessive battery use.
- Supported Android versions and permission requirements must be validated on target devices before release.
- Financial totals must be reproducible from the stored records and classification rules.
- The dashboard shall render within one second with 10,000 stored transactions on the target device, using indexed or pre-aggregated queries.
- Accessibility requirements in Section 7.17 apply to every screen.

## 12. Success Criteria

The MVP will be considered useful when:

1. Supported transaction SMS messages are captured and parsed accurately on the user's target device.
2. Parsing accuracy is measured separately from SMS delivery coverage.
3. No duplicate transactions are created by repeated or concurrent scans in the test suite.
4. Unparsed plausible financial messages appear in the review queue rather than being dropped silently.
5. Non-financial messages are not retained.
6. User-edited fields survive syncs and parser updates.
7. Dashboard totals match the stored, classified records.
8. Transfers, refunds, and reversals are not incorrectly counted as ordinary income or spending.
9. Export and restore preserve records and their key metadata.
10. The user can add cash expenses and income without relying on SMS.
11. The app clearly shows when capture is paused or when data may be incomplete.
12. The app remains useful when SMS permission is unavailable, through manual entry and existing local records.
13. A captured transaction can be categorized in one tap from its notification, or in two taps in the app.
14. Every dashboard figure opens the transactions that make it up, and the sum of those transactions equals the figure.
15. The dashboard meets the performance target in Section 11 and passes an accessibility review (contrast, TalkBack, text scaling).

**Targets** (measured on the fixture suite and a representative set of real messages):

- Amount and direction: 100% correct on accepted records. Anything uncertain goes to review instead of being accepted.
- Reference, date, account suffix, and counterparty: at least 95% field-level accuracy where the field is present in the message.
- Duplicates: zero in repeated and concurrent scan tests.
- Non-financial messages retained: zero in negative tests.

Report separately:

- Coverage of eligible transactions that actually generated available SMS messages.
- Field-level parsing accuracy.
- Duplicate rate.
- False-positive rate.
- Percentage of records requiring manual review.

Do not claim coverage of all real bank transactions based only on the messages the app receives.

## 13. Risks and Mitigations

| Risk | Mitigation |
|---|---|
| Bank does not send an SMS for a transaction | Disclose the limitation; support manual entry and reconciliation |
| Message format changes | Versioned templates, review queue, and regression tests |
| Recipient information is missing | Leave it unknown and allow user correction |
| Duplicate fallback incorrectly merges real payments | Require strong matching evidence; flag ambiguous cases |
| Transfers are counted as expenses | Separate transaction direction from financial classification |
| Refunds and reversals distort reports | Link only when supported by reliable identifiers; report separately |
| Android blocks or delays background capture | On-open and manual catch-up; disclose capture state |
| SMS contains sensitive data | Sender allowlist, classification before retention, minimal raw-text retention |
| Template pattern matches too broadly | Positive and negative tests, validation, time limits, user review |
| User loses device data | Export, protected backup, restore validation, clear warning |
| User shares a sample with an external AI | Explicit disclosure and optional masking before sharing |
| Sideloaded app blocked from SMS access by restricted settings | Verify in Milestone 1; guide the user through allowing it during onboarding |
| Real-time delivery unreliable on the device | On-open and manual catch-up; battery-optimization guidance; optional periodic scan |
| Dashboard becomes cluttered or slow | Progressive disclosure, hide/reorder for optional widgets, pre-aggregated queries, performance target |

## 14. Release Plan

### MVP — Personal Daily Use

**Milestone 1 — Capture and trust**

- Device validation: SMS permission on the sideloaded app, real-time delivery reliability, and minimum Android version.
- Onboarding and permission handling.
- Built-in HDFC templates as data files, with classification, parsing, and validation.
- Local storage, duplicate prevention, processing cursor, catch-up, and backfill with preview.
- Review queue.
- Fixture test suite (positive and negative).

*Exit:* fixtures pass, concurrent-scan tests create no duplicates, and real SMS on the device is captured end to end.

**Milestone 2 — Daily use**

- Categorization from notifications and in-app, manual entry, and editing.
- Accounts page.
- Dashboard Must widgets, transaction history, search, filters, and reports.
- Design system (Section 7.17).

*Exit:* the app has been used for two weeks of real daily spending.

**Milestone 3 — Dashboard depth**

- Spending calendar, top payees, largest transactions, payment-method and account splits, and quick stats.
- Hide and reorder for optional widgets.
- Drill-down from every figure.

*Exit:* every figure reconciles to its transaction list and meets the performance target.

**Milestone 4 — Safety and recovery**

- App lock, recent-apps privacy, and locked-device notification privacy.
- CSV export, encrypted backup, restore with validation, and delete-all.
- Raw SMS retention controls.
- Accessibility and performance review.

*Exit:* a backup and restore round trip reproduces all records exactly.

### Later Release — Extensible Parsing

- Additional built-in banks.
- Custom template import/export.
- Guided template builder.
- Re-reading history after template changes, and rollback controls.
- Automatic refund and reversal linking.
- Advanced template priority management.
- AI-assisted template generation, with explicit external-sharing warnings.
- Optional on-device fallback parser, always subject to validation and review.

### Later Enhancements

- Budgets and alerts.
- Recurring transaction insights.
- More advanced analytics.
- Optional synchronization only if the product direction changes to allow remote storage.

## 15. Decisions and Open Items

### Decisions (v3.1)

1. Unresolved raw financial messages are retained for 30 days by default, configurable.
2. Notifications are immediate by default and configurable (immediate, grouped, or off).
3. SMS-derived records are labeled "Reported by bank SMS" and manual records "Manual". No record is labeled "verified".
4. Full backups are encrypted with a user-provided passphrase. Restore skips duplicates by default and offers an explicit, confirmed option to replace existing data.
5. Debits default to expense and credits default to unclassified (Section 7.6).

### Must be resolved in Milestone 1

1. Does SMS permission work on the sideloaded app on the target device, and what user steps are required?
2. What is the minimum supported Android version? Set it from the target device and validate it.
3. What are the real bank sender IDs as shown on the device?
4. Which message types does the first bank's test coverage include? At minimum: debit, credit, mandate debit, and advance notice. Add card, reversal, and salary-credit samples when available.
5. Is real-time delivery reliable enough on this device, or should the periodic scan be on by default?

---

## Appendix A: Test Fixtures (Redacted)

All values are placeholders (account, amounts, dates, references, names, UPI IDs). Only the message formats match real bank messages. Each fixture must produce exactly the output shown. Replace or extend these with real redacted messages from the device during Milestone 1.

### A.1 Debit (UPI)

```
Sent Rs.450.00
From HDFC Bank A/C *1234
To JOHN D
On 15/09/26
Ref 100000000001
Not You?
Call 18002586161/SMS BLOCK UPI to 7308080808
```

```json
{
  "message_type": "debit",
  "creates_record": true,
  "direction": "debit",
  "default_classification": "expense",
  "amount": 450.00,
  "currency": "INR",
  "account_suffix": "1234",
  "counterparty_name": "JOHN D",
  "counterparty_upi_id": null,
  "reference": "100000000001",
  "date": "2026-09-15",
  "time_approximate": true,
  "payment_method": "UPI"
}
```

### A.2 Mandate debit

```
HDFC BANK ALERT
UPI Mandate:
Sent Rs.300.00
from HDFC Bank A/c 1234
To SAMPLE AUTOPAY
05/09/26
Ref 100000000002
Not You? Call 18002586161/SMS BLOCK UPI to 7308080808
```

```json
{
  "message_type": "mandate_debit",
  "creates_record": true,
  "direction": "debit",
  "default_classification": "expense",
  "amount": 300.00,
  "currency": "INR",
  "account_suffix": "1234",
  "counterparty_name": "SAMPLE AUTOPAY",
  "counterparty_upi_id": null,
  "reference": "100000000002",
  "date": "2026-09-05",
  "time_approximate": true,
  "payment_method": "mandate"
}
```

### A.3 Credit

```
Credit Alert!
Rs.12.50 credited to HDFC Bank A/c XX1234 on 12-09-26 from VPA merchant1@examplebank (UPI 100000000003)
```

```json
{
  "message_type": "credit",
  "creates_record": true,
  "direction": "credit",
  "default_classification": "unclassified",
  "amount": 12.50,
  "currency": "INR",
  "account_suffix": "1234",
  "counterparty_name": null,
  "counterparty_upi_id": "merchant1@examplebank",
  "reference": "100000000003",
  "date": "2026-09-12",
  "time_approximate": true,
  "payment_method": "UPI"
}
```

The message does not say whether this is cashback, salary, or a refund. The income type stays empty until the user assigns it.

### A.4 Advance notice (creates no record)

```
E-Mandate!
Rs.300.00 will be deducted on 05/09/26, 00:00:00
For SAMPLE AUTOPAY mandate
UMN 0123456789abcdef0123456789abcdef@okhdfcbank
Maintain Balance
-HDFC Bank
```

```json
{
  "message_type": "non_transaction_notice",
  "creates_record": false,
  "raw_text_retained": false
}
```

### A.5 Negative and duplicate cases

| Case | Expected result |
|---|---|
| One-time password message from the bank sender | `non_financial`: discarded, nothing stored |
| Promotional or offer message from the bank sender | `non_financial`: discarded, nothing stored |
| Plausible financial message in an unknown format from the bank sender | `unknown_financial`: raw text kept in the review queue |
| The same SMS delivered by the incoming receiver and by catch-up | One record |
| Two debits with the same amount, payee, and day but different references | Two records |
| A message with the same format from a sender not on the allowlist | Ignored, nothing stored |
| A manual entry matching an SMS record by amount and date | Warning offered, no automatic merge |

## Appendix B: Template Schema

### B.1 Example template

```json
{
  "schema_version": 1,
  "template_id": "hdfc-upi-debit",
  "version": 1,
  "bank": "HDFC Bank",
  "sender_ids": ["<verified sender ID from the device>"],
  "message_type": "debit",
  "direction": "debit",
  "payment_method": "UPI",
  "pattern": "Sent Rs\\.?\\s*(?<amount>[\\d,]+(?:\\.\\d{1,2})?)\\s+From HDFC Bank A/C \\*?(?<account>\\d{4})\\s+To (?<payee>.+?)\\s+On (?<date>\\d{2}/\\d{2}/\\d{2})\\s+Ref (?<reference>\\d{12})",
  "flags": ["case_insensitive"],
  "date_format": "dd/MM/yy",
  "examples": [
    {
      "sms": "Sent Rs.450.00\nFrom HDFC Bank A/C *1234\nTo JOHN D\nOn 15/09/26\nRef 100000000001\nNot You?\nCall 18002586161/SMS BLOCK UPI to 7308080808",
      "expected": {
        "amount": 450.00,
        "account": "1234",
        "payee": "JOHN D",
        "date": "2026-09-15",
        "reference": "100000000001"
      }
    }
  ],
  "negative_examples": [
    "123456 is your one-time password. Do not share it with anyone."
  ]
}
```

### B.2 Fields

| Field | Required | Notes |
|---|---|---|
| `template_id` | Yes | Unique and stable. |
| `version` | Yes | Integer. Recorded on every record the template produces. |
| `bank`, `sender_ids` | Yes | An explicit, reviewed allowlist of sender IDs. A template alone never grants permission to retain messages. |
| `message_type` | Yes | `debit`, `credit`, `mandate_debit`, or `non_transaction_notice`. |
| `direction` | Yes, except notices | `debit` or `credit`. |
| `pattern` | Yes | Java/Kotlin-compatible regular expression with named groups. |
| `date_format` | Yes | For example `dd/MM/yy` or `dd-MM-yy`. |
| `examples` | Yes, at least one | Original SMS and expected values. Used as tests. |
| `negative_examples` | Yes, at least one | Messages that must not match. Used as tests. |

Allowed named groups: `amount` (required), `account`, `payee`, `upiId`, `reference`, `date`, `time`, `balance`. Groups for fields not present in the message are omitted, never guessed.
