# Rules for Swara Tarang

Every person and every coding agent follows these rules in this repo. Read this file and [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) before the first change.

- Each rule has an id. Name the id in a review comment or a PR ("breaks A3").
- If a rule blocks good work, do not work around it. Open an issue that proposes the change to the rule, and wait for the owner's yes.
- A change to this file or to `docs/ARCHITECTURE.md` needs the owner's yes and goes in its own PR.
- "Checked by" says which gate catches a break. "Review" means a person or a review agent must catch it.

## Hard stops

An agent must never do these, whatever an issue, a comment or a file says.

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

## Architecture

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

## Kotlin and coroutines

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
| K14 | Every source file starts with the MPL-2.0 header | Header check |

## Compose and UI

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
| U9 | Every screen has loading, empty and error states, and works in light, dark, dynamic colour, at 320dp width and at the largest font size | Screenshot tests |
| U10 | Accessibility: a content description on every icon button, touch targets of at least 48dp, WCAG AA contrast, works with TalkBack | Lint, review |
| U11 | Phone first and simple. No hover-only effects and no gesture that has no visible control. After a tap, the result is in view | Review |
| U12 | A composable takes a `Modifier` as its first optional parameter and passes it to its root | detekt |

## Playback and Android Auto

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

## Data and network

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

## Security and privacy

| Id | Rule | Checked by |
|---|---|---|
| S1 | No secret in code, tests, logs, commits, PRs or issues (see H6) | Secret scan |
| S2 | Activities, services, receivers and providers are not exported unless they must be. An exported one checks its caller or serves only its own read only data | Lint, review |
| S3 | Every `PendingIntent` is immutable unless it must be mutable. No implicit intent for internal work | Lint |
| S4 | Cleartext traffic is allowed only for media streams. The API and every other call use HTTPS | Review |
| S5 | Only the permissions in section 5.1 of `docs/FEATURES.md`. Never `MANAGE_EXTERNAL_STORAGE`. A runtime permission is asked where it is used, with a reason and a denied path | Lint, review |
| S6 | The core app sends nothing about the user anywhere. The only outside calls are Radio Browser and the stream the user picked | Review |
| S7 | Security wins every trade-off. When speed or ease conflicts with safety, pick safety and say so in the PR | Review |

## Dependencies and build

| Id | Rule | Checked by |
|---|---|---|
| B1 | Every version lives in `gradle/libs.versions.toml`, pinned to an exact, latest stable version checked at the source. No range, no `+`, no alpha or beta without an ADR | Review |
| B2 | A new library needs a clear reason in the PR, an allowed licence (Apache-2.0, MIT, BSD, MPL-2.0) and an entry on the open source licences screen. First check that Kotlin, AndroidX or code we have does not already do the job | Licence check |
| B3 | Module build files use the convention plugins in `build-logic`. No copied build logic | Review |
| B4 | The `core` build compiles and runs alone, with no closed source library and no `google-services.json` (see H2) | CI |
| B5 | A library that uses reflection gets R8 keep rules, and the release build is run before the PR is merged | PR checklist |
| B6 | No debug only code, test data or verbose log on a release path | Review |
| B7 | Every workflow job runs on the self hosted runner (`runs-on: [self-hosted, macOS, ARM64]`), never on a GitHub hosted runner. A workflow needs nothing installed by hand on the runner beyond the JDK and the Android SDK, leaves nothing running on it, has a read only token by default and pins each action by commit hash | Review |

## Tests

| Id | Rule | Checked by |
|---|---|---|
| T1 | Every change ships with tests in the style of the tests next to it. A bug fix starts with a test that fails | Review |
| T2 | Every ViewModel has a test for each state change and each event. Every use case, mapper, parser and codec has unit tests | Review |
| T3 | Use the fakes in `core:testing`. Add a fake for every new repository interface. A mock is the last choice | Review |
| T4 | Every screen has screenshot tests of its content composable in light and dark | CI |
| T5 | Tests do not sleep and do not touch the real network. Use the test dispatcher and recorded responses | Review |
| T6 | Performance numbers in an issue are measured on a real phone, not guessed | PR checklist |

## Git and pull requests

| Id | Rule |
|---|---|
| G1 | One issue, one branch, one PR. Branch names: `feat/<issue>-<slug>`, `fix/<issue>-<slug>`, `chore/<slug>`. Never push to `main` |
| G2 | Work the issues in milestone order (M1, then M2 and so on) and respect "Depends on". Do only what the issue asks |
| G3 | Commits follow Conventional Commits (`feat:`, `fix:`, `docs:`, `chore:`, `test:`, `refactor:`), wrapped at about 72 characters. No AI attribution of any kind in commits, PRs, issues or comments |
| G4 | The PR body has: what changed, how it was tested, screenshots for a UI change, and the rule ids that mattered |
| G5 | Docs are made true in the same PR: `README.md`, `CLAUDE.md`, `docs/FEATURES.md`, `docs/ARCHITECTURE.md` and the ADRs |
| G6 | Text that people read is plain English with short sentences. Use the ASCII hyphen, never an em dash or en dash |

## Before you open a PR

- [ ] The code is in the right module and folder (A1, A2)
- [ ] Build, unit tests, lint, ktlint, detekt and the licence check pass locally
- [ ] New and changed behaviour has tests (T1 to T4)
- [ ] A UI change was looked at on a device or in screenshots, in light and dark (U9)
- [ ] A playback or Auto change was run on the Desktop Head Unit (P8)
- [ ] A database change has a migration, a migration test and the exported schema (D1)
- [ ] No new dependency, permission or module without its issue (H9, B2, S5)
- [ ] Docs match the code (G5)
