# Implementation notes

## 2026-10-13 — Command-line foundation and HDFC parser

- Confirmed target: realme P3 Ultra on Android 15; no USB. User provided `<XX>-HDFCBN-S` as the reviewed bank sender format. Wireless testing may require a second phone to host the hotspot.
- User approved canonical unclassified naming, system-derived provenance, refund/reversal schema support, and calculation-basis reconciliation for scalar statistics.
- Added a JVM Kotlin `core` module, four runtime JSON templates, and requirement-derived parser tests based on Appendix A and negative/error cases.
- Money is integer paise; date validation is strict. Credits remain unclassified. Advance notices produce no transaction. Template sender lists do not expand the separately reviewed allowlist.
- Portable JDK 17 and Gradle 8.13 passed SHA-256 verification. Android command-line tools passed the official SDK repository's size/SHA-1 check. Toolchain/cache files stay under ignored `.tools/`; no system Java installation or USB policy was changed. User explicitly authorized Android SDK license acceptance.
- Core compilation, ktlint formatting/checks, and all 16 parser tests passed. Gradle on this Windows machine failed moving a generated transform workspace while running; after its daemon exited, moving completed generated cache directories to their intended immutable cache paths allowed the build to continue. No user files or security settings were changed.
- Added a Compose Android capture-check probe and four Android instrumentation tests. This probe records only last financial-message capture status/time and previews seven-day recognized/review counts. It intentionally does not implement transaction persistence yet. Android 15 is the initial minimum validated target; support for earlier versions is not claimed.
- User chose manual APK installation. Android 35/platform-tools/build-tools packages installed successfully. Debug application and Android instrumentation APKs compiled. Both modules passed ktlint; core tests remain 16/16 passing. Android lint reports zero errors and eight SDK/dependency upgrade advisories; versions are pinned for the initial Android 15 feasibility target, with upgrades to be reviewed before the eventual daily-use release.
- APK signature verification passed (v2, debug certificate). Package metadata confirms Android 15 minimum/target, read/receive SMS permissions, and no INTERNET permission. ZIP inspection confirms the HDFC template resource is packaged. The installable handoff is `dist/expense-tracker-capture-check.apk`.
- Instrumentation tests were compiled but not executed. No connected phone was used. Permission/capture reliability, representative real-message accuracy, database concurrency, and every later milestone remain pending. The capture-check APK is a device feasibility step, not an MVP completion claim.

## 2026-10-13 — GitHub distribution and CI

- User requested source publication to `https://github.com/DinakarJC/personal-tracker.git`, a phone-downloadable APK, and GitHub Actions builds.
- Added an Android workflow for main pushes, pull requests, and manual runs. It sets up JDK 17/Android 35, runs core tests and both modules' formatting checks, runs Android lint, builds application/instrumentation APKs, and uploads the application APK/checksum plus check reports. Official actions are pinned to immutable commits and the job has read-only repository permission.
- Added line-ending rules and executable-wrapper staging for Linux CI. Local SDK/JDK/cache/build/signing files and distribution binaries stay out of source control. The current locally verified binary is intended for a GitHub release asset, rather than a binary in Git history.
- CI debug signing remains ephemeral and is explicitly documented. Stable release signing is required before distributing updates to a ledger containing user data.
- GitHub push, release publication, and hosted workflow results are pending until recorded below. No application behavior changed in this step.
