## What and why

<!-- A sentence or two. Link the issue if there is one. -->

## How I checked it

- [ ] `./gradlew :app:testDebugUnitTest`
- [ ] `./scripts/build.ps1` (builds the release APK)
- [ ] Tried it on a device or emulator (say which): <!-- model, Android version -->
- [ ] If the PDF changed: generated a report and compared it with the base layout (one page for 7 days with notes)

## Release

- [ ] This change should be released: I bumped `VERSION` and rewrote `RELEASE_NOTES.md` (merging publishes `v<VERSION>` automatically)
- [ ] No release yet: `VERSION` is unchanged (merging only builds)

<!-- If the database schema changed: bump the Room version in AppDatabase.kt and add a Migration, never a destructive fallback - it would wipe people's diaries.
     If a chart changed: it is drawn by charts/ChartRenderer.kt for both the screen and the PDF, check both. -->
