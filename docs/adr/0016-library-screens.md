# ADR 0016: Library screens

- Status: accepted, 20 Nov 2026.
- Rules: A7, A9, A12, U1, U5, U9, T2.

## Context

The Library tab shows the songs, albums, artists and folders of the phone. A library can hold 10,000 songs and more. Each list must open at once, scroll well and play on a tap, and the order of the songs must be kept.

## Decisions

| Decision | Options looked at | Reason |
|---|---|---|
| One screen with four lists under a row of tabs, and one ViewModel for it | One screen and one ViewModel per list, as first drawn in the folder tree | The four lists share the top bar, the scan and the change signal. One ViewModel reads each list once and keeps the place in a list while another one is shown |
| An album, an artist and a folder open as their own screen. The routes are inside the group of `LibraryRoute` | Routes next to the tab route | The bottom bar marks a tab when its route is in the chain of the open screen. Inside the group the Library tab stays marked, and coming back to the tab shows the screen that was open |
| The route of a detail screen carries the id and the texts of the top bar | Read the album or the artist again by id | The list that opens the screen has the texts already. The top bar shows at once, and the repository needs no function for one album or one artist |
| A long list is read 100 rows at a time by `PagedList`. The next page is asked for 20 rows before the end. After a change of the library the rows that are shown are read again, in reads of at most 500 rows | The Paging library; read the whole list | The repository gives pages already (ADR 0013). This needs about 60 lines and no new library. Reading 10,000 rows at once would hold the screen for a moment at every change |
| A row that is read a second time, because rows moved between two reads, is left out | Trust that pages never overlap | A list on screen must never hold the same key twice. It would crash |
| A tap plays the song with the whole list as the queue. The list is read again for this, in pages of 500, and the place of the song is looked up in it | Give the player only the rows the screen has read | The queue must be the whole list in the order shown, also when only the first 100 rows were read. The queue loader takes a long list in parts (ADR 0006) |
| Only the songs list has a choice of order: title, artist, album, newest first, shortest first. It is saved in the settings | A choice of order for every list | Albums and artists have one natural order, by name. More choices can come when someone needs them |
| The rows of a song, an album, an artist and a folder are in `core:ui` | Keep them in the library feature | Search and the playlists show the same rows. A song must look and behave the same in every list |
| The Library screen starts a scan when it opens | Wait for the next start of the app | Android tells nobody when the audio permission is allowed. The screen shows only with the permission, so this is the first moment a scan can work. A scan with no change writes nothing (ADR 0010) |
| The menu of a song has "Play next" and "Add to queue" | Also "Add to playlist" and the favourite | Those two come with playlists and favourites |

## Consequences

- A ViewModel holds the rows its screen has read so far. This is the window of a long list, not a second copy of the library: the database stays the one source, and every change of it reads the window again.
- The Library screen has one state for its four lists. A detail screen has its fixed texts from the route and one list, so it gives the list as it is.
- A read that fails does not end the screen. A list with nothing to show says so and offers to try again. A list that already shows rows keeps them, and the next change of the library reads again.
- A text that travels in a route is cut to 200 characters, and a second tap that comes while a screen already changes is dropped.

- A change of the library while a list is far scrolled reads up to as many rows as are shown. After a change the screen waits half a second before it reads again, so a scan of many files does not read the lists many times.
- A song that was removed between the read of the screen and the tap is played alone.
- An album row shows the music note tile. The picture of an album comes later (ADR 0015).
- The tab row scrolls. On a narrow phone the fourth tab is partly outside the screen until the row is moved.
- The checks that need a phone are in `docs/MANUAL-TESTS.md`.
