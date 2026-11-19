# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project state

Swara Tarang is an Android media player for local MP3 and AAC files (`.mp3`, `.aac`, `.m4a`) and online radio, on the phone and in Android Auto. The build started on 19 Nov 2026. The Gradle project and its modules exist. The app has its shell: the theme, the navigation graph and a bottom bar with four tabs (Home, Library, Radio, Favourites) plus Settings. Each screen is a placeholder until its feature issue lands. The git repo is `silicon-circuits/swara-tarang` on GitHub (private for now, default branch `main`).

- Read [RULES.md](RULES.md) and [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) before any change. `RULES.md` has the hard stops and the coding standards. `docs/ARCHITECTURE.md` has the layers, the modules and the folder structure. [docs/FEATURES.md](docs/FEATURES.md) says what the app does.
- The work is in GitHub issues under milestones M1 to M10. Work them in milestone order and respect each issue's "Depends on" list. One issue, one branch, one PR.
- When a decision is made, record it in the "Decisions made" table of `docs/FEATURES.md`. A design decision made during the build also gets a short ADR in `docs/adr/`.

## Build, test and lint

Use JDK 21 (`JAVA_HOME`) and the Android SDK (`sdk.dir` in `local.properties`, or `ANDROID_HOME`). Gradle is run through the wrapper only.

| Task | Command |
|---|---|
| Build the debug app (the `core` build) | `./gradlew assembleCoreDebug` |
| All unit tests, with the build logic tests | `./gradlew test` |
| Unit tests of one module | `./gradlew :core:domain:test` (plain Kotlin module) or `./gradlew :feature:library:testDebugUnitTest` (Android module) or `./gradlew :app:testCoreDebugUnitTest` (the app) |
| One test class or one test | `./gradlew :core:domain:test --tests "*.SomeTest"` or `--tests "*.SomeTest.some test name"` |
| Only the build logic tests (module rules) | `./gradlew :build-logic:convention:test` |
| Save new pictures for the screenshot tests | `./gradlew test -PrecordScreenshots`, or one module: `./gradlew :app:testCoreDebugUnitTest -PrecordScreenshots` |
| Android lint | `./gradlew lint` |
| Style (ktlint); fix what can be fixed by itself | `./gradlew ktlintCheck`; `./gradlew ktlintFormat` (build logic: `./gradlew -p build-logic ktlintFormat`) |
| Code rules (detekt) | `./gradlew detekt` |
| Licences of the libraries that ship in the app | `./gradlew :app:licenseeCoreRelease` |
| Everything CI runs | `./gradlew test ktlintCheck detekt lint :app:licenseeCoreRelease assembleCoreDebug` |
| Premium boundary check alone | `./gradlew checkPremiumBoundary` |
| Licence header check alone | `./gradlew checkLicenceHeaders` |
| Install on a connected device | `./gradlew :app:installCoreDebug` |

