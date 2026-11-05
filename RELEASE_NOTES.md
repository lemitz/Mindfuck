# Mindfuck v1.2.0

Reports in English.

## Changes

- **Report language.** The dialog shown before every report now starts with *Report language: Русский / English*. The English report has the same layout as the Russian one: value scale, day-by-day table, summary by period, sleep, medications, charts and self-observation notes, with English dates ("October 18–24, 2026", "Oct 20–21: severe.").
- The choice is remembered together with the selected sections, so the next report uses the same language. Russian stays the default.
- Your notes and the names of your custom tracking points and medications go into the report exactly as you typed them, in either language.

## Downloads

- `Mindfuck-1.2.0-android.apk` - the app (Android 14 or newer).
- `SHA256SUMS.txt` - checksum.

Android asks you to allow installing from your browser or file manager. To verify the download in PowerShell, run `Get-FileHash .\Mindfuck-1.2.0-android.apk -Algorithm SHA256` and compare the result with `SHA256SUMS.txt`.

## Good to know

- This APK is signed with the project's release key. If Android refuses to install it over your current Mindfuck ("conflicts with an existing package"), your copy was signed with an older test key: **export a backup first** (Settings → Export all data), then uninstall, install this version and restore the backup. Updates after that install normally.
- Reports and backups are ordinary unencrypted files: share them only with people you mean to.
