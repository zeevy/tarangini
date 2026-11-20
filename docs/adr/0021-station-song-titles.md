# ADR 0021: Song titles from a station

- Status: accepted, 20 Nov 2026.
- Rules: P1, S6, U8.

## Context

Many stations send the title of the song on air inside the stream (ICY metadata), and their own name in the answer to the first request. The title should show in the mini player, on now playing, in the notification, on the lock screen and in the car. The texts come from servers we do not know: they can be empty, only a dash, thousands of characters long or in a broken encoding.

## Decisions

| Decision | Options looked at | Reason |
|---|---|---|
| A listener of the player (`StationTitles`) writes the cleaned title into the item that plays, with `replaceMediaItem` | Let Media3 merge the title into the metadata of the player by itself | Media3 puts the texts of the item above the texts of the stream, so a station with a name would never show a song. The raw text would also go to the notification with no cleaning. An item that only changes its texts is replaced without a break in the sound |
| While a song title is known it is the title, and the name of the station is the artist line. With no song title the name of the station is the title | Station as title always, song under it | It is what a car display and a watch expect: title on top. The station is still on screen |
| The name of the station is the one the item came with (`MediaMetadata.station`). An item with no name takes the name the server sends, and with none the host of the address | Always the name the server sends | The user named the station. A server can send any name it likes |
| Only an item with an `http` or `https` address is touched, and a stream always has a title | Act on every metadata the player reports | A file of the phone reports metadata too, and its texts come from the library. A stream with no title of its own would show the text of the server as it came, uncut |
| A text is cleaned before it is used (`cleanStreamText`): control characters, the marks of a wrong encoding and the characters that turn the direction of the text around are taken out, white space becomes one space, a text with no letter or digit counts as none, and at most 200 characters are kept | Show what comes | A title of 5,000 characters must not reach the notification or the car, and " - " between two songs is no title |
| The same title sent again changes nothing | Replace the item each time | A station repeats the title every few seconds. Each replace makes the player send its queue to every listener |
| A queue item says that it is a stream (`PlaybackItem.isStream`). A stream with no artist shows "Live" where a song shows "Unknown artist" | Show "Unknown artist" | A station that sends no titles has no artist, and that is not something missing |

## Consequences

- Media3 reads the stream text as UTF-8 and, when that fails, as Latin-1. A station that uses another encoding shows wrong letters. The marks of a failed decode are removed.
- The station name of the user reaches the item when stations are stored. Until then the name comes from the server, or is the host of the address.
- The name the server sends is read from the metadata of the player. The tests cannot make a real server answer reach the player, so that path is a check on a phone.
- The song title is not saved. After a restart the item shows the name of the station until the next title comes.
- The checks that need a phone are in `docs/MANUAL-TESTS.md`.
