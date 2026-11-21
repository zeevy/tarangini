# Contributing to Tarangini

Thank you for helping. This page says how to build the app, what a change must follow and what we need from you before we can merge it.

## Before you start

- For a bug, open an issue with the bug report form. For a new feature, open an issue first and wait for a reply before you write code. A feature that does not fit [docs/FEATURES.md](docs/FEATURES.md) may not be merged.
- A security problem is never reported in a public issue. Follow [SECURITY.md](SECURITY.md).
- The paid features are built in a private module. A pull request that adds one of them to this repo, or that puts a free feature behind a payment, is not accepted.

## Build and test

You need JDK 21 and the Android SDK with platform 37. The steps are in the [README](README.md#build).

| Task | Command |
|---|---|
| Build the debug app | `./gradlew assembleCoreDebug` |
| All unit tests and the build checks | `./gradlew test` |
| Android lint | `./gradlew lint` |
| Style and code rules | `./gradlew ktlintCheck detekt` |
| Licences of the shipped libraries | `./gradlew :app:licenseeCoreRelease` |

Run all of them before you open a pull request. CI runs the same commands and they must pass. `./gradlew ktlintFormat` fixes most style problems by itself.

CI also runs the end to end test of the main flows on an emulator. To run it yourself, start an emulator and run `./gradlew :app:connectedCoreDebugAndroidTest`. Use an emulator or a test phone, not your own phone: the test adds three short songs to the music of the device (and removes them), and leaves a favourite and a playlist in the app.

## Rules for a change

The full list is in [Rules with ids](#rules-with-ids) at the end of this page, and [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) says where code goes. The points that matter most:

- One issue, one branch, one pull request. Do only what the issue asks.
- Put the code in the right module. A feature module depends only on `core:domain` and `core:ui`. The build fails when a module breaks this.
- Every change comes with tests in the style of the tests next to it. A bug fix starts with a test that fails.
- Every new Kotlin, Java, Gradle and XML source file starts with the GPL-3.0 header. Copy it from any other file. `./gradlew checkLicenceHeaders` fails without it.
- Commit messages follow [Conventional Commits](https://www.conventionalcommits.org/) (`feat:`, `fix:`, `docs:` and so on).
- No ads, no analytics, no account and no secrets in the code.

## Third party code and libraries

This repo is under GPL-3.0. The Play Store build mixes this code with closed code, which the project owner may do only with code the owner can relicense. So the licence of everything we add matters, and GPL code from other projects cannot come in.

| Licence | Allowed |
|---|---|
| Apache-2.0, MIT, BSD, MPL-2.0 | Yes |
| GPL, AGPL (any version) | No |
| Anything else, or no licence | Ask in the issue first |

- A new library needs a clear reason in the pull request, and an exact version in `gradle/libs.versions.toml`. The build fails when a library that ships in the app has a licence outside the "Yes" row.
- Do not copy code from a web page, a book or another project unless its licence is in the "Yes" row. Say where it came from in the pull request.
- Do not add a station list, station logos, album art, fonts or sounds that you do not have the right to share.

## Checksums of the libraries

Every library and plugin the build downloads is listed with its SHA-256 in `gradle/verification-metadata.xml`. A file with another checksum, or a file that is not listed, fails the build and Gradle names it. So a change that adds or updates a library must also update this file.

1. Change the version in `gradle/libs.versions.toml`.
2. Run `GRADLE_USER_HOME=/tmp/gradle-home-for-checksums ./gradlew --write-verification-metadata sha256 --no-configuration-cache --no-build-cache test ktlintCheck detekt lint :app:licenseeCoreRelease assembleCoreDebug ktlintFormat`. The empty Gradle home matters: with your normal one, Gradle skips files it already has and the list comes out incomplete. `ktlintFormat` is in the list on purpose, so the files that task needs are listed too. Gradle adds the new entries and keeps the old ones. Delete the folder afterwards.
3. Read the diff of the file. Only the libraries you changed, and what they bring with them, should be new. If anything else changed, stop and find out why.
4. A new version of the Android Gradle plugin also needs the `aapt2` file of the other systems, because Gradle lists only the one for your system. Download `aapt2-<version>-osx.jar`, `-linux.jar` and `-windows.jar` from Google's Maven repository (`https://dl.google.com/dl/android/maven2/com/android/tools/build/aapt2/<version>/`), run `shasum -a 256` on each, and add the missing `<artifact>` entries next to the one Gradle wrote.
5. Remove the entries of the versions that are no longer used, and commit the file with the change.

Dependabot does not update this file. Its pull request fails CI with "Dependency verification failed" until someone checks out its branch, does steps 2 to 5 and pushes the result.

Source and javadoc jars, which Android Studio downloads for reading, are trusted without a checksum. Signatures are not checked yet.

What this check does not cover: the Android system jars that Robolectric downloads by itself when the tests run, and the JDK that CI installs. The Gradle download has its own checksum in `gradle/wrapper/gradle-wrapper.properties`.

`build-logic/gradle/verification-metadata.xml` is a link to the same file. On Windows, check the repo out with `git config core.symlinks true`, or Gradle cannot read it.

## Contributor agreement

Before we merge your first pull request, you agree to the contributor agreement in [docs/CLA.md](docs/CLA.md). In short: you keep the copyright in your work, and you give the project owner the right to use it and to license it under other terms, closed ones included. The owner needs this to ship the Play Store build, which adds the closed premium module to this code.

To agree, tick the agreement line in the pull request form. One time is enough for all your later pull requests.

The agreement has a version. If the text changes, the new version covers only what you send after you agreed to it.

## The pull request

- Fill in the pull request form. Link the issue with `Closes #<number>`.
- Add screenshots for a change that people can see.
- A maintainer reviews it. Small pull requests are reviewed faster.

## Rules with ids

This is the full list of rules for the code. A reviewer checks a pull request against it.

- Each rule has an id. Name the id in a review comment or a pull request ("breaks A3").
- If a rule blocks good work, do not work around it. Open an issue that proposes the change to the rule.
- A change to these rules or to `docs/ARCHITECTURE.md` goes in its own pull request.
- "Checked by" says which gate catches a break. "Review" means the reviewer must catch it.

### Hard stops

A change must never do these.

| Id | Never |
|---|---|
| H1 | Add a second player, a second media session or a separate playback path for the car |
| H2 | Import premium code from public code, or add a closed source library, Play Billing, Cast, Firebase or `google-services.json` to the `core` build |
| H3 | Add a GPL, AGPL or unknown licence dependency, or copy code from such a project |
| H4 | Bundle or host a station list, or download a station logo |
| H5 | Add ads, usage analytics, tracking or an account sign-in to the core app |
| H6 | Commit a secret, a signing key, a token or personal data |
| H7 | Skip, disable or delete a test, a lint rule or a CI check to make a run pass |
| H8 | Edit a Room migration or an exported schema file that is already on `main` |
| H9 | Add a module, change which module may depend on which, or add a permission to the manifest without an issue that says so |
| H10 | Put a core feature behind `PremiumGate` |
| H11 | Merge a contribution from outside whose author has not agreed to `docs/CLA.md`. The Play build joins this GPL-3.0 code with closed code, which is allowed only for code the project may relicense (ADR 0009) |

### Architecture

| Id | Rule | Checked by |
|---|---|---|
| A1 | Code lives in the module and package that `docs/ARCHITECTURE.md` gives for it. A new file follows the folder structure there | Review |
| A2 | Module dependencies follow the table in section 3 of `docs/ARCHITECTURE.md`. A feature depends only on `core:domain` and `core:ui`. A feature never depends on another feature, on `core:data` or on `core:playback`. `core:data` and `core:playback` never depend on each other | Build check |
| A3 | `core:domain` is plain Kotlin. No `android.*` or `androidx.*` import, no Room, Retrofit, Media3 or Compose type | Build (JVM module) |
| A4 | Room entities, DAOs, DTOs and DataStore keys are `internal` to `core:data`. Only domain models cross a module edge | Build, review |
| A5 | Media3 and ExoPlayer types stay in `core:playback`. ViewModels use `PlaybackController` from the domain | Build |
| A6 | A repository is the only writer and the only reader of its data. No ViewModel keeps its own copy of repository data | Review |
| A7 | The phone UI and the Android Auto browse tree read the same repository interfaces. Never a second query path for the car | Review |
| A8 | Only the scanner in `core:data/mediastore` queries MediaStore. Only `core:data/remote` calls Radio Browser. Everything else reads Room | Review |
| A9 | One use case is one class with one public `operator fun invoke`. No use case that only passes a call through; call the repository interface instead | Review |
| A10 | Navigation between features happens only in `app/navigation`. A feature offers its routes in its own `navigation/` package | Review |
| A11 | A design decision made during the build is written as a short ADR in `docs/adr/` with the options and the reason | Review |
| A12 | Reuse before you add. Look for a component, use case, mapper or fake that already does the job. Three copies of the same markup or logic become one shared piece | Review |

### Kotlin and coroutines

| Id | Rule | Checked by |
|---|---|---|
| K1 | ktlint and detekt pass with no new warning and no new baseline entry. No `@Suppress` without a one line reason | ktlint, detekt |
| K2 | No `GlobalScope`, no `runBlocking` outside tests, no `Thread`, `Handler` or `AsyncTask`. Use `viewModelScope`, the service scope or an injected scope | detekt |
| K3 | Dispatchers are injected (`@IoDispatcher`, `@DefaultDispatcher`). No hard-coded `Dispatchers.IO` in code that a test must control | detekt |
| K4 | No blocking I/O or parsing on the main thread | StrictMode in debug, review |
| K5 | Never catch `CancellationException` without throwing it again. Never an empty `catch` | detekt |
| K6 | Expected failures (no network, no permission, bad input) are values: a sealed result type from `core:domain/common`. Exceptions are for bugs | Review |
| K7 | No `!!`. No `lateinit` outside Android framework classes | detekt |
| K8 | Public state is read only: expose `StateFlow`, `Flow` and `List`, never `MutableStateFlow` or `MutableList` | detekt, review |
| K9 | Everything is `internal` or `private` unless another module needs it | Review |
| K10 | Names: `XxxScreen`, `XxxViewModel`, `XxxUiState`, `XxxRepository` (interface), `DefaultXxxRepository` or `OfflineFirstXxxRepository` (implementation), `XxxUseCase`, `XxxEntity`, `XxxDto`, `XxxDao`, `FakeXxxRepository`. One top level class per file, file named after it | detekt, review |
| K11 | A function does one thing and fits on one screen. A file over about 400 lines or a composable over about 150 lines is split | detekt |
| K12 | Comments explain why, and stand alone: no issue numbers, dates or history. Public classes and interfaces in `core:*` have a short KDoc | Review |
| K13 | Log through the one app logger, never `Log.*` or `println`. Never log a file path, a search query, a stream URL with credentials or any token | detekt, review |
| K14 | Every source file starts with the GPL-3.0 header | Header check |

### Compose and UI

| Id | Rule | Checked by |
|---|---|---|
| U1 | One immutable `UiState` per screen, exposed as one `StateFlow`. One-off events (navigation, snackbar) go through a `Channel`, never a flag in the state | Review |
| U2 | Each screen has a stateful entry composable that gets the ViewModel and a stateless content composable that takes state and callbacks. `hiltViewModel()` is called only in the entry | Review |
| U3 | A ViewModel holds no `Context`, `Activity`, `View` or Compose type. State that must survive process death uses `SavedStateHandle` | detekt, review |
| U4 | Collect flows with `collectAsStateWithLifecycle`. Side effects only in `LaunchedEffect`, `DisposableEffect` or a callback | Lint, review |
| U5 | `LazyColumn` and `LazyGrid` items have stable keys. Lists page from Room; no screen loads a whole table | Review |
| U6 | Every user facing text is a string resource, with plurals where a count shows. No text built by joining strings | Lint |
| U7 | Colours, type, shapes and spacing come from the theme in `core:ui`. No hard-coded colour, `sp` or `dp` value for these in a feature | detekt, review |
| U8 | Shared pieces (song row, station tile, empty and error states) live in `core:ui`. A feature does not keep a private copy | Review |
| U9 | Every screen has loading, empty and error states, and works in the one dark look of the app, at 320dp width and at the largest font size | Screenshot tests |
| U10 | Accessibility: a content description on every icon button, touch targets of at least 48dp, WCAG AA contrast, works with TalkBack | Lint, review |
| U11 | Phone first and simple. No hover-only effects and no gesture that has no visible control. After a tap, the result is in view | Review |
| U12 | A composable takes a `Modifier` as its first optional parameter and passes it to its root | detekt |

### Playback and Android Auto

| Id | Rule | Checked by |
|---|---|---|
| P1 | One `MediaLibraryService`, one ExoPlayer, one `MediaLibrarySession` (see H1) | Review |
| P2 | The session callback checks the caller. Only this app, Android Auto, Assistant and the system may connect. A test proves an unknown caller is refused | Test |
| P3 | Media ids are made and read only by the `MediaId` codec. An id that does not parse gives an error result, never a crash | Test |
| P4 | The Auto root has at most 4 tabs. The tree is shallow. Each item is browsable or playable, never both. Limits come from section 4 of `docs/FEATURES.md` | Test |
| P5 | Nothing in the car asks for sign-in, settings or long text. Errors reach the car through the session with a short message | Review |
| P6 | Stream URLs are `http` or `https` only. Playlist files (`.pls`, `.m3u`) are resolved with limits on size, redirects and depth | Test |
| P7 | The equalizer is our own audio processor. Never the platform `Equalizer` or `BassBoost` | Review |
| P8 | A change to the player, the session or the browse tree is run on the Desktop Head Unit before the PR is merged | PR checklist |

### Data and network

| Id | Rule | Checked by |
|---|---|---|
| D1 | Every Room schema change bumps the version, exports the schema JSON and ships a migration with a migration test. No destructive migration | Test, CI |
| D2 | Every query that backs a list, a search or a join has an index, and the PR says which | Review |
| D3 | A write of several rows runs in one transaction. A rescan never removes favourites, playlists or history | Test |
| D4 | DataStore key names never change. Values that change together are read and written as one model | Review |
| D5 | One OkHttp client for the whole app, shared by Retrofit and the Media3 data source | Review |
| D6 | The Radio Browser API is called over HTTPS, with our `User-Agent`, a server picked through DNS, time limits and a response size limit | Test |
| D7 | Everything from outside is untrusted: API fields, ICY titles, playlist and import files, tags in audio files, voice queries, media ids. Check length and form before use. XML is parsed with external entities off | Test, review |
| D8 | SQL only through Room with bound parameters. No query built by joining strings | Review |

### Security and privacy

| Id | Rule | Checked by |
|---|---|---|
| S1 | No secret in code, tests, logs, commits, PRs or issues (see H6) | Secret scan |
| S2 | Activities, services, receivers and providers are not exported unless they must be. An exported one checks its caller or serves only its own read only data | Lint, review |
| S3 | Every `PendingIntent` is immutable unless it must be mutable. No implicit intent for internal work | Lint |
| S4 | Cleartext traffic is allowed only for media streams. The API and every other call use HTTPS | Review |
| S5 | Only the permissions in section 5.1 of `docs/FEATURES.md`. Never `MANAGE_EXTERNAL_STORAGE`. A runtime permission is asked where it is used, with a reason and a denied path | Lint, review |
| S6 | The core app sends nothing about the user anywhere. The only outside calls are Radio Browser and the stream the user picked | Review |
| S7 | Security wins every trade-off. When speed or ease conflicts with safety, pick safety and say so in the PR | Review |

### Dependencies and build

| Id | Rule | Checked by |
|---|---|---|
| B1 | Every version lives in `gradle/libs.versions.toml`, pinned to an exact, latest stable version checked at the source. No range, no `+`, no alpha or beta without an ADR | Review |
| B2 | A new library needs a clear reason in the PR, an allowed licence (Apache-2.0, MIT, BSD, MPL-2.0) and an entry on the open source licences screen. First check that Kotlin, AndroidX or code we have does not already do the job | Licence check |
| B3 | Module build files use the convention plugins in `build-logic`. No copied build logic | Review |
| B4 | The `core` build compiles and runs alone, with no closed source library and no `google-services.json` (see H2) | CI |
| B5 | A library that uses reflection gets R8 keep rules, and the release build is run before the PR is merged | PR checklist |
| B6 | No debug only code, test data or verbose log on a release path | Review |
| B7 | Every workflow job runs on a GitHub hosted Linux runner (`runs-on: ubuntu-24.04`); the repo is public, so this is free. A workflow has a read only token by default, pins each action by commit hash, and uses only GitHub's own actions and `gradle/actions` | Review |

### Tests

| Id | Rule | Checked by |
|---|---|---|
| T1 | Every change ships with tests in the style of the tests next to it. A bug fix starts with a test that fails | Review |
| T2 | Every ViewModel has a test for each state change and each event. Every use case, mapper, parser and codec has unit tests | Review |
| T3 | Use the fakes in `core:testing`. Add a fake for every new repository interface. A mock is the last choice | Review |
| T4 | Every screen has screenshot tests of its content composable | CI |
| T5 | Tests do not sleep and do not touch the real network. Use the test dispatcher and recorded responses | Review |
| T6 | Performance numbers in an issue are measured on a real phone, not guessed | PR checklist |

### Git and pull requests

| Id | Rule |
|---|---|
| G1 | One issue, one branch, one PR. Branch names: `feat/<issue>-<slug>`, `fix/<issue>-<slug>`, `chore/<slug>`. Never push to `main` |
| G2 | Respect the "Depends on" list of the issue. Do only what the issue asks |
| G3 | Commits follow Conventional Commits (`feat:`, `fix:`, `docs:`, `chore:`, `test:`, `refactor:`), wrapped at about 72 characters. |
| G4 | The PR body has: what changed, how it was tested, screenshots for a UI change, and the rule ids that mattered |
| G5 | Docs are made true in the same PR: `README.md`, `docs/FEATURES.md`, `docs/ARCHITECTURE.md` and the ADRs |
| G6 | Text that people read is plain English with short sentences. Use the ASCII hyphen, never an em dash or en dash |