- Application id and base package: `com.siliconcircuits.swaratarang`. A module's namespace follows its path (`:feature:library` is `com.siliconcircuits.swaratarang.feature.library`).
- Every version number lives in `gradle/libs.versions.toml`. A library is added there by the change that first uses it.
- Module build files apply a convention plugin and list their own dependencies, nothing more. The plugins are in `build-logic/convention` (`swaratarang.android.feature`, `swaratarang.android.library`, `swaratarang.android.application`, `swaratarang.android.compose`, `swaratarang.android.hilt`, `swaratarang.kotlin.library`).
- Every build fails when a module breaks the dependency table. The rule is in `build-logic/convention/src/main/kotlin/ModuleRules.kt`; change it only together with the table in `docs/ARCHITECTURE.md`.
- The app has the flavours `core` and `play`. `play` exists only when the private module is checked out in `premium/` (ignored by git). Flavour code lives in `app/src/core` (and `app/src/play` when it is needed); the `core` bindings of the premium interfaces are in `app/src/core/kotlin/.../di`.
- `checkPremiumBoundary` runs before every app build and with `./gradlew test`. It fails when a Kotlin, Java, XML or ProGuard file under `app`, `core` or `feature` names the package `com.siliconcircuits.swaratarang.premium`. The module rules also refuse a dependency on `:premium` from anything but the `play` configurations of `app`.
- Hilt is added to a module with the `swaratarang.android.hilt` plugin.
- The theme is `SwaraTarangTheme` in `core:ui/theme`: wallpaper colours by default, the indigo brand colours when `dynamicColor` is false. Spacing comes from `Spacing` there. Icons are vector drawables in `core/ui/src/main/res/drawable`; there is no icon library.
- Navigation: each feature has a `navigation/` package with its route classes (`@Serializable`) and one `NavGraphBuilder` function per screen. `app/navigation` joins them in `SwaraTarangNavHost`. The tabs are the entries of `TopLevelDestination`. `SwaraTarangShell` holds the screens, the mini player slot and the bottom bar. See `docs/adr/0001-navigation-and-tabs.md`.
- A screen that is not built yet uses `PlaceholderScreen` from `core:ui`. Replace it when the real screen is built, and remove it with the last one.
- The database is Room 3 (`androidx.room3`) in `core:data/local`: entities in `entity/`, DAOs in `dao/`, all `internal`. Read `docs/DATABASE.md` before any change to it. A module gets Room with the `swaratarang.android.room` plugin. The schema of every version is exported to `core/data/schemas/` and committed; CI fails when it differs from the entities. A change to an entity needs a higher version, a migration and a test in `SchemaTest`.
- A library save is "insert, or change the row with the same key". Never save a song with a replace: that removes its favourite, playlist and play count rows. A song whose file is gone is marked with `missing_since`, not removed.
- Search text goes through `FtsQuery` before it reaches the search table. Names are sorted by the key that `sortKey` makes.
- Settings are in one DataStore file. The keys are in `core:data/settings/SettingsKeys.kt`; a key name never changes.
- DAO tests run on an in-memory database with Robolectric (`testDatabase()` and the helpers in `core/data/src/test/.../local/TestLibrary.kt`). Robolectric is a test library of every Android module.
- Compose tests and screenshot tests run on the JVM with Robolectric and Roborazzi, as part of `./gradlew test`. Saved pictures are in `src/test/screenshots/` of the module. A test that taps or presses Back needs `RegisterComponentActivityRule` (see `app/src/test`). Robolectric runs API 36, set in `config/robolectric/robolectric.properties`. See `docs/adr/0002-screenshot-tests.md`.
- `checkLicenceHeaders` runs with `./gradlew test`. It fails when a Kotlin, Java, Gradle (`.kts`) or XML source file under `app`, `core`, `feature`, `build-logic` or the root does not start with the MPL-2.0 header. Copy the header from any other file.
- `LICENSE` is the MPL-2.0 text. `CONTRIBUTING.md`, `SECURITY.md` and the draft contributor agreement in `docs/CLA.md` are for outside contributors; the lawyer's review of them is check C9.
- ktlint and detekt come with the `swaratarang.quality` plugin, which every module convention plugin applies. Their settings are `.editorconfig` and `config/detekt.yml`; Android lint settings are `config/lint.xml`. The blocks in `config/detekt.yml` and `config/lint.xml` name the rule in `RULES.md` that they check.
- The licence check (licensee) allows Apache-2.0, MIT, BSD and MPL-2.0 for the libraries that ship in the app. Test only libraries are not checked. The list is in `swaratarang.android.application.gradle.kts`.
- CI is `.github/workflows/ci.yml`. It runs on the self hosted Mac runner (`runs-on: [self-hosted, macOS, ARM64]`), never on a GitHub hosted runner. Actions are pinned by commit hash and Dependabot keeps them and the Gradle libraries current. The workflow does not use the `setup-gradle` action: the runner keeps its Gradle home between runs, and on `main` that action hangs while it packs the whole folder into a GitHub cache.

## Decisions already made

