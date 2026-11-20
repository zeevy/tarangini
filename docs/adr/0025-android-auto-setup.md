# ADR 0025: Android Auto setup and who may browse

- Status: accepted, 20 Nov 2026.
- Rules: H1, P1, P2, P8, S2, S7.

## Context

Playing in the car is the main use of this app. Android Auto shows a media app when the app declares it, and then talks to the one media session the phone screens use ([ADR 0004](0004-playback-service-and-session-access.md)). Until now Android Auto was only let in as a caller the system trusts, with the transport controls. To show the library it must be allowed to browse and to start what the driver picks.

## Decisions

| Decision | Options looked at | Reason |
|---|---|---|
| The app declares itself for Android Auto with `automotive_app_desc.xml` (`media` only) and the `com.google.android.gms.car.application` entry, in the manifest of the playback module | The app manifest | The entry belongs to the service the car talks to, and that service lives in the playback module |
| A third kind of caller, the car: Android Auto (`com.google.android.projection.gearhead`) and the Google app (`com.google.android.googlequicksearchbox`, for voice). It may browse the library and start playback. Every item it asks for still goes through the same check as before | Give every caller the system trusts the right to browse; one "full" level for the car too | An app with notification access is trusted by the system, and that must not be enough to read the library. The car gets its own level so that a later command only this app should have does not reach the car by accident |
| A caller is the car only when three things hold: its package name is in the list, the system says the calling user id owns that package, and the installed package is signed with a listed certificate (SHA-256) | Package name only; ask the system with `hasSigningCertificate` | A name can be claimed by any app. The user id comes from the system. The certificates are read from the installed package, and the history of a rotated key counts, which the Google app needs. The system call does the same, but the test library cannot stand in for it, and a check without a test is worse than a few lines of our own |
| The certificate values are the ones Google publishes in the caller list of its sample player (`android/uamp`), compared with the two apps on a Pixel 6a: Android Auto 18.0 is signed with the listed `1ca8dc...`, and the Google app on Android 13 and newer is signed with `7ce83c...`, a newer key that is not in the published list, with the listed `f0fd6c...` as its older key | Copy the published list as it is | The published list is from before the Google app changed its key. Without the newer value voice play would be refused on current phones |
| The car is asked before the "trusted by the system" flag | The other order | Android Auto has notification access on most phones. In the other order it would stay at the transport controls |
| A package signed with several keys at once must have every key in the list | One listed key is enough | An extra, unknown signer is a reason to refuse (S7) |
| The manifest names the two packages in `<queries>` | Rely on Android showing a caller to the service it binds to | Android hides other apps by default. A hidden app would be refused with no sign of why |
| Debug certificates of Google's own test builds are not in the list | Add them as the sample does | Nobody outside Google runs those builds. A shorter list is the safer one (S7) |

## Result

- Tarangini shows in the app list of Android Auto. The session answers the car with a top node and an empty list under it; the browse tree itself comes with the next change. Until then the car shows an empty app, and what plays on the phone can be controlled from the car.
- An unknown app, and an app that only uses the package name of Android Auto, are refused as before. Tests cover both.
- When Google signs one of the two apps with a new key, that app is refused until its new value is added to `CarCallerCertificates`. The sign is: the app is missing in the car, or voice play stops working, after an update of Android Auto or the Google app.
- The steps for the Desktop Head Unit are in [ANDROID-AUTO.md](../ANDROID-AUTO.md). Rule P8 asks for a head unit run before a change to the player, the session or the browse tree is merged.
