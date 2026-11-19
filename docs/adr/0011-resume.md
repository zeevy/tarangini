# ADR 0011: Coming back to what was playing

- Status: accepted, 20 Nov 2026.
- Rules: A8, D3, P1, S1.

## Context

The app must come back with the same queue, item and position after it was closed or killed. A play button on a Bluetooth device must start the last item when the app is not running. The tables `queue_items` and `resume_state` are there since the first schema (ADR 0003).

## Decisions

| Decision | Options looked at | Reason |
|---|---|---|
| The playback service saves and restores, through `ResumeStore` from `core:domain`. `RoomResumeStore` in `core:data` keeps it in Room | Save from the screens; save in DataStore | The service plays also when no screen is open, and it is the only place that sees every change. Playback and data do not depend on each other, so the interface is in the domain |
| The place (item, position, shuffle, repeat) is saved on a pause or a start, a change of item, a seek and every 5 seconds while something plays | Only on a pause; every second | A killed app loses at most 5 seconds. One small row is written |
| The queue is saved when it changed and has been still for 1 second. The queue and the place go in one transaction | Save on every change | A long list comes into the player in parts (ADR 0006). Each part would write the whole queue again |
| A queue that came back is not written again while it is the same | Write it on every start | A start writes nothing, also with 100,000 songs |
| Saves run one after the other in one coroutine | A new coroutine for each save | A slow save of the queue can never land on a newer place |
| On start the service puts the saved queue into the empty player, paused and not prepared | Prepare it too | A prepared player shows the media notification. Opening the app must show and sound nothing new. A play request prepares the player |
| Saving starts only after the read of the saved queue ended | Save from the start | The empty player of a new service must never wipe what is stored |
| A request to go on with an empty player (`onPlaybackResumption`) gets the saved queue from the database. `MediaButtonReceiver` is in the manifest, so a media button starts the service when the app is not running | Take the items the caller names | Nothing a caller sends decides what plays (ADR 0004). The items come only from our own database |
| The saved queue goes into the player in one call | In parts, like a list from a screen | Media3 takes the items of a resumption only as one list. Nothing waits to sound when the app opens, and the list is made in the same process. ADR 0006 is about lists from a screen, where the first sound must not wait |
| The player knows a song by its MediaStore address; the database stores the song row | Store the address | A song that is removed takes its queue rows with it, and a song whose file is gone is left out by the same read as everywhere |
| When the file of the saved item is gone, the next item that is there is current, from its start. With none after it, the queue starts at the top | Give up the whole queue | The rest of the queue is still good |
| While something plays, an item whose file is gone is skipped (`SkipMissingFile`). It stops after one round of the queue | Stop with an error | A deleted file in a long queue must not end the music. Other errors still stop the player and are shown |

## Consequences

- An item that is not a song of the library is not saved. A station is not saved yet, because the app cannot play one. It goes into `resume_state.station_id` together with radio playback.
- The order that shuffle made is not saved, only that shuffle was on. After a restart the same queue is shuffled in a new order.
- The system media controls after a reboot are not part of this.

## Open points

- The checks on a real phone are in `docs/MANUAL-TESTS.md`, section "Playback: resume": a force stop, and the play button of a Bluetooth device while the app is not running.
