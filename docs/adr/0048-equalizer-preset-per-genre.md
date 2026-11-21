# ADR 0048: Equalizer preset per genre

- Status: accepted, 21 Nov 2026.
- Rules: A9, P1.
- Decision D77. It builds on ADR 0047.

## Context

A person who listens to different kinds of music changes the preset by hand again and again. The app remembers a preset for each genre and switches by itself when a song starts. The switch must be at the start of the song, with no click and no gap, also in gapless playback.

## Decisions

| Decision | Options looked at | Reason |
|---|---|---|
| The audio renderer tells the equalizer when the first sound of a song goes to the sink (`onOutputStreamOffsetUsChanged` of Media3, between the last buffer of one song and the first of the next). The first song of a new queue, or after a skip, plays at once: Media3 tells of it while the stream is handed over, or not at all when its offset is the same as before, so the renderer tells of that song itself. It finds the song by the timeline of the player (`StreamSongs` keeps the song of each stream it read); a song it cannot find plays as one with no genre | Switch on `onMediaItemTransition` of the player. Use the stream data Media3 hands to processors on a flush | The player tells of a new item when its sound is heard, about a quarter of a second after it went through the processors, so the start of a song would get the old preset. The sink hands stream data to its processors only when it sets them up again, which does not happen between two gapless songs of the same format |
| Each queue item carries the genre keys of its song in an extra of its texts, added by `LibraryTexts` where it adds the title and the artist | Read the genres from the database when a song starts | The renderer runs on the playback thread, which must never wait for the database. The item already holds what comes from the library |
| A genre is known by its key, the sort form of its name (`Genre.key`) | The genre id. The name as the tags write it | The id can change when a genre is removed and found again by a rescan. "Rock" and "rock" are one genre with one key |
| A song of several genres takes the preset of the first genre, by key, that has one. A song with no genre tag, and a radio stream, take the main setting | Ask the person to choose an order | Rare, and the order is stable |
| The bass boost is the same for every song; only the preset changes | A bass boost per genre | Fewer settings. The bass boost depends on the headphones more than on the music |
| The presets per genre work only while the equalizer is on, and one switch turns them all off. A change by hand on the screen changes the main setting | Turn the equalizer on for a genre with a preset | The equalizer switch stays the one way to hear the file as it is |
| The switch uses the same 20 ms fade as a change on the screen | Switch at once at the first sample | A switch at once makes a click when the songs run into each other |

## What follows

- The list shows at most 500 genres, the most one page of the library gives.
- A song added to the queue before its genre was read by a scan has no genre keys until it is added again.
- The switch on a real phone, with the gapless test albums, is row 140 of `docs/MANUAL-TESTS.md`; rows 141 and 142 cover a restart, a rescan and the car.
