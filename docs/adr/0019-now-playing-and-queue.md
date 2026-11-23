# ADR 0019: Now playing screen and queue

- Status: accepted, 20 Nov 2026. The layout of the now playing screen is replaced by ADR 0055; up next on it is ADR 0057.
- Rules: A10, A12, U1, U7, U8, T2.

## Context

The now playing screen is the full player: the cover, the texts, a seek bar and every button. From it the person opens the queue, where an item is dragged to another place or swiped away. There are no drawn designs for these screens, so they follow Material 3 and the look of the rest of the app.

## Decisions

| Decision | Options looked at | Reason |
|---|---|---|
| The queue is its own screen, opened by a button in the top bar of now playing | A sheet that slides up over the player | A list that is dragged and swiped inside a sheet that is itself dragged fights for the same gestures. A screen has the whole height for the list |
| The mini player is hidden on both screens | Show it on the queue | Both show the current item. On the queue the row that plays is marked, and one Back leads to all controls |
| The seek bar tells the player only when the finger lifts | Seek at every step of the drag | A seek for each step makes the sound stutter |
| After a seek the screen shows the place asked for until the player reports a place near it, for at most 2 seconds | Show what the player reports | The player goes on reporting the old place for a moment. Shown as it is, the thumb jumps back and then forward. The time limit keeps the bar alive when a seek is refused |
| An item with no known length is live: no seek bar, no shuffle and no repeat, and a line that says "Live" or "Loading" | One layout for all | A stream has no place to seek to and is one item with no end. The station name is the title and the song on air is the line under it |
| The length of an item comes from the library until the player has opened the file | Only what the player reports | A queue that came back at the start is not opened yet. Without the length it would show as live |
| The cover is above the controls on an upright screen and beside them on a wide one, and the controls scroll when they do not fit | One upright layout | A phone on its side is about 320dp high. The title has one line there so every button fits |
| One button for repeat goes through off, all and one | Two buttons, or a menu | It is the usual control, and its name for a screen reader is the mode that is on |
| The drag to another place is our own small code (`ReorderState`) | A library for it | It is about 50 lines, and a library would be one more thing to keep current and to check the licence of. The limit: the list does not scroll by itself while a row is dragged to its edge |
| The player is told about a move once, when the finger lifts | Tell it at every row that is passed | Each change makes the player send its whole queue to every listener (ADR 0006) |
| A row has "move up", "move down" and "remove" as actions for a screen reader | Only the gestures | A drag and a swipe cannot be done with a screen reader or a switch |
| A tap on a row plays that item from its start (`PlaybackQueue.playAt`) | No tap | A list of songs that does nothing on a tap looks broken |
| No favourite button yet | A button that does nothing | Favourites are their own piece of work. The button comes with them |

## Consequences

- A song can be in the queue twice. A row is known by its id and the count of the same id before it.
- The list opens at the item that plays.
- The play button follows the same rule as the mini player (`showsPause`): "pause" from the tap on play, "play" after an error and at the end of the queue.
- The sleep timer and the speed control are not here. They are planned for v1.1.
- The checks that need a phone are in `docs/MANUAL-TESTS.md`.
