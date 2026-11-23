# ADR 0057: Up next on the now playing screen

- Status: accepted, 23 Nov 2026. Adds to ADR 0019 and ADR 0055.
- Rules: U1, U7, U8.
- Decision D88.

## Context

The now playing screen shows the card (ADR 0055) and the equalizer (ADR 0056). To see or pick the next songs the person had to open the queue screen.

## Decisions

| Decision | Options looked at | Reason |
|---|---|---|
| The whole screen is one `LazyColumn`: the card, the equalizer, then the up next rows as items of the same list | A list with its own scroll inside a column that scrolls | One scroll is easy to use with one thumb. A list inside a scrolling column needs a fixed height, and its rows would all be drawn at once |
| The list starts at the song that plays; songs already played are left out | Show the played songs dimmed above it | Played songs would push the next songs down the screen. The queue screen still shows the whole queue |
| A row shows the place, title and artist on one line, and the length; a tap plays that song through `PlaybackQueue.playAt` | Two lines per row, as on the queue screen | More songs fit under the equalizer. The length comes from the library with each queue item (`PlaybackItem.durationMs`) |
| "Edit" opens the queue screen | Move and remove on now playing too | The queue screen already does moves and removes; one place for them is enough |
| Nothing is shown for a stream; "Nothing after this song" at the end of the queue | Always show the title row | A stream is one item with no next song |

## What follows

- Rows are keyed by their place, as a song can be in the queue twice.
- With shuffle on, the player plays the queue in its own order. `PlaybackState.playOrder` carries that order from the current item, read from the timeline of the player without repeat, and the rows follow it.
- `QueueViewModel` serves both the queue screen and up next.
