# ADR 0018: Mini player

- Status: accepted, 20 Nov 2026.
- Rules: A10, A12, U1, U7, U8, T2.

## Context

The item that plays must be in view and under control on every screen: its picture, title and artist, play or pause, next, and a thin line that shows how far it has played. A tap opens the now playing screen. The shell of the app has had an empty slot for this bar since ADR 0001.

## Decisions

| Decision | Options looked at | Reason |
|---|---|---|
| The bar sits in the bottom bar area of the shell, above the tabs | A layer drawn over the screens | The screens get their size from what is left above the bottom area, so a list ends above the bar and no row is covered. A layer on top would need every list to add space for it |
| The bar shows on every screen, also on one with no tabs (settings, search, an album) | Only on the tabs | The item must stay under control everywhere. With no tabs under it, the bar keeps clear of the gesture bar of the phone. A hidden bar takes no space there |
| The bar is hidden on the now playing screen | Show it there too | That screen shows the same item with all its controls |
| The bar is hidden while nothing is loaded | A bar with empty texts | An empty bar takes a row from every screen and its buttons would do nothing |
| The shell does not know the bar. The activity hands it in, and the bar gets the way to open now playing | The shell calls the bar itself | Tests of the shell need no player, and the tests of where the bar sits use a plain text in its place |
| The button shows "pause" from the tap on play, also while the player still loads | Follow "sound is coming out" | A radio stream can load for seconds. With "play" shown in that time, a second tap would not stop it |
| After an error and at the end of the queue the button shows "play", and a tap starts the item again | Show what the player reports | The player still says that play was asked for in both cases, but nothing plays. The notification offers play then, and the bar must show the same |
| The line shows the position in steps of a thousandth of the item | Hand every position on | The position changes several times a second. The line is at most a screen wide, so smaller steps cannot be seen, and the bar is not drawn again for them |
| While the player waits for data the line moves from side to side. With no known length and no wait it is an empty space of the same height | Hide the line | The bar keeps its height in every state, so the screen above it does not jump |
| The now playing screen is a placeholder with its route for now | Leave the tap with nothing to open | The tap and the way back can be built and tested now. The real screen replaces the placeholder |

## Consequences

- The bar reads the player only through `PlaybackController`, as every screen does (ADR 0005), so it is in step with the notification: both show the one session.
- The player is watched only while the bar is on screen.
- An item with no title or no artist shows "Unknown title" or "Unknown artist".
- Previous and seek are not on the bar. They are on the now playing screen and in the notification.
- A picture test of a state that moves (the wait for data) holds the clock of the test, or it never ends.
- The checks that need a phone are in `docs/MANUAL-TESTS.md`.
