# ADR 0045: Lyrics from the tags of the file

- Status: accepted, 21 Nov 2026.
- Rules: A9, S1.

## Context

Many music files carry the text of the song in their tags. The now playing screen can show it. Nothing is fetched from the network: the app has no lyrics service.

## Decisions

| Decision | Options looked at | Reason |
|---|---|---|
| The tags are read with the readers of Media3 that play the file (`Mp3Extractor`, `Mp4Extractor`) | A tag library. `MediaMetadataRetriever` of Android | No new library. Android gives no lyrics. A tag is understood the same way as in the player |
| The file is read while the now playing screen shows the song, once per song, and the text is kept nowhere | Read the lyrics in the library scan and save them in the database | The scan reads MediaStore only and opens no file. Lyrics of every song would make the database large for a text that is read for one song at a time |
| The reading ends when the reader tells the format of the track. An M4A file with its tags at the end is opened once more at that place. The reader is told the length of the file | Read the whole file | The tags are part of the format, so the sound is not read. For an MP3 file the whole tag is read, with its cover picture. With the length a tag that states to be longer than the file is refused before room is made for it |
| Only a MediaStore audio item is opened | Any address of the item that plays | The same rule the player has. A radio stream has no lyrics here |
| MP3: the `USLT` frame, taken apart here in its four encodings. M4A: the lyrics item, which Media3 gives as text | Also synced lyrics (`SYLT`) | Synced lyrics were looked at and left out for now |
| The text is cleaned before it is shown: one kind of line break, no control character, no character that turns the direction of the text (the joiners of the Indian scripts stay), 50,000 characters at most | Show the tag as it is | A tag is text of unknown origin |
| A line "Lyrics" in the menu of the now playing top bar, only for a song with lyrics. It opens a dialog with the text, which scrolls | A button below the player buttons. An icon in the top bar | Below the buttons it is out of view on a small screen. The top bar has no room for a fourth icon at 320 dp |
| Not in Android Auto | A lyrics list in the car | A driver must not read |

## What follows

- A file with lyrics in another frame (a comment, a custom text frame) shows none.
- An `.aac` file has no tags the readers give, so it shows none.
- The lyrics show a moment after the screen shows the song, when the file was read. A new song closes open lyrics.
- Nothing is read while the screen is closed.
- The checks on a real phone are row 132 of `docs/MANUAL-TESTS.md`.
