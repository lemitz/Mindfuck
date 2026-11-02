# Security and privacy

## What Mindfuck promises, and what it does not

Mindfuck keeps a mood and symptom diary, which is sensitive health data. It is built so that this data **never leaves the phone unless you export it yourself**:

- The app has **no internet permission** (check the APK: `aapt2 dump permissions`). It cannot send anything anywhere, and there is no account, cloud sync, analytics or crash reporting.
- Android cloud backup is **disabled** (`android:allowBackup="false"`), so the diary is not copied to your Google account either.
- The diary is stored in the app's private database. Other apps cannot read it on a normal, unrooted phone.

What Mindfuck does **not** do:

- The database is **not encrypted** beyond what Android's file-based encryption of your phone already provides. Anyone who can unlock your phone and open the app can read the diary; there is no PIN or biometric lock (by design, for now).
- **PDF reports** are ordinary files. A copy is saved to `Downloads/Mindfuck`, where any app with access to your downloads, and anyone you share it with, can read it.
- **JSON backups** are plain text with everything in the diary. Keep them somewhere private.
- Uninstalling the app deletes the diary. Export a backup before reinstalling or installing an APK signed with a different key.

Mindfuck is not a medical device and gives no medical advice.

## Reporting a problem

If you find a way for another app or a remote party to read the diary, for the app to send data off the device, or for a crafted backup file to do something other than restore a diary, please report it **privately** through [GitHub security advisories](https://github.com/lemitz/Mindfuck/security/advisories/new) rather than a public issue. Do not attach real diary data, reports or backups to any report.
