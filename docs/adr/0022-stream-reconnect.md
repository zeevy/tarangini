# ADR 0022: A stream that drops is tried again

- Status: accepted, 20 Nov 2026.
- Rules: P1, P2, S6.

## Context

Radio in a moving car loses the network often. The player must come back by itself, show that it is loading in the meantime, move to another address of the station when one is dead, and stop with a clear error when nothing helps.

## Decisions

| Decision | Options looked at | Reason |
|---|---|---|
| The tries are made inside the player, by a load error policy (`StreamRetryPolicy`): after 1, 2 and 4 seconds, then every 5 seconds | A listener that prepares the player again after each error | With the policy the player stays in the loading state between the tries. With a listener it would report an error each time, and the screens, the notification and the car would show a failure every few seconds |
| The limit is 2 minutes of failures by the clock. The time starts again when sound starts, with another item, after an error, and when nothing failed for 45 seconds | A number of tries | A try on a weak network can hang for seconds, and Media3 starts its count again when it reopens a live stream. Time is what the person feels. The 45 seconds are for a drop that mends while the buffer still plays: the player reports nothing then |
| No wait is longer than 5 seconds, and nothing watches the network | Watch the network with `ACCESS_NETWORK_STATE` and try the moment it is back | A try with no network fails at once and costs nothing, so the sound is back at most 5 seconds after the network. A watcher would be more code for a gain of a few seconds |
| A fault that waiting cannot mend ends the tries at once: an answer from 400 to 499 (but not 408 or 429), a file that is not audio, a faulty playlist file, a refused address | Try everything for 2 minutes | A dead address should give way to the next one of the station without a wait |
| A file of the phone keeps the few quick tries of Media3 | One policy for all | The 2 minutes are for the network |
| When the player gives up on an address, `StreamFailover` puts the next address of the same station into the item and prepares again. Each address is tried once per round. The item keeps its id and texts | A new item per address | The queue, the saved place and the screens do not change. After a restart the first address is tried first again |
| The addresses come from the stored stations (`StationStreams`), found by the address that plays | Carry the other addresses in the item | Nothing a caller sends reaches the player but one checked address. Every address from the store is checked again before use |
| A stream that fell behind its live window starts again at the live edge | Report the error | After a long pause that is what radio does |
| Now playing shows the reason and a "Try again" button in place of the seek bar. The button does what the play button does | A dialog | Simple, and the same on every screen size. The session reports the error of the player by itself |

## Consequences

- A station with two addresses and no network takes 2 minutes per address before the error shows.
- Between the error of one address and the start of the next, the error shows for the time of one read of the database.
- Media3 opens a live stream again once with no wait when it drops. Every try after that waits as the table says.
- The read by address scans the streams table. It has a few rows per station the user keeps.
- The reason texts are the same for a song and a station, apart from the network one.
- What needs a phone and a real network is in `docs/MANUAL-TESTS.md`.
