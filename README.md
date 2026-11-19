# Swara Tarang

An Android media player for local MP3 and AAC files and for online radio, on the phone and in Android Auto. The free app has no ads, no account and no tracking.

> [!NOTE]
> The app is being built. Today it has the four tabs and the Settings screen, each with a placeholder. The list below is the plan, and [docs/FEATURES.md](docs/FEATURES.md) says what is done.

## Features

| Area | What the app will do |
|---|---|
| Local music | Plays `.mp3`, `.aac` and `.m4a` files from the phone. Browse by song, album, artist and folder. Gapless playback |
| Online radio | Add a station by name and URL, or search the public Radio Browser list. Icecast, Shoutcast and HLS streams. The song title is shown when the station sends it |
| Android Auto | The same library, radio, favourites and recent lists in the car, with voice play |
| Personal lists | Favourites, playlists, history and a sleep timer |
| Sound | A 5 band equalizer |
| Privacy | No ads, no account and no usage analytics. The app built from this repo makes only two kinds of outside call: to Radio Browser and to the stream you picked. The Play Store build also sends crash reports |

## Build

You need JDK 21 and the Android SDK with platform 37. Android 12 (API 31) is the lowest version the app runs on.

1. Clone the repo.
2. Tell Gradle where the SDK is: set `ANDROID_HOME`, or write `sdk.dir=/path/to/Android/Sdk` in a file named `local.properties` at the root of the repo.
3. Build the debug app:

```bash
./gradlew assembleCoreDebug
```

The APK is written to `app/build/outputs/apk/core/debug/`. No key, secret or `google-services.json` is needed.

| Task | Command |
|---|---|
| All unit tests, the screenshot tests and the build checks | `./gradlew test` |
| Save new pictures for the screenshot tests | `./gradlew test -PrecordScreenshots` |
| Android lint | `./gradlew lint` |
| Style and code rules | `./gradlew ktlintCheck detekt` |
| Licences of the shipped libraries | `./gradlew :app:licenseeCoreRelease` |
| Licence header check alone | `./gradlew checkLicenceHeaders` |
| Install on a connected device | `./gradlew :app:installCoreDebug` |

## The two builds

The app is open core. This repo holds the whole free app. The paid features live in a private module that is not in this repo.

| Build | Made from | What it is |
|---|---|---|
| `core` | This repo alone | The complete free app. No ads, no account, no closed source library, no `google-services.json` |
| `play` | This repo plus the private `premium` module | The Play Store build, with the paid features |

- The `play` build exists only when the private module is checked out in a folder named `premium/` at the root of this repo. Without it, only `core` is built, and Gradle says so in one line when it sets up the build.
- Public code never imports premium code. It defines interfaces, such as `PremiumGate`, in `core:domain`. The `core` build binds them to versions that answer "not available". The premium module binds the real ones.
- A build check fails when a source, XML or ProGuard file in this repo names the premium package, or when anything but the `play` build depends on the premium module.

## Docs

- [docs/FEATURES.md](docs/FEATURES.md): what the app does and the decisions made.
- [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md): layers, modules and the folder structure.
- [docs/DATABASE.md](docs/DATABASE.md): the tables on the phone, with the ER diagram.
- [RULES.md](RULES.md): the rules for every change, for people and for coding agents.

## Contributing

Bug reports, ideas and pull requests are welcome. Read [CONTRIBUTING.md](CONTRIBUTING.md) first. To report a security problem, follow [SECURITY.md](SECURITY.md) and do not open a public issue.

## Licence

The code in this repo is under the [Mozilla Public License 2.0](LICENSE). Every source file starts with the MPL-2.0 header.

The licence covers the code only. The name "Swara Tarang" and the app logo are not licensed with it. If you publish a changed version of the app, give it another name and another logo, so that users can tell it apart from this one.
