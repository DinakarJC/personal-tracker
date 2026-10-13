# Testing on the realme P3 Ultra

Target confirmed by user: Android 15. USB is unavailable on the office laptop. No Android Studio is required.

The user selected manual APK installation. Wireless pairing is an optional future route, not a prerequisite for the current build.

## Parser test after Play Protect blocked the SMS build

The user reported the sensitive-data/fraud Play Protect block while installing the original SMS-permission APK. Use the separate `expense-tracker-parser-test.apk` build on the Android 15 P3 Ultra instead. Its application name is **Personal Tracker Test** and its application ID is `local.tracker.app.manual`.

1. Download the parser-test APK from its GitHub release and install it manually.
2. Open Personal Tracker Test. It should request no SMS permissions.
3. Tap Use sample debit, then Parse message. Check amount ₹450.00, payee JOHN D, date 2026-09-15, account suffix 1234, and reference 100000000001.
4. Tap Clear message. The input and parsed result should disappear.
5. Optionally paste a redacted HDFC transaction message and its sender label. Check the extracted values on the phone; do not send personal message text to GitHub or chat for this test.

This tests the shared parser and phone UI only. It does not read the inbox, save ledger transactions, or validate incoming SMS delivery. Removing the sensitive permissions addresses the reported trigger, but installation acceptance must still be confirmed on the phone. Google's explanation of the warning: https://developers.google.com/android/play-protect/warning-dev-guidance

Device result on 2026-10-13: the user confirmed the permission-free APK installed and the sample parsed. Other manual checks and automatic SMS capture are still pending.

## Manual installation of the original SMS probe (blocked on the target phone)

1. Transfer `dist/expense-tracker-capture-check.apk` to the P3 Ultra using your chosen permitted file-transfer channel. The original Gradle output is `app/build/outputs/apk/debug/app-debug.apk`.
2. Open the APK in the phone's file manager. Allow installation from that app if Android prompts, then install Personal Expense Tracker.
3. Open the application and read the capture-setup explanation. Tap Allow SMS access and grant read/receive SMS if available.
4. If permission is blocked, open app permissions and inspect any restricted-settings option. Record the actual behavior; this option does not universally resolve installer restrictions.
5. Tap Preview last seven days. Report the inbox/incoming permission states and recognized/review message counts. This probe does not save ledger transactions or raw bodies, and its counts are message counts rather than deduplicated transaction totals.
6. After a naturally occurring eligible HDFC SMS arrives, reopen the app and refresh capture status. Report whether a debit/credit/mandate was recognized; do not paste personal financial details.

The initial APK is a debug-signed capture-feasibility probe. It requires Android 15+. Keep its signing identity for updates; do not uninstall/reinstall the eventual ledger app without a backup. Device tests and permission reliability remain unverified until these steps are actually completed.

## Wireless connection

1. Enable Developer options from Settings → About device → Version → Build number (labels may vary in realme UI). Tap the build number repeatedly and authenticate if prompted.
2. Open Developer options → Wireless debugging. The user confirmed this phone requires a Wi-Fi connection and cannot use the original hotspot-hosting arrangement.
3. Use the second phone as the hotspot and connect both the laptop and realme phone to it. Both devices must be allowed to communicate on that network.
4. Choose Pair device with pairing code on the realme phone. Use the address/port shown there with `adb pair`. Enter the current temporary code when prompted.
5. If pairing does not automatically connect, use the address/port on the main Wireless debugging screen with `adb connect`. The connection port is often different from the pairing port.
6. Confirm `adb devices` shows the intended device. Turn off wireless debugging when testing is finished.

Pairing is not a connection until the device appears online. Network isolation or office restrictions may prevent it. Do not use the legacy `adb tcpip 5555` method: it would require USB setup here.

Official instructions: https://developer.android.com/tools/adb

## Initial acceptance checklist

These are planned device checks; none has been completed yet.

- [ ] Install the test APK by wireless ADB or an agreed permitted file-transfer route.
- [ ] Validate the installer allows SMS permissions; record required steps without assuming restricted-settings guidance is sufficient.
- [ ] Grant SMS access; deny/revoke it and confirm a clear paused state.
- [ ] Confirm actual HDFC sender format normalizes to the reviewed `HDFCBN-S` label. Never export unrelated personal messages for diagnostics.
- [ ] Capture an available real HDFC transaction message end to end and compare extracted fields against the original on the phone.
- [ ] Confirm OTPs, offers, advance notices, and non-allowlisted senders leave no retained body or transaction.
- [ ] Repeated scans of the same range yield one record per real transaction.
- [ ] Test app foreground/background, locked screen, battery idle, reboot, and force-stop/reopen. Record catch-up behavior separately from real-time delivery.
- [ ] Verify manual entry remains useful when SMS access is denied.
- [ ] Record bank-message field accuracy separately from the availability of SMS alerts.

Later checks: classification/total reconciliation, notification actions/privacy, app lock, backup/restore, accessibility at 200% text and TalkBack, and dashboard latency with 10,000 records. The two-week daily-use trial cannot be substituted by automated tests.
