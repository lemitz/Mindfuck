# Third-party notices

Mindfuck's own code is covered by the [MIT License](LICENSE). The APK also contains the following.

## Libraries (Apache License 2.0)

Bundled through Gradle from Google's Maven repository and Maven Central:

- **AndroidX:** Core, Activity, Lifecycle, Compose (UI, Foundation, Animation, Material 3, Material Icons Extended), Room, ProfileInstaller - The Android Open Source Project
- **Kotlin standard library** and **kotlinx.coroutines** - JetBrains

Full licence text: <https://www.apache.org/licenses/LICENSE-2.0>.

## Typeface

- **Realme Real Choice** (`app/src/main/res/font/realchoice.ttf`, internally *OPPOSans 2.0*, variable weight). Used for the interface and embedded in the generated PDF reports. Copyright OPPO / realme. The file was taken from [Cykeek-Labs/android_external_google-fonts_realchoice](https://github.com/Cykeek-Labs/android_external_google-fonts_realchoice), where it is packaged for Android ROMs. It is not covered by this repository's MIT licence; if you are a rights holder and want it removed, open an issue.

## Artwork

- The brain in the launcher icon (`app/src/main/res/drawable/ic_launcher_fg.xml`, `assets/icon-circle.png`) is the **"neurology" glyph from [Material Symbols](https://github.com/google/material-design-icons)** by Google, Apache License 2.0. The dark disc and the composition are part of this repository.

## Build tools (not shipped)

JDK, Gradle, Android Gradle Plugin, Kotlin Gradle plugin, Kotlin Symbol Processing and the Room compiler (Apache-2.0).
