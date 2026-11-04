# Mindfuck v1.1.0

Bring your eMoods history along.

## Changes

- **Import from eMoods.** *Settings → Import from eMoods (.emoods)* reads the `export.emoods` file that eMoods' export creates and adds it to your diary:
  - days you have not logged in Mindfuck are added with mood scales, sleep, psychotic symptoms, therapy and notes;
  - **nothing is deleted or overwritten:** a day you already logged keeps its values and only gets the fields that are still empty; notes are appended line by line if they are not there yet;
  - medications are matched by name, unknown ones are created with their eMoods dose, and intakes are added with the dose taken that day;
  - importing the same file again changes nothing;
  - before anything is touched, a full backup of the current diary is saved to `Downloads/Mindfuck/mindfuck_backup_before_emoods_<date>.json`, restorable with *Restore from backup*.
- Built with AGP 9.5, Kotlin 2.4, compileSdk 37 and Compose BOM 2026.09.

## Downloads

- `Mindfuck-1.1.0-android.apk` - the app (Android 14 or newer).
- `SHA256SUMS.txt` - checksum.

Android asks you to allow installing from your browser or file manager. To verify the download in PowerShell, run `Get-FileHash .\Mindfuck-1.1.0-android.apk -Algorithm SHA256` and compare the result with `SHA256SUMS.txt`.

## Good to know

- This APK is signed with the project's release key. If Android refuses to install it over your current Mindfuck ("conflicts with an existing package"), your copy was signed with an older test key: **export a backup first** (Settings → Export all data), then uninstall, install this version and restore the backup. Updates after that install normally.
- Reports and backups are ordinary unencrypted files: share them only with people you mean to.
