# ADR 0055: Now playing card

- Status: accepted, 23 Nov 2026. Changes the layout part of ADR 0019.
- Rules: U1, U7, U8.
- Decision D86.

## Context

The new now playing screen puts the song, the equalizer (#163) and the next songs (#164) on one screen. This change is the first part: one card at the top with everything about the song that plays and the keys of the player.

## Decisions

| Decision | Options looked at | Reason |
|---|---|---|
| One card: the format line and the place in the queue, the left and right level meter beside a large time, the title and the artist with the heart, the plain seek bar with the time played and the time left, a thin line, then five square keys | Keep the cover and the round buttons | The cover leaves room for the equalizer and the list under the card. It stays in the mini player, the widget, the notification and the car |
| The top bar says "Now playing" with the album of the song under it | "Playing from album, playlist, artist, genre or folder" with its name | The queue does not keep where it came from yet. The album of the song is true for every queue. The source is follow-up work |
| The numbers use JetBrains Mono (OFL), cut to the Latin letters, digits and a few marks (20 KB) | Manrope with tabular digits. The whole font file (303 KB) | Every digit is as wide as the next, so a time that counts up does not move. The cut keeps the app small |
| The keys are 52 dp high and a fifth of the row wide. The screen and the card keep 12 dp at the sides and 6 dp between keys | 16 dp sides and 8 dp gaps, as drawn | At 320 dp the drawn spacing makes a key 45.6 dp wide. With 12 and 6 dp each key is 49.6 dp, over the 48 dp a finger needs |
| The level meter has its own view model, and the card reads the level only inside the meter | Add the levels to `NowPlayingViewModel` | That view model is at its limit of parameters, and 25 levels a second would draw the whole screen again each time |
| The meter is still in a pause, while the player waits for data and after an error. It stands for a stream too | Leave it out for a stream | A stream is decoded to the same samples as a file, so its level is real |
| The large time gets smaller to fit (from 38 down to 18 sp) before it is cut. Title and format line take two lines when they need them | Cut with an ellipsis | At font scale 2 and 320 dp the time would be cut otherwise |
| On a wide screen the card keeps a width of at most 560 dp in the middle | Spread it over the full width | The keys stay in reach and the line lengths stay short |

## What follows

- Shuffle is a toggle for a screen reader ("on" or "off"), repeat says its mode, and the place in the queue is read as "Song 2 of 8".
- The meter colours per look are `levelColors` in `core:ui`, picked by the ground colour of the theme.
- The licence of JetBrains Mono is in the open source licences screen with Manrope.
