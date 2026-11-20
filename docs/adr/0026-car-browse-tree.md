# ADR 0026: The list the car shows

- Status: accepted, 20 Nov 2026.
- Rules: P2, P3, P4, P5, P8, A9, S2.

## Context

Android Auto shows a media app as tabs with lists under them. It draws the screens itself; the app only gives the tabs, the rows and what a tap plays. The car has no paging and shows only a limited number of rows, and Google gives no number for that limit (section 4 of [FEATURES.md](../FEATURES.md)). A driver also cannot scroll through hundreds of rows. So the list for the car cannot be the phone lists as they are.

Radio is out of the first release (D48), and favourites, recently played and playlists are not built yet.

## Decisions

| Decision | Options looked at | Reason |
|---|---|---|
| The top has one tab for now, Library, with Songs, Albums, Artists and Folders under it. Favourites and Recent become tabs when they are built. The tab limit the car gives is read and the tabs after it are left out | Songs, Albums, Artists and Folders as four tabs | The plan has Library, Favourites and Recent as tabs, because the last two are what a driver wants first. Four library tabs now would have to be taken away again |
| A list by name (songs, albums, artists) with more than 100 rows is shown as one node per first letter. Digits, signs and empty names share one node, "#" | One long list; groups of a fixed size ("1 to 100"); recent and most played only | A long list is cut by the car at a place nobody knows, and cannot be scrolled while driving. A letter is something a driver can aim for. The play counts do not exist yet |
| A node never gives more than 500 rows, the most one read of the library gives | Read page after page into one answer | 500 rows is already more than a driver can use. What is after them is found by voice or search |
| The letters come from the database: one query per list that groups the sort key by its first character. The counts added up give the place of a letter, so its rows are read with the paged read the phone uses | Work out the letters in the playback module from the names | The order of a list is the order of the sort key. Only the database can group by the same key, so a letter node always holds exactly the rows the list has at that place |
| An artist opens the songs of the artist, not the albums | Artist, then albums, then songs | One level less. With the letter level a song is at most four taps below a tab |
| A folder shows the folders in it, then its songs | Folders and songs in two nodes | It is what the folder holds on the phone |
| The id of a node is made and read only by the `MediaId` codec: a short text like `album.12`. Only the forms the app makes are read back; a number is only digits | Pass the database id or the address of the file | The car is another app, and what it sends back is input. An id that is not one of the forms gives the error "bad value" and nothing else |
| The id of a song in the car names the list it was shown in (`song.345@album.12`). A tap plays that list from the song on. The queue is read from the library again at that moment | Play only the tapped song; keep the lists the car has read in memory | A driver expects the album to go on. Reading again needs no memory of what each car was shown, and a song that has left the list still plays, alone |
| An item that names an address is checked as an address, whatever its id says | Trust the id first | The check of what may be played ([ADR 0004](0004-playback-service-and-session-access.md)) must not have a way around it |
| After a change of the library the car is told that the lists it has read changed, one second after the last change. The last 50 lists are remembered | Tell on every change; tell every possible list | A scan changes the library many times in a row. The car reads again only what it has open |
| The car asks for a whole list at once and gets it. A caller that asks for a page gets that page | Always give the whole list | Media3 refuses an answer with more rows than the page that was asked for, and the refusal would stop the service |
| A song is played from the car only by a tap, which sets the queue. A request to add a song to the queue by its car id is refused | Take the id there too | The car does not send such a request. One way in is easier to keep safe than two |
| A read that fails gives the car an error result. Nothing a caller sends can crash the service | Let the failure through | The same service plays the music |

## Result

- The car shows the library of the phone. A tap on a song plays its list from that song on.
- Album art in the lists, error texts for the car (for example when the app may not read the audio files), search and voice play come with their own changes.
- The lists a caller gets are the same for this app and for the car. Who may ask at all is decided at the connection ([ADR 0025](0025-android-auto-setup.md)).
- Rule P8 asks for a run on the Desktop Head Unit before this is merged. The checks are rows 69 to 76 of [MANUAL-TESTS.md](../MANUAL-TESTS.md).
