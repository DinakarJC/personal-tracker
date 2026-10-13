# Architecture

The Android application is being built from the PRD v3.1. The first boundaries are the Kotlin JVM `core` module and a Compose Android `app` capture-feasibility probe. This initial Android build does not implement the transaction ledger or claim milestone completion.

`core` contains money/date validation, sender normalization, typed parser outcomes and provenance, and runtime-loaded built-in JSON templates. It has no Android dependencies, persistence, networking, or raw-text logging. The sender allowlist is supplied independently from templates: a template does not authorize a sender.

`core/src/main/resources/templates/hdfc-v1.json` supplies the four Appendix A formats. Examples are initial fixtures, not evidence of complete real-device format coverage. The parser returns accepted fields, a review reason, or a discard/notice/ignored outcome. The Android intake layer will own minimum raw financial retention and persistent source identities. It must never retain a body for discard, ignored-sender, or notice outcomes.

The planned Android layer uses Compose/Material 3, Room migrations, and a shared capture/scan pipeline. Database uniqueness and atomic scan progress will provide idempotency; the pure parser alone does not prevent duplicate storage. See `IMPLEMENTATION_PLAN.md` for boundaries, milestones, and release gates.

The initial app supports Android 15+ to validate the user's Android 15 device; earlier-version support has not been promised or tested. It requests read/receive SMS access, previews recognized/review message counts over the last seven days on explicit action, and records only last eligible incoming-capture status/time. No raw message body, amount, account, or payee is persisted by this probe. The receiver requires the system BROADCAST_SMS permission. No INTERNET permission is declared, Android backup is disabled, and app previews/screenshots are protected. Full app lock, ledger persistence, scans with preview/apply, notifications, and all later features remain pending.

Built-in regular expressions are trusted, bounded application assets. Custom template import and arbitrary untrusted regex execution are outside the MVP. Each asset includes schema/version metadata, positive expected values, and negative examples. Missing facts remain empty and system defaults have explicit provenance.
