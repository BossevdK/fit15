# Fit15

Free Android app with 15-minute bodyweight workouts for building muscle at home. No equipment, no account, no ads.

## Features

- 8 workouts of 15 minutes and 2 quick workouts of 7 minutes
- 4-week program (3 sessions a week, a bit harder every week)
- Easy / Medium / Hard intensity, with easier and harder exercise variations
- Animated exercise illustrations (drawn in code, no copyright issues)
- Optional 1-minute warm-up
- Body map that colours the muscle groups you trained this week
- Streak and weekly overview
- Daily reminder
- Updates through GitHub releases (the app checks for a newer release when it opens)

## Building

The Android tools live in `C:\Users\Gain\android-tools` (see the global setup). Create a `local.properties` with
`sdk.dir=C\:\\Users\\Gain\\android-tools\\sdk`, then:

```
gradle assembleRelease
```

A release is signed with the key in `sleutel/` (`fit15-release.jks` + `keystore.properties`, copied to the project root).
These files are never committed. Every release must be signed with the same key, otherwise phones refuse the update.

## Releasing an update

1. Raise `versionCode` and `versionName` in `app/build.gradle.kts`.
2. Build with `gradle assembleRelease` and copy `app/build/outputs/apk/release/app-release.apk` to `Fit15.apk`.
3. Create a GitHub release with tag `vX.Y` and attach `Fit15.apk`.

## Exercise previews

`node tools/preview.js` reads `Data.kt` and writes `tools/preview.html`, a page that shows both poses of every exercise.
