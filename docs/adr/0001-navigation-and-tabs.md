# ADR 0001: Navigation Compose with typed routes, and how the tabs behave

- Status: accepted. Decided by dev in auto mode, 19 Nov 2026.
- Rules: A10, U11.

## Context

The app needs one navigation graph, a bottom bar and a way for each feature to offer its screens without knowing the other features.

## Options

| Option | For | Against |
|---|---|---|
| Navigation Compose 2 with typed routes | Stable. Keeps and restores the state of each tab by itself. A route is a class, so a wrong argument fails at compile time | Needs the Kotlin serialization compiler plugin in feature modules |
| Navigation Compose 2 with text routes | No extra plugin | Arguments are text, so a mistake shows only when the app runs |
| Navigation 3 | Newer, and the app owns the back stack | We would write and test the state of each tab ourselves |

## Decision

- Navigation Compose 2 with typed routes. Each feature has a `navigation/` package with its route classes and one `NavGraphBuilder` function per screen. Only `app/navigation` joins them.
- The bottom bar has the four tabs of the approved design: Home, Library, Radio and Favourites. Settings opens from the Home top bar and has no tab.
- Only one tab is on the back stack at a time. Back from the first screen of any tab leaves the app. A tab keeps its screens and their state while the user is on another tab.
- The bottom bar shows only on the tabs. A screen opened on top of a tab, such as Settings, has a back arrow instead.
- A tab label stays on one line. It follows the font size the user picked and shrinks only as far as needed to fit, never below its size at the standard font setting. Each label is sized by itself, so at the largest font on a narrow screen a long label such as Favourites is smaller than the short ones.

## Result

- `kotlinx-serialization-core` and the serialization plugin are added for feature modules. Both are Apache-2.0.
- A new tab is one more entry in `TopLevelDestination`.
