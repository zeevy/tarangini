# ADR 0031: Play history

- Status: accepted, 21 Nov 2026.
- Rules: A9, P1.

## Context

The app shows a quick way back to what was heard last, and later a "most played" list. The table `song_plays` exists since the first schema: one row per song with the time of the last play and a count. Radio is out of the first release (decision D48), so station plays are not written now.

## Decisions

| Decision | Options looked at | Reason |
|---|---|---|
| A song counts as played once it was heard for 10 seconds (`CountPlays`, `core:playback/player`). Time in pause does not count. An item counts once each time it starts | Count at the start. Count at the end. Half of the length | A skip in the first seconds is not a play. A person who stops a long song after a minute did hear it |
| The playback service writes the play, through `PlayHistory` from `core:domain` | The screens write it | The car, a headset button and the notification start songs too, with no screen open |
| A play is written only for a song the library knows, in one statement that also adds one to the count | Insert and catch the foreign key error | A song can go while it plays. Two plays at once are both counted |
| No trim | Keep the last N plays | There is one row per song, so the table cannot be longer than the library. A trim would lose the counts that "most played" needs |
| `clear()` forgets every play, the counts too | Clear the times only | "Clear history" in Settings means that nothing of it is left |
| The car has a Recent tab after Favourites, with the 50 songs heard last (`MediaId.Recent`). A tap plays the list from that song on | Recent under Library | One tap for the driver. Three of the four tabs the car allows are used now |
| After a play was counted the car is told about the Recent list only, when it has it open | Tell every open list | The list the driver looks at must not be read again for it |

## What follows

- The time heard is counted in steps of one second, so a pause can cost a part of a second. A file shorter than 10 seconds never counts.
- While the driver looks at Recent, the list is read again about 11 seconds into each new song, because its order changed.
- The phone shows recent songs on the Home screen, which is its own piece of work.
- Recent stations (the last 20) come back with radio.
- "Clear history" gets its button with the Settings screen.
