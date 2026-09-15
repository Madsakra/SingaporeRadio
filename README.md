# Singapore Radio

A simple Android radio player built for older listeners and everyday family use. It offers **26 Singapore radio stations and digital channels**, with large controls and English/Chinese labels. **YES 933**, **LOVE 972**, and **CAPITAL 958** remain first for existing listeners. See the [station catalog and source checks](docs/STATIONS.md) for coverage and availability.

The app adds no ads, analytics, subscriptions, or sign-in. **Live radio needs an internet connection**; this is not an offline radio receiver. Advertisements within a station's broadcast may still be heard.

<img src="docs/screenshot.png" alt="Singapore Radio: accessible station list, language filter, large green PLAY buttons and a red STOP button" width="340">

## Features

- A shared catalog covering Mediacorp, SPH Media, BBC World Service and Kakee.
- Language filters for Mandarin, English, Malay, Tamil, Cantonese and Korean; bilingual stations appear in both relevant filters.
- Full station names, FM/online labels and language information. The original three logos remain; other stations use a neutral radio icon.
- Light grey background, large green PLAY buttons, and a large red STOP button.
- English and Chinese controls, wrapping station names and stacked controls on narrow screens or at large font sizes.
- A scrolling station list with playback status and STOP kept outside the scroll area.
- Filter selection survives screen rotation; filtering does not interrupt playback.
- Playback with the screen off, audio focus handling, and stopping when headphones disconnect.
- Clear connection, offline, and playback status messages.
- Direct broadcaster streams; no project-owned audio server.

## Build

Requires **JDK 17 or 21**, **Android SDK Platform 36**, and Android SDK Build Tools. The included Gradle wrapper uses Gradle 8.11.1 and Android Gradle Plugin 8.10.1.

1. Clone this repository and open it in Android Studio, or install the requirements for a command-line build.
2. Set `JAVA_HOME` to JDK 17 or 21 and `ANDROID_HOME` to your Android SDK directory. Alternatively, configure `sdk.dir` in an untracked `local.properties` file.
3. Build a debug APK:

```powershell
# Windows
.\gradlew.bat :app:assembleDebug
```

```sh
# macOS / Linux
./gradlew :app:assembleDebug
```

The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`. It uses a development signing key and cannot update a differently signed installation.

For a release build and Android lint checks:

```sh
./gradlew :app:assembleRelease :app:lintRelease
```

On Windows, use `.\gradlew.bat` instead of `./gradlew`. The release APK in `app/build/outputs/apk/release/` is **unsigned**. Sign it with your own private key before installing or distributing it. An update must use the same application ID and signing key as the existing installation. Signing keys and passwords are deliberately excluded from this repository.

For an Android App Bundle (AAB) and automated checks:

```sh
# JDK 21 is required for Robolectric tests against Android 16.
./gradlew :app:testDebugUnitTest :app:bundleRelease :app:lintRelease
```

The release bundle is written to `app/build/outputs/bundle/release/app-release.aab`.
It is **unsigned** until you configure release signing. Automated tests cover the
catalog, every station's PLAY intent, filtering, recreation, and small-screen
layouts on Android 7 and Android 16. Native-rendered UI previews are generated in
`app/build/reports/ui/` for visual review. `docs/screenshot.png` is a preview of
the Android views, not a physical-device screenshot.

Google Play preparation and remaining release work are documented in
[GOOGLE_PLAY.md](docs/GOOGLE_PLAY.md). This repository does not publish an app or
create signing keys automatically.

## Project details

- Current version: **1.3** (version code 4)
- Android 7.0 (API 24) or newer; targets Android 16 (API 36)
- Package: `com.oai.singaporeradio`
- Native Java Android UI
- AndroidX Media3 ExoPlayer 1.8.0 and AndroidX Core 1.15.0
- `MainActivity.java`: station list, large controls, and playback status
- `RadioService.java`: streaming, background playback, audio focus, and connection handling
- `StationData.java`: immutable station catalog, language filters and broadcaster stream endpoints

Version 1.3 expands the station catalog and adapts the UI to longer lists and
names while retaining the existing playback service. Live stream checks received
audio from 22 feeds; four Kakee feeds returned HTTP 403 from the verification
network. All Kakee streams are labelled Singapore-only, consistent with the
broadcaster's availability policy. See [STATIONS.md](docs/STATIONS.md) for the
exact channels and limitations.

Stream addresses and availability are controlled by the broadcasters. If an endpoint changes, update `StationData.java` and rebuild.

## Station attribution

This is an independent community project and is not an official Mediacorp, meLISTEN, SPH Media, BBC or Kakee app. Station names, logos, and broadcasts belong to their respective owners. See [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md) for asset sources and dependency information. Publishing this source does not grant rights to third-party branding or broadcasts; permission for broader distribution has not been verified.
