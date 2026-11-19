# ADR 0003: The Room database, version 1

- Status: accepted. Decided by dev in auto mode, 19 Nov 2026.
- Rules: D1 to D4, D7, D8, H8, B1.
- The full design is in [docs/DATABASE.md](../DATABASE.md).

## Decisions

| Decision | Options looked at | Reason |
|---|---|---|
| Room 3 (`androidx.room3`) | Room 2 | Room 3 is the latest stable line (B1). Starting on the older line would mean a move later |
| The SQLite that ships with Android | The SQLite bundled with Room | No native library in the app, and the platform patches it. The driver can be changed in one line if a newer SQLite is needed |
| Full text search with FTS4 and the `unicode61` tokenizer, as its own table that the library DAO writes | A table tied to `songs` by Room; FTS5 | The search covers artist and album names, which are not columns of `songs`. Room has no FTS5 entity |
| A song whose file is gone is marked, not removed | Remove the row at once | Removing the row would remove its favourite, playlist and play count rows. A memory card that is taken out for a day would cost the user their lists |
| One play count row per song and per station | A log with one row per play | Gives "recently played" and "most played" and cannot grow without limit |
| Artists and albums are found by their name key | MediaStore artist and album ids; the exact name | Songs from another source later join the same artists and albums. "ABBA" and "Abba" are one artist; merging them later would mean moving songs between rows |
| A stored sort key next to each name | SQLite text ordering rules | The same order on every phone, with an index, and a function that tests can check |
| Lists take a limit and an offset | The Paging library now | No screen exists yet. The library screens issue picks the paging tool |
| Settings in DataStore, queue and resume state in Room | Everything in DataStore | The queue points to songs, so it belongs with them and goes when a song goes |

## Result

- New libraries: Room 3, `androidx.sqlite` framework driver, DataStore Preferences, kotlinx coroutines. All Apache-2.0.
- Robolectric is now a test library of every Android module, not only of Compose modules.
- CI has one more step: the committed schema files must match the entities.
