# Google Play release preparation

Status: development version 1.3 (code 4), prepared for testing; not published.

## Included in this change

- Targets Android 16 / API 36, as required for new mobile apps from 31 August 2026.
- Keeps the existing `com.oai.singaporeradio` application ID and minimum Android 7 support.
- Provides an unsigned release AAB with `./gradlew :app:bundleRelease`.
- No app-owned backend, login, analytics, advertising SDK, purchase or subscription.
  Broadcasters may include advertisements in their audio and collect connection data.

## Before submission

1. Confirm permission to offer the broadcaster streams in a public third-party app
   and to use the three existing logos. Free distribution and a publicly accessible
   stream do not establish permission. Keep the app and listing clearly independent.
2. Test actual audio on a Singapore phone, particularly POWER 98 Mixtape, POWER 98
   EDM Club Hits, 88.3JIA Trending Hits and 88.3JIA K-Pop, which returned HTTP 403 in
   the current network check. Also test station switching, screen-off playback,
   interruptions, headphones, offline errors, TalkBack, rotation and large text.
   JVM tests and HTTP audio probes do not replace device playback testing.
3. Choose and protect the release/upload key and configure Play App Signing.
   Use the existing installation's signing key if an update must replace the
   version used by the family. Do not commit keys or passwords. The test APK's
   development key and the unsigned AAB are not a production signing setup.
4. Publish a privacy policy at a public URL and provide an in-app link. Describe
   the direct connections to broadcasters (including IP address / request data),
   Android backup of local app state and any provider-side processing you verify.
   Complete Data safety using the actual app and provider practices, rather than
   assuming that the absence of an analytics SDK means no data is processed.
5. Complete the Play Console account, app access, content rating, target audience,
   ads and foreground media playback declarations. Set the price to Free and
   select the countries supported by the streams. Supply a support contact,
   store description, icon, feature graphic and current device screenshots.
6. If this is a personal developer account created after 13 November 2023,
   check the closed-testing requirement (currently at least 12 testers opted in
   continuously for 14 days) and apply for production access when eligible.

## Official references (checked 15 September 2026)

- [Target API requirement](https://support.google.com/googleplay/android-developer/answer/11926878)
- [User data and privacy policy](https://support.google.com/googleplay/android-developer/answer/10144311)
- [New personal account testing](https://support.google.com/googleplay/android-developer/answer/14151465)
- [Store listing assets](https://support.google.com/googleplay/android-developer/answer/9866151)
- [Play App Signing](https://support.google.com/googleplay/android-developer/answer/9842756)
- [Kakee availability FAQ](https://www.kakee.sg/)
