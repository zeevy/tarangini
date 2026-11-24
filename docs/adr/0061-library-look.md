# ADR 0061: The library in the new look

- Status: accepted, 24 Nov 2026. Changes ADR 0015 and ADR 0016.
- Rules: U9.
- Decision D93.

## Context

The player, the mini player and the bars had the look of D90, while the library, Favourites, search and the screens of an album, an artist, a folder, a genre and a playlist still had Material top bars, tabs and plain list rows. This change brings them to the same look.

## Decisions

- A screen of the library has a plain header on the ground (`ScreenHeader` in `core:ui`): the title, a back arrow on a detail screen, and its buttons. The list sits on its own list card (`ListCard`) with the size of the list and its tools on the top line.
- The Library picks its list with a row of chips with lamps in place of tabs. The header has Search and Settings. The songs list has Shuffle all and Sort on its card, the playlists list has New playlist and Import.
- The sizes of the songs, albums and artists lists come from the first letters the car already counts (`LibraryLetters`), so no new query is needed. A list that has no count of its own shows the rows read so far, with a plus while more can come.
- Every row is a `LookRow`: a tile at the start, the title with a line under it, a short value in digits at the end and a button after it. A song row shows the artist with the album and the length, and is lit while that song plays (`SongRowActions.playing`).
- A song or album with no picture shows a colour tile with the first letters of the album, in place of the music note. The colour comes from the name, so an album has the same tile everywhere. The letters are decoration and are not read out.
- The albums are a grid of tiles. An album screen has a card with the cover, the name, the artist, the length and the Play and Shuffle buttons.
- The heart on a favourite song is a button that takes the song out of Favourites.
- Search has its field on the dark display and its results on a list card with titles in capitals.
- Recent searches of the design are not built: the app keeps no search history.

## Results

- Every screen of the app now has one look.
- The rows are taller and show more, so a few more tests scroll before a tap.
