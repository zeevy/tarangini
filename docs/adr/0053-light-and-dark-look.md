# ADR 0053: A light and a dark look

- Status: accepted, 23 Nov 2026. Replaces ADR 0027.
- Rules: U9.
- Decision D83.

## Context

ADR 0027 gave the app one dark look so the phone would match the car. Android Auto draws its own screens from the media session, so the colours of the app never reach the car. On the phone a dark only app is hard to read in daylight, and most apps follow the dark mode of the phone. The new now playing screen (#162) was drawn in a light look with a dark pair.

## Decisions

| Decision | Options looked at | Reason |
|---|---|---|
| Two looks: Daylight (ground `#F4F1EA`, white cards, text `#15181D`, accent `#0D7050`, a little darker than the first drawing so small text keeps 4.5:1 on dialogs and the selected tab) and Night (ground `#0F1311`, cards `#1A1F1C`, text `#EEF2EE`, accent `#46D39A`). Both use orange for warnings and the heart | Keep the one dark look. Four colour sets to pick from. The wallpaper colours | Two looks are drawn and checked twice, not four times. One green accent keeps the app the same in both. Fixed colours keep the contrast that `ColorsTest` measures, which a wallpaper colour cannot promise |
| The look follows the dark mode of the phone by default. Settings has "Colours" with Follow the phone, Light and Dark, saved under the old key `theme_mode` | Always follow the phone. Light only | People who want one look all day can have it. The old key was never written by a released build, so it can be used again; a value that is not a `Look` counts as no value |
| The colours stay two Material 3 colour schemes (`LightColors`, `DarkColors`), and main buttons use the accent (`secondary` is the accent too) | Our own colour system. White main buttons | Every Material component keeps working with no colour set by hand. White buttons only made sense on the dark glass |
| Cards and floating panels use `PanelColor` (the `surfaceContainer` colour) and `PanelEdge` (a 1 dp edge in `outlineVariant`). The glass background with its purple and blue glow is removed | Keep the glow in the dark look | A glow on paper looks wrong, and one way of drawing cards serves both looks |
| `MainActivity` hands the look to Android as the night mode of the app (`UiModeManager.setApplicationNightMode`, API 31): auto for Follow the phone, no for Light, yes for Dark. The theme reads the dark mode it then sees | Read the setting in Compose and pass it to the theme. AppCompat for its night mode | Android keeps the mode between starts, so the window before the first frame (`values-night`), the icons of the status bar and the gesture bar, and the system dialogs have the colours of the look, with no flash at a start. The minimum SDK is 31, so no library is needed |

## What follows

- A pick starts the screen again in the new mode, as a change of the dark mode of the phone does.
- `TaranginiTheme(darkTheme)` follows the dark mode it sees when no value is given, so a test with the Robolectric qualifier `+night` shows the dark look.
- The main screens have a `*_dark` screenshot and an accessibility check in the dark look next to the light ones.
- JetBrains Mono for the numbers of the now playing card comes with #162, the change that first uses it.
- The home screen widget still follows the phone and its wallpaper (ADR 0046).
