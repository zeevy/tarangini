# Swara Tarang

An Android media player for local MP3 and AAC files and for online radio, on the phone and in Android Auto. The app is being built; today it shows an empty screen.

## Build

You need JDK 21 and the Android SDK (set `sdk.dir` in `local.properties`, or `ANDROID_HOME`).

```bash
./gradlew assembleCoreDebug
```

The APK is written to `app/build/outputs/apk/core/debug/`.

| Task | Command |
|---|---|
| All unit tests | `./gradlew test` |
| Android lint | `./gradlew lint` |
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
- [RULES.md](RULES.md): the rules for every change, for people and for coding agents.
