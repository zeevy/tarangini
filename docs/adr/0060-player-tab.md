# ADR 0060: The player as the first tab

- Status: accepted, 24 Nov 2026. Replaces ADR 0033, ADR 0055, ADR 0056 and ADR 0057. Changes ADR 0047 and ADR 0018.
- Rules: U9, P8.
- Decision D91.

## Context

The app opened on Home, and the player was a screen on top of the tabs, with the queue and the equalizer on screens of their own. The new design puts the player on the first tab and makes it the screen the app opens on, as on a hardware player: one display with the time, a visual of the sound, the song and the keys, and the queue right under it.

## Decisions

- Tabs: Player, Library, Favourites. Home is gone, with the `feature:home` module. Recent and most played songs stay in the Library and in the car.
- The player tab is `NowPlayingScreen` in `feature:player`. It holds a header (the app name, where the queue came from, Search and More), the player card and Up next. While Up next is scrolled the card shrinks to one compact line, and a tap on it brings the card back.
- The display shows the play mark, the time (a tap shows the time left), Shuffle and Repeat, status tags (sleep timer, speed, volume levelling) only while they are on, the visual, and the song with its place in the queue. A tap on the song opens its album.
- Previous and Next take a tap to move one song, and a hold to move 10 seconds at a time. TalkBack gets the 10 seconds as a custom action.
- The seek bar has a small grip, as in the design. This replaces the bar with no handle of D47.
- The visual is bars (32 bands from 40 Hz to 16 kHz), a wave, or off. A tap on it picks the next mode, which is saved (`visual_mode`). It is fed by the level meter processor: the samples go into a ring buffer, and an FFT runs every 40 ms, only while the visual is on screen. Nothing is measured while nobody watches.
- Up next is one list card: the song that plays stays at the top, the next songs follow in the order they will play. Save keeps the queue as a new playlist, Clear keeps only the song that plays. A row is removed with a swipe to the left or by TalkBack; Undo stays for 4 seconds before the song really leaves the queue. A long press and drag moves a row, except while shuffle is on.
- With nothing loaded the player offers Shuffle all songs and Open library.
- The equalizer screen and the faders are gone. The bands are no longer applied; their saved values stay, so they can come back later. Volume levelling stays, as a switch in Settings.
- Settings opens from the More menu of the player.
- On a tablet the player pane stands beside the other tabs, not beside the player tab, and the mini player is hidden on the player tab.

## Results

- One screen shows everything a person needs while music plays, and the app opens on it.
- The queue screen, the equalizer screen and Home are gone, so there are fewer places to keep in step.
- Screenshot tests cover every state in both looks, at 320 dp, at the largest font and on a wide screen, and each state passes the accessibility checks.
