# Dream's bKash Verifier v1.1

Reads bKash "money received" SMS and saves each TrxID to Firestore (`automated_bkash_trx/{TrxID}`)
so your website verifies student payments automatically.

## What is new in v1.1
- Broader SMS reading (English + Bangla, payment / send-money formats, sender name contains "bKash").
- Safety net: the app also scans the SMS inbox when opened and every ~15 minutes, so a payment is
  saved even if the phone blocked the live SMS broadcast.
- Diagnostics screen: permission status, "last SMS this app received", and a box where you can paste a
  real bKash SMS to see if it is recognised.
- Fixed signing key: later updates install over this version without uninstalling.

## First install of v1.1
Uninstall the old version once (its signing key was different), then install the new APK.

## Setup
1. Open the app, enter the device password (Firebase Auth user), tap Save settings.
2. Tap "1) Allow SMS permissions" and allow BOTH Receive SMS and Read SMS.
3. Tap "2) Disable battery optimization" -> Unrestricted / No restrictions.
4. Xiaomi / Redmi / POCO: tap "3) Open app settings" ->
   - Menu (three dots) -> "Allow restricted settings" (if shown)
   - Autostart ON
   - Permissions -> SMS -> allow; also allow "Notification SMS" / "Read SMS" if listed
   - Battery saver -> No restrictions
5. Tap "Send test transaction" -> log must show "Saved: TEST...".
6. Paste a real bKash SMS in the check box -> must say "OK - TrxID ..., Tk ...".
7. Ask a friend to send you Tk 10. The log must show "Received <TrxID>" then "Saved: <TrxID>".
   If it does not, tap "Scan SMS inbox now".

## Build
Upload this whole folder to GitHub (keep .github and app/debug.keystore) -> Actions -> Build APK.
