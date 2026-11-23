# ADR 0027: One dark look

- Status: replaced by ADR 0053 on 23 Nov 2026.
- Rules: U9.

## Context

The app had three sets of colours: the wallpaper colours of Android, an indigo brand theme in light and in dark. Every screen had to be drawn and checked in each. The main place of the app is the car, and the screens of a car are dark. I want the phone to look like the car, and one look that is easy to read while driving.

## Decisions

| Decision | Options looked at | Reason |
|---|---|---|
| One look, dark only: background `#06080F`, text `#F2F5FA`, glass panels, white main buttons with dark content, one accent (cyan `#5CE1FF`) for the playing item and progress | Keep light and dark. Keep the wallpaper colours as a choice | A dark screen is right in the car by day and by night. One look is drawn and tested once. An accent picked from a wallpaper can fall below the contrast a driver needs |
| The colours are still a Material 3 colour scheme (`GlassColors`) | Our own colour system next to Material | Every Material component keeps working with no colour set by hand, apart from the few listed below |
| Text is Manrope, as one variable font file in `core:ui` | The system font. A font downloaded at run time | The look needs light large titles, which Roboto does not give well. A file in the app needs no network and no Google Play services. The licence is the SIL Open Font License 1.1; its text ships in `assets/licences` |
| A screen draws its background with `Modifier.glassBackground()` and its frame and top bar have no colour (`glassTopBarColors()`). List rows are clear | Draw the background once in the app shell | A screen must look right alone, in its own screenshot test, without the shell |
| The bottom bar and the mini player are glass pills (`GlassPanel`, `GlassEdge`) with space around them | Bars from edge to edge | They read as controls that float over the screen, as in the car |
| The seek bar stays a plain bar with no handle (decision D47) | A bar pattern | Decided before; the plain bar is the easiest to read |

## What follows

- `TaranginiTheme` takes no options. The settings key `dynamic_color` is not used.
- Screenshot tests save one picture per state.
- A filled main button sets the white colours itself (`MessageState`, the play button), because the Material default for it is the accent.
- The panels are not blurred. A blur of what is behind a panel needs a render effect per panel and is left until it is shown to be worth its cost.
