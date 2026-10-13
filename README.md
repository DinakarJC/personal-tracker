# Personal Expense Tracker

Native Android, local-first expense tracker based on PRD v3.1. Development is in progress. Two test builds are available: `manual` parses user-pasted messages without SMS access; `sms` checks permissions, previews seven-day HDFC message counts, and records the last eligible incoming-message status. Neither test build persists a transaction ledger yet.

The `core` module contains independently tested HDFC parsing from runtime JSON templates. The reviewed sender label is `HDFCBN-S`, optionally preceded by a two-letter carrier prefix. Other senders are ignored. Cash entry, database dedup/cursors, dashboard/reports, app lock, and backup/restore remain on the implementation checklist.

## Download on your phone

**[Download the parser-test APK](https://github.com/DinakarJC/personal-tracker/releases/download/v0.2.0-parser-test/expense-tracker-parser-test.apk)** on your Android 15 phone. This build requests no SMS access and exists because Play Protect blocked the original SMS-permission build. Paste a redacted HDFC message or select Use sample debit, then Parse message. Its [release page](https://github.com/DinakarJC/personal-tracker/releases/tag/v0.2.0-parser-test) includes validation notes and a checksum. Follow `DEVICE_TESTING.md` for phone checks.

GitHub Actions builds both variants on pushes to `main`, pull requests, or manual workflow runs. Successful **Android build** runs upload separate **expense-tracker-parser-test-apk** and **expense-tracker-sms-test-apk** artifacts with checksums. Tests, formatting checks, and Android lint must pass, and the built manual manifest is checked for unintended SMS/network access. Reports are uploaded separately even when a check fails. Device instrumentation tests are compiled, not executed on a phone by this workflow.

CI debug APKs use a temporary runner's signing key and may not install over a build signed elsewhere. Use the published release for the initial phone test. Stable signing for future ledger-app updates will be configured before daily-use distribution; signing keys are never committed to the repository.

## Build without Android Studio

This workstation has portable JDK 17, Gradle 8.13, Android command-line tools, Android 35 platform, and build tools 35.0.0 under ignored `.tools/`. `local.properties` points to the SDK. The PowerShell runner chooses that JDK/cache and avoids changing system Java.

```powershell
.\scripts\Run-Gradle.ps1 :core:test :core:ktlintCheck :app:ktlintCheck :app:lintManualDebug :app:lintSmsDebug :app:assembleManualDebug :app:assembleSmsDebug :app:assembleManualDebugAndroidTest :app:assembleSmsDebugAndroidTest
```

Run formatting as a separate invocation before checks, so parallel Gradle tasks do not read files while the formatter edits them:

```powershell
.\scripts\Run-Gradle.ps1 :core:ktlintFormat :app:ktlintFormat
```

The Gradle wrapper is checked in with the distribution SHA-256 pinned. A fresh machine needs a compatible JDK 17 and Android SDK installed from official sources, an SDK location in `local.properties`, and the required SDK license acceptance. The portable `.tools/` downloads are not source files or repository artifacts. `scripts/Install-AndroidPackages.ps1` installs packages after verified toolchain extraction; use `-AcceptLicense` only with authorization to accept Google's SDK license.

## Phone testing

The initial APK targets Android 15+. The user chose manual installation on a realme P3 Ultra. See `DEVICE_TESTING.md` for transfer/install and acceptance steps. Instrumentation tests require a connected target; compiling their APK is not equivalent to executing them. Do not claim device capture reliability until tested on the phone.

`IMPLEMENTATION_PLAN.md` maps PRD features, tests, and release gates. `ARCHITECTURE.md` describes boundaries. `IMPLEMENTATION_NOTES.md` records evidence and outstanding work. Engineering standards in `C:/Users/dinakar.jc/Desktop/ENGINEERING` remain authoritative.
