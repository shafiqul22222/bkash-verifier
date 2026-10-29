# Dream's bKash Verifier (MacroDroid replacement)

Reads bKash "money received" SMS on your phone and saves each TrxID to Firestore
(`automated_bkash_trx/{TrxID}`) so your website verifies student payments automatically.

## Setup
1. Firebase Console > Authentication > Users > Add user:
   email `bkash-device@dreamsacademy.app`, any strong password (Email/Password sign-in must be enabled).
2. Build the APK (no Android Studio needed):
   - Create a free GitHub repository, upload this whole folder (keep the `.github` folder).
   - Open the repository's Actions tab > "Build APK" > Run workflow.
   - When it finishes, download the `bkash-verifier-apk` artifact and unzip it.
   - Or open the folder in Android Studio and press Build > Build APK.
3. Install the APK on the phone that receives your bKash SMS (allow "install unknown apps").
4. Open the app: enter the device password, Save, tap "Allow SMS permission",
   tap "Disable battery optimization" and allow it. On Xiaomi/Oppo/Vivo/Realme also enable Autostart.
5. Tap "Send test transaction". The log must show "Saved: TEST...".
6. Publish the new Firestore rule from `bkash_rules_replace.txt`, then turn MacroDroid off.

Note: sideload only. Google Play does not allow apps that read SMS for this purpose.
