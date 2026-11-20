# ADR 0032: Playlists

- Status: accepted, 21 Nov 2026.
- Rules: A9, U4.

## Context

A person makes their own lists of songs. The tables `playlists` and `playlist_items` exist since the first schema: a place has its own id, a position and the song, and the same song can be in a playlist more than once. Import and export of `.m3u` files and smart playlists are not part of this.

## Decisions

| Decision | Options looked at | Reason |
|---|---|---|
| Playlists are a fifth list of the Library tab, with "New playlist" as its first row. A playlist opens as its own screen inside the tab | A sixth entry in the bottom bar. A screen behind Settings | The bottom bar is full for a narrow phone, and a playlist is a part of the library like an album |
| Everything goes through `PlaylistRepository` from `core:domain`. Its `changes()` fires after every change the app made | Screens watch a database flow | One change signal for the list of playlists, an open playlist and the car, with no long list held in a flow |
| A name is trimmed, must not be empty and has at most 100 characters (`checkPlaylistName`). Two playlists can have the same name | Unique names | The check is in the dialog and again in the repository. A second "Drive" does no harm |
| "Add to playlist" is in the menu of every song row and is a button on now playing. The dialog lists the playlists and has a field that makes a new one with the song in it | A separate screen to pick a playlist | One tap for the common case, and the first playlist can be made right there |
| A song is added at the end, in one statement that adds nothing when the song or the playlist is gone | Insert and catch the foreign key error | The song can leave the library while the dialog is open |
| A place moves from its menu: up, down, to the top, to the end. A move writes the new order of the whole playlist in one transaction | Drag with a handle. Fractional positions | A menu works with a screen reader and with one thumb, and needs no second copy of the drag code of the queue. 5,000 places are written in well under a second, which the test shows |
| A song whose file is gone keeps its place and is left out of the playlist and of its song count until the file is back. When the library removes the song for good, its places go with it | Show it greyed out. Remove the place at once | A memory card that is put back brings the playlist back whole. A row that cannot play only confuses |
| Deleting a playlist asks first and says that the songs stay in the library | Undo in a bar at the bottom | A playlist cannot be brought back |
| "Play" plays the playlist in its order, a tap on a song plays from that song on, and "Shuffle" plays it in a new order each time | Turn on the shuffle mode of the player | The shuffle switch of the player stays as the person left it |
| The car shows Playlists as a fifth list under Library (`MediaId.Playlists`, `MediaId.Playlist`). A playlist lists its songs and a tap plays them from that song on. The car cannot change a playlist | A tab of its own | The three tabs are in use, and the driver only plays |
| After a playlist changed the car is told about the list of playlists and the open playlists only | Tell every open list | The list the driver looks at must not be read again for it |

## What follows

- In the car a song that is in a playlist twice is one row, because the car tells the rows of a list apart by the song.
- The car shows at most 500 playlists and at most 500 songs of a playlist, as for every other list.
- A move from the menu works among the rows the screen has read so far.
- Import and export of `.m3u` files come later as their own piece of work.
