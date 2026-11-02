# Mindfuck v1.0.0

First release of Mindfuck, a private mood diary for Android 14+ in the spirit of eMoods, with a doctor-ready PDF report in Russian.

## What's inside

- **Log Moods:** sleep, the day's most extreme depressed mood, elevated mood, irritability and anxiety (None / Mild / Moderate / Severe), psychotic symptoms and talk therapy (yes / no), your own tracking points, medications with the dose taken each day, and self-observation notes. Everything saves instantly.
- **Calendar** coloured by the most severe symptom of each day.
- **Graph** for 7, 30 or 90 days: sleep with its average, eMoods-style low-to-high marks, yes / no points, and medications with dose changes. Every chart has a legend.
- **PDF report "Дневник настроения и симптомов"** for any period. The base report reproduces the paper diary: the 0-3 scale, the day table, the period summary in proper Russian and the last day's notes. Optionally add sleep, psychotic symptoms and therapy, medications with adherence and doses, custom points, charts and the notes of every day. Files are named `mindfuck_DDMMYY.pdf`, then `_v2`, `_v3`… for more reports on the same day; a copy goes to `Downloads/Mindfuck`.
- Swipe between tabs, animated transitions, dark theme by default with a light theme in Settings, the display's fastest refresh rate, and Realme's *Real Choice* typeface throughout.
- JSON backup and restore. No internet permission, no account, no cloud backup, no telemetry.

## Downloads

- `Mindfuck-1.0.0-android.apk` - the app (Android 14 or newer).
- `SHA256SUMS.txt` - checksum.

Android asks you to allow installing from your browser or file manager. To verify the download in PowerShell, run `Get-FileHash .\Mindfuck-1.0.0-android.apk -Algorithm SHA256` and compare the result with `SHA256SUMS.txt`.

## Good to know

- Reports and backups are ordinary unencrypted files: share them only with people you mean to.
- Uninstalling deletes the diary. Export a backup (Settings → Export all data) before reinstalling.