| Decision | Effect on the code |
|---|---|
| minSdk 31 (Android 12) | Dynamic colour is always there. `READ_EXTERNAL_STORAGE` is only needed with `maxSdkVersion="32"`; API 33+ uses `READ_MEDIA_AUDIO` |
| Android Auto only for MVP | One APK for phone and car. No Android Automotive OS module yet |
| Stations come from the user (name and URL) or from Radio Browser search. We ship no station list of our own | Never bundle or host a station list. The India section is only a Radio Browser filter. No station logos unless a station gave permission; show letter tiles |
| Free core app with no ads, plus a premium subscription after v1.1 (ten features, listed in section 2.10 of `docs/FEATURES.md`) | No Play Billing in MVP or v1.1. Never gate a core feature |
| Open core: this public core is MPL-2.0, the premium features live in a separate private repo | Never add GPL or AGPL code or libraries. Apache-2.0, MIT, BSD and MPL-2.0 are fine. Public code must never import premium code; it only defines interfaces (such as `StorageSource`, `PremiumGate`) that the premium module binds through Hilt. Keep an open source licences screen in Settings up to date |
| Library index in Room, synced from MediaStore | Search, sorting and the Auto tree read Room, not MediaStore |
| Retrofit with OkHttp | One OkHttp client shared with the Media3 OkHttp data source |
| Crash reports only (Firebase Crashlytics) | No usage analytics SDKs |
| English first | Every user facing text in string resources |
| Equalizer as our own ExoPlayer audio processor | Do not use the platform `Equalizer` or `BassBoost` effects |
| Subscription checked on the phone through Play Billing; backup and sync stored in the user's own cloud storage | No backend of ours. Google Drive is left out of cloud storage at first |
| Two builds: `core` (public repo alone) and `play` (core plus premium, the Play Store build) | Play Billing, Cast and Crashlytics belong only in the premium module. The `core` build must compile and run with no closed source library and no `google-services.json` |

## Planned architecture

- Kotlin, Jetpack Compose, Material 3, MVVM with clean architecture layers (UI, domain, data). Hilt for DI, Room for the library index, favourites, playlists and history. DataStore for settings, MediaStore for scanning, Retrofit with OkHttp for Radio Browser, WorkManager for rescans.
- **One playback service for everything.** A single Media3 `MediaLibraryService` owns one ExoPlayer and one `MediaLibrarySession`. The phone UI talks to it through a `MediaController`. Android Auto, Google Assistant, Bluetooth, the notification, the lock screen and Wear OS all use the same session. Never add a second player or a separate path for the car.
- The Auto browse tree is built from the same repositories as the phone UI. The root has at most 4 tabs: Library, Radio, Favourites, Recent. Keep the tree shallow, and make each item either browsable or playable. A node returns its whole, short list: the car has no paging.
- Voice play ("play X on Swara Tarang") arrives as a search query in the session callback (`onSetMediaItems` / `onAddMediaItems` with `requestMetadata.searchQuery`) plus the `MEDIA_PLAY_FROM_SEARCH` intent filter.
- Radio streams cover Icecast, Shoutcast, HLS (`media3-exoplayer-hls`) and `.pls` / `.m3u` playlists. Now playing titles come from ICY metadata.
- Premium cloud storage playback (WebDAV, S3, OneDrive) plugs in behind one `StorageSource` interface in the data layer, next to MediaStore. The phone talks directly to the user's storage; there is no server of ours. Credentials are encrypted with the Android Keystore.
- Modules: `app`, `feature:*` (home, library, radio, player, search, settings), `core:domain` (plain Kotlin), `core:data`, `core:playback`, `core:ui`, `core:testing`, plus `build-logic`. A feature depends only on `core:domain` and `core:ui`. The full table and the folder tree are in `docs/ARCHITECTURE.md`.

## Open points to check before building

- The Android Auto limits were checked on 19 Nov 2026 and are in section 4 of `docs/FEATURES.md`, with a link to each source page. The car gives the tab limit in the root hints, there is no paging in the car, and artwork must be a `content://` or `android.resource://` URI. Check the table again before the Play release.
- The Radio Browser rules were checked on 19 Nov 2026 and are in section 2.3 of `docs/FEATURES.md`: server through DNS with no fixed host, HTTPS only, `SwaraTarang/<version>` as `User-Agent`, a click report only for directory stations, never the whole list, no code from its AGPL server.
- Media3 has no built-in crossfade or ReplayGain. The equalizer processor, the pause and rewind buffer for live radio and crossfade each need a short trial build before they are committed to.
- Section 9 of `docs/FEATURES.md` lists every check still open (legal view, exact alarm permission and more).
