# ADR 0044: Backup and restore to a file

- Status: accepted, 21 Nov 2026.
- Rules: A9, S1, D1.

## Context

Favourites, playlists and the settings of the library live only on the phone. A person who changes the phone or installs the app again loses them. The app has no server, so the copy goes into a file the person keeps.

## Decisions

| Decision | Options looked at | Reason |
|---|---|---|
| One JSON file with the name `tarangini-backup` and a version number, written and read through the file picker of the system | A copy of the database file. A zip with several files | The database holds ids of this phone and the whole library index, which the next phone builds itself. One readable file is easy to check and to keep. The picker needs no storage permission |
| A song is named by the path of its file as the library saves it: storage, folders, file name | The id of the song. Title and artist | An id means nothing on another phone. The path is the same when the music was copied over as it was |
| A song of the file is found with the rule playlist files use (`M3u.match`): the most folders in common from the file name up, or the one file with that name | An exact path only | A memory card has another name on the next phone, and people move a music folder. A guess between two files is never made |
| The file holds the favourites in the order they were marked, each playlist with its songs in order, the hidden folders, the shortest track and the order of the songs list | Also the play history and the saved queue | The issue asks for favourites, playlists and settings. The history is large and of less worth |
| Two ways to restore, asked each time: "Add" keeps what is here, "Replace" also removes the favourites, playlists and hidden folders the file does not hold | One way only | Both are wanted: bring two phones together, or make this phone like the backup |
| With "Add" a playlist whose name is here already is left as it is | Make it again. Join the two | The same file read twice must not double every playlist. Joining would change a playlist the person made here |
| With "Replace" the things of the file are added first and the rest is removed last | Remove first | A failure on the way then leaves more than was asked for, never less than was there |
| The file is not trusted. At most 5 MiB are read. Every list has a limit (50,000 favourites, 500 playlists, 5,000 songs each and 100,000 in all, 1,000 folders), every text is cut and loses its control characters, a value outside its range is left out. A file of a newer version is refused | Trust a file the app wrote | The person can pick any file, and a file can be made to do harm |
| A backup with songs is refused while the library is empty | Read it anyway | Before the first scan no song can be found, and every favourite of the file would be dropped without a word |
| A hidden folder of the file is hidden only when the library knows the folder | Hide every path of the file | Settings offers the same folders. A made up file can so not hide a whole storage, and no path is saved that nobody can find again in Settings |
| A song whose file is gone for now (a card that is out, a hidden folder) is written to the backup and found on a restore | Leave it out as the lists of the app do | "Replace" on the same phone would take away its mark and its place in a playlist for good |
| The favourites are written in one database step, with times that keep the order of the file | One write per song | 50,000 single writes are slow, and each one makes every screen read the favourites again |
| One backup or restore at a time; the rows say "Working" meanwhile | Let a second one start | Two restores side by side would make every playlist twice, or remove what the other made |
| After a restore the library is scanned | Leave it to the next start | The settings of the file can hide or show songs |

## What follows

- A favourite or a playlist song whose file is not on the phone is not restored. The result says how many songs were not found.
- A file with very many songs that are not in the library, in a library with very many files of the same name, takes long to compare. The limits keep it bounded, and a closed screen ends it.
- The restore is not one database transaction: it goes through the repositories, so the screens hear of each change. The order above keeps a failure from losing anything.
- A later version of the file gets a higher number. This app reads version 1 only.
- The checks on a real phone are rows 130 and 131 of `docs/MANUAL-TESTS.md`.
