# ADR 0010: Scanning the phone into the library

- Status: accepted, 20 Nov 2026.
- Rules: A8, D3, D7, K3, K6.

## Context

Every list, the search and the car read the library in Room. The library must be a true copy of the audio files MediaStore knows, and a new scan must not cost the user a favourite, a playlist place or a play count.

## Decisions

| Decision | Options looked at | Reason |
|---|---|---|
| A scan reads the whole file list once, compares it with the stored songs in memory (`planScan`) and writes only the difference | Save every file on every scan; ask MediaStore only for files changed since the last scan | A scan with no change writes nothing, so it is cheap enough to run often. Asking only for changed files cannot see a file that was removed |
| A file is "changed" when its change time or its size differs from the stored one | Compare every tag | Both numbers are in the one list query. An edit of the tags changes the change time |
| Songs are saved 500 in one transaction, with the ids of artists, albums and folders kept in memory for the scan | One transaction for each song; one transaction for the whole scan | One transaction for each row is many times slower. One for the whole scan shows no progress and loses everything when the app is stopped |
| A song whose file is gone is marked and leaves the lists. After 30 days it is removed for good | Remove at once; keep for ever | A memory card that is taken out for a trip comes back with nothing lost. A row kept for ever would hold favourites of files that are deleted |
| A new file in the folder, with the name and the size, of exactly one song that is gone is taken as that song, and the song gets the new file id | Trust the id of MediaStore alone; match by name and size in any folder | MediaStore can give a file a new id after a memory card is put back. Without the folder, a file of another song with the same name and size would take over the favourite and the playlist places. With two candidates the scanner does not guess |
| A song that the settings leave out (too short, or in an excluded folder) leaves the lists but is never removed for good while its file is on the phone. It gets its own mark, `HIDDEN_SINCE`, in `missing_since`. When its file goes away later, the 30 days start then | Treat it like a file that is gone; a new column | A changed setting must not cost a favourite or a playlist place 30 days later. The mark is a value of the column that is there, so the schema does not change |
| Text from a file (tags, names, folders) is cut to 512 characters, a file name to 255, and control characters are removed, in the one place that reads MediaStore | Store what the file says | A file is written by anyone. One tag of a megabyte would break every list that reads the row (D7) |
| An album belongs to the album artist tag, or to the artist of the song when that tag is missing | The artist of the song always | A collection by many artists stays one album |
| A file with no title tag shows its file name without the ending. No artist or album tag stores the empty name, and a screen shows its own "Unknown" text | Store "Unknown artist" in the row | The text of a screen is translated; a stored word is not. This is the rule of the database design |
| A top folder is the storage name of MediaStore (`external_primary`, or the id of a memory card). A screen gives it a readable name | Leave the storage out of the path | Two memory areas can hold a folder of the same name |
| The formats are chosen by the file ending (`.mp3`, `.aac`, `.m4a`) | The `IS_MUSIC` flag of MediaStore; the MIME type | The app plays exactly these three. The flag also covers formats the app does not play, and the MIME type of `.aac` differs between phones |
| The scanner is `LibraryScanner` in the domain, with its state as a `StateFlow` and a result that is a value. `AudioFileSource` in `core:data/mediastore` hides MediaStore | Let a ViewModel call a repository function | The scan is started from more than one place later (the library screen, the rescan on a file change). Tests give the scanner a list of files |
| The file list is read on the dispatcher injected as `@IoDispatcher` | `Dispatchers.IO` in the class | A test replaces it (K3). This is the first injected dispatcher of the app |

## Result

- `DefaultLibraryScanner` is the only writer of the library tables, and `MediaStoreAudioSource` is the only class that queries MediaStore.
- A scan that finds no permission, or gets no answer from MediaStore, fails with a reason and changes nothing. No answer is never read as "all files are gone".
- The settings `min_track_seconds`, `included_folders` and `excluded_folders` are applied before the comparison. A folder in the settings is a whole path such as `external_primary/WhatsApp`, and it covers the folders inside it. An entry that names no folder is ignored.
- A scan that stops with an error or is cancelled leaves the batches it has saved, and the state goes back to idle. The next scan carries on from what is stored.
- Unused albums, artists and folders are removed only by a scan that changed or removed a song, in the same transaction as the removal. A scan that is stopped after it saved changed songs can leave an unused album or artist row until the next such scan. No list shows a row without songs.
- Nothing starts a scan yet. The library screen and the rescan on a file change do that.

## Open points

- The time of a first scan of 10,000 songs on a mid range phone is not measured yet (manual test 19). On the build machine the scanner saves 10,000 songs in about 2 seconds, without the read from MediaStore.
- Whether MediaStore gives a file a new id after a memory card is put back is not checked on a phone (manual test 20).
- A file with no known length is kept, whatever the minimum length is.
- A file list that comes back empty while MediaStore is still building its index marks every song as gone. The next scan brings them back with nothing lost, because a song is kept for 30 days.
- When MediaStore is built new, it can give the id of one file to another file. The scanner then reads the other file as a change of the first song, and a favourite of the first song points to the other file. A check that tells this from a renamed file is not built.
- A file that got a new id and a new size at the same time is a new song; the old one goes after 30 days.
- The number of files is not limited. The design value is 100,000 songs; the length limit on text keeps one row small.
