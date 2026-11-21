# ADR 0043: Genres

- Status: accepted, 21 Nov 2026.
- Rules: A9, D1, P1.

## Context

People want to browse the library by the genre tag, on the phone and in the car. A file can name more than one genre, and many files name none.

## Decisions

| Decision | Options looked at | Reason |
|---|---|---|
| Two tables: `genres` (name, sort form of the name, unique) and `song_genres` (song, genre) | A genre text column on the song. The genre lists of MediaStore, read live | A song under each of its genres needs a join. The lists stay one query with a limit, and the car and the phone read the same tables as for every other list |
| The scan reads the `GENRE` column of MediaStore with the other tags and saves the genres of a song in the transaction that saves the song | A second pass over the genre tables of MediaStore | One read of MediaStore, and a song never has new tags with old genres |
| A tag is divided at ";", "," and the zero character of the newer tag format. "/" does not divide. At most 5 genres per song, 100 characters each | Divide at "/" too | "R&B/Soul" is one genre. The limits keep a faulty tag from making hundreds of rows |
| Two names with the same sort form are one genre ("Rock" and "rock") | Keep them apart | It is the rule artists follow |
| A song with no genre tag gets the genre with the empty name, shown as "Unknown" and listed last | No row for such a song | Every song is under a genre, and the list needs no special query |
| Database version 3 adds the two tables empty. The scan treats a song with no genre row as changed, so the first scan after the update reads every tag again, once | Fill the tables in the migration. Change a column of every song in the migration | The migration cannot read MediaStore, and it changes nothing that exists. The mark "not read yet" needs no column: it is the missing row |
| A genre with no song left is removed when songs are removed for good or tags change | Keep empty genres | The list shows only what can be played. A genre whose songs are all gone for now is not listed, by the query |
| Genres is the fourth list of the Library tab and a node under Library in the car. A genre opens its songs by title, and a tap plays them as the queue | A tab of its own in the car | The car shows four tabs at most, and they are taken |
| `GenreRepository` is its own interface in `core:domain` | Three more functions on `LibraryRepository` | That interface is at the size limit, and the genre lists are read by two screens and the car only |

## What follows

- The first scan after the update to version 3 saves every song again. On a large library that takes as long as a first scan. Favourites, playlists and play counts are not touched: a song is updated, never replaced.
- The car shows at most 500 genres, as for every list. No letter nodes: no library has that many genres.
- Search does not look at genres.
- The checks that need a real phone and a head unit are rows 127 and 128 of `docs/MANUAL-TESTS.md`.
