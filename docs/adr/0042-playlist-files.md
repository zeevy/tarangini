# ADR 0042: Playlist files

- Status: accepted, 21 Nov 2026.
- Rules: A9, S1.

## Context

People have playlists from other players and want to take the playlists of this app along. The common format is `.m3u` (and `.m3u8`, the same in UTF-8): one path per line, with optional comment lines.

## Decisions

| Decision | Options looked at | Reason |
|---|---|---|
| Files are read and written through the file picker of the system (`OpenDocument`, `CreateDocument`) | A fixed folder on the storage. A share target | The picker needs no permission, and the person sees where the file goes |
| Only a `content:` address is opened | Open any address | The only address that reaches this code is the result of the picker. The check is a second guard: a `file:` address could name a file of the app itself |
| A file over 1 MB is refused before a line is looked at. A line over 4096 characters is left out, and at most 5000 lines are used | No limits | The file is not trusted. A playlist of 5000 songs is about half the size limit |
| A line is only ever compared with the files the library knows. Nothing from the file is opened, fetched or played | Add an unknown path or address as a new item | A file cannot bring anything into the app but an order of songs that are already there |
| Matching: the song whose path has the most folders in common with the line, counted from the file name up. A path of a Windows computer and a relative path match that way. With only the file name in common, the song is taken when it is the one song with that name | Exact path only. First song with the name | Paths differ between devices. A guess between two songs with the same name would put a wrong song in |
| The import always makes a new playlist with the name of the file. With no song found, no playlist is made | Fill an open playlist | Nothing that exists is changed by a file |
| The result says how many lines were found and shows up to 50 lines that were not | A count only | The person can see which songs to copy to the phone |
| The export writes the extended form (`#EXTM3U`, `#EXTINF`) with the path other players on a phone use (`/storage/emulated/0/...`, a card as `/storage/<its code>/...`). A song whose file is gone is left out | Relative paths | Absolute paths are what players on Android write. The import of this app matches both |
| The matching and the format are plain code in `core:domain` (`M3u`). Reading and writing the picked file is `PlaylistFiles` (`DefaultPlaylistFiles` in `core:data`) | All in the data layer | The rules are tested without Android |

## What follows

- A file name is compared without regard to case for the letters a to z only, which is what the database can do. A name with other letters must be written in the file as it is on the phone.
- A file that is not UTF-8 is read as the Western code page (ISO 8859-1). A line that starts with `#` is a comment, as the format says, also when it was meant as a file name.
- An export of a playlist with no file left on the phone writes nothing and says it could not be saved.
- Stream addresses in a file are listed as not found: the app has no radio in this release.
- The checks that need a real phone are rows 124 and 125 of `docs/MANUAL-TESTS.md`.
