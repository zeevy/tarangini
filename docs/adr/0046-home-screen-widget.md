# ADR 0046: Home screen widget

- Status: accepted, 21 Nov 2026.
- Rules: A1, A9, S1.

## Context

A widget on the home screen shows what plays and has play or pause and next, so the person does not need to open the app. It must work when the app is not running, and it must not add a second way into the player.

## Decisions

| Decision | Options looked at | Reason |
|---|---|---|
| Jetpack Glance 1.2.0, the latest stable version, in its own module `feature:widget` | RemoteViews by hand. The widget in `app` or in `core:playback` | Glance writes a widget the way the screens are written. A module of its own keeps the library out of every other module |
| The playback service tells the widget about each change: `NowPlayingFeed` (a player listener in `core:playback/card`) hands a `NowPlayingCard` to every `NowPlayingCards` in a Hilt set. The widget adds `WidgetCards` to the set | The widget asks the session itself. A timer | The widget runs only when it is drawn and must not hold a connection. The set keeps `core:playback` free of the widget, and it is empty in the tests of the service |
| Only a change the widget shows is passed on: a card equal to the last one, as after a seek, is not sent again. A slow widget gets only the newest card. The empty player at the start of the service is not sent | Send every event of the player | The widget updates within a second of a change, and does no work while a song plays on. The service restores the saved queue a moment after it starts, and the widget keeps the last song until then |
| The card is written into the saved state of each widget (Glance state), with the number of the process that plays | Keep it in memory | After the app or the phone was restarted the widget still shows the last song. Playback runs in the process of the app, so a card of a process that has ended shows a play button, also when the process had no time to say that it stopped |
| Play or pause is sent to the media button receiver of Media3, as a headset button | A broadcast of our own | The receiver starts the playback service when the app is not running, and the service plays what was saved last (`onPlaybackResumption`). No new way into the player |
| Next is sent to the playback service itself, and shows only while something is loaded and the process that wrote the card still runs | The media button receiver | The receiver of Media3 passes on only "play" keys when the app is not running, so it would drop "next". The service handles a media button in its start command |
| Both buttons send the same action, so each intent carries an address of its own (`tarangini-widget:key/<code>`) | Rely on the extras | Android treats two waiting intents with the same action and address as one, and the second button would send the key of the first |
| The cover is the small picture the system keeps for the song (`loadThumbnail` of the MediaStore address), read when the widget is drawn | The cover address of the app | The launcher is another app, and the cover provider of the app does not serve it |
| The widget follows the light or dark look of the phone and the colours of its wallpaper (`GlanceTheme`, Material You) | The one dark look of the app (D51) | A widget sits among the widgets of other apps on the home screen, which all follow the phone |
| Two sizes: small (110 by 40 dp) with the cover and the buttons, wide (from 250 dp) also with the title and the artist. Every button is 48 dp | One size | A widget of two cells must still work |
| `feature:widget` names `MediaButtonReceiver` and the action of `MediaLibraryService` from Media3 | Pass the intents in from `core:playback` | A feature may use only `core:domain` and `core:ui`, and the domain has no intents. The class and the action are the public API Media3 gives for this |

## What follows

- Next does nothing when the phone does not let the app start the service at that moment, for example while the app is paused in the background on some phones. Play still works.
- A stream shows its station and title, and no cover.
- The checks on a real phone are rows 133 to 135 of `docs/MANUAL-TESTS.md`.
