# ADR 0006: The queue, shuffle and repeat

- Status: accepted. Decided by dev in auto mode, 19 Nov 2026.
- Rules: A5, A9, H1, T6.

## Context

Users change what plays next: play next, add to the end, reorder, remove, clear, shuffle and repeat. "Play all songs" can mean 20,000 songs, and the first one must be heard in under a second.

## Decisions

| Decision | Options looked at | Reason |
|---|---|---|
| The queue lives only in the player of the playback service. The app keeps no second copy | A queue of our own in the domain that is pushed to the player | One owner (H1). Android Auto, Bluetooth and the notification change the same queue through the session, so a copy of ours would go wrong |
| Screens change the queue through the interface `PlaybackQueue` in `core:domain`, next to `PlaybackController` | One use case class per operation, as the issue text says | Each use case would only pass the call on, which rule A9 forbids. An interface in the domain gives the phone screens one shared place all the same. The car changes the queue through the session |
| Shuffle and repeat are the player's own modes | Shuffle the list ourselves before loading it | The player keeps the queue order and picks at random, so switching shuffle off gives the order back |
| A place in the queue is its index in the playback state. A place that does not exist is ignored | Ids for queue entries | The same song can be in the queue twice, so a song id does not name an entry. An index from a state that is a moment old can be wrong by one; a wrong place must never crash |
| A list goes into the player in parts: 20 items from the asked place first, then, once sound is out (or after 3 seconds), the rest in parts of 5,000 | The whole list in one call; many small parts | Measured on the emulator with 20,000 items. Many small parts (1,000 each): first sound after 11.3 s, because every part makes the player send its whole queue to every listener. One start part and large parts: first sound after 0.67 s, whole list after 4.6 s |
| Loads run one after the other. A new "play" gives up a load that still runs | Let loads run side by side | Side by side they would mix their items |
| No upper limit on the queue size | Cut the queue at a fixed size | Cutting "play all" without telling the user is wrong, and a limit needs a design for what happens at its end. See the open point below |

## Result

- Removing the current item makes the next one current. Clearing the queue stops playback.
- "Play next" and "add to the end" with an empty queue start to play.
- A list of 20 items or fewer goes into the player in one call.
- With shuffle on, "play next" still puts the files after the current item in the queue, but the player picks its own moment to play them. Making them truly next under shuffle is left for the queue screen.
- When the user moves or removes an item near the front while the items before the start place are still loading, those items can land one place off. The load takes a few seconds, so this is left as it is.
- The time to first sound was measured on an emulator, not on a phone. Rule T6 asks for a real phone; the check is number 12 in `docs/MANUAL-TESTS.md`.

## Open point: very long queues and the system

Media3 shows the whole queue to the Android media session. With 20,000 items the Bluetooth service of the emulator wrote about 358,000 log lines in 50 seconds (about 9 per item, 9,300 for 1,000 items), so it works on every item each time the queue changes. Playback was not disturbed. On a real phone this may cost battery, or be slow with a car stereo; checks 14 and 15 in `docs/MANUAL-TESTS.md` look at it.

If it is a problem, the options are: show only a window of the queue to the system, keep only a window of a long list in the player and feed it as playback moves on, or limit the queue size. Each one changes what Bluetooth and Android Auto can show, so it is decided with the Android Auto queue.
