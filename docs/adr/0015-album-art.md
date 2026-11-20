# ADR 0015: Album art

- Status: accepted, 20 Nov 2026.
- Rules: A7, A12, U5.

## Context

A cover picture shows in lists, the mini player, the now playing screen, the notification and the lock screen. It must be the same picture everywhere, and a list of thousands of songs must still scroll well.

## Decisions

| Decision | Options looked at | Reason |
|---|---|---|
| The picture of a local song is asked for at the address of the song plus `/albumart`, for example `content://media/external/audio/media/42/albumart`. `artworkUriOf` in `core:domain` makes it | The album art table of MediaStore; reading the picture out of the file ourselves; a column in our database | The phone gives the picture inside the file there, already made small, and stores it itself. It needs no MediaStore album id, which we do not keep, and no change of the database. Checked on Android 17: a JPEG for a file with a picture, an error for a file with none |
| The address says where to ask, not that a picture exists | Find out at scan time which files have a picture | Finding out means opening every file. The loader shows the tile when the phone gives an error, and that costs nothing at scan time |
| Coil 3 (Apache-2.0) loads the pictures in the screens, through `AlbumArt` in `core:ui`. Only `core:ui` depends on Coil | Glide; our own loader | Coil is made for Compose, loads in the size of the place it is shown in, and is small. A screen never names Coil, so it can be replaced in one module |
| Decoded pictures are kept in memory, in at most 15% of the memory of the app. Nothing is kept on disk by Coil | A disk cache | Every picture comes from the phone, which keeps its small pictures on disk already. A second copy would only use space. A disk cache comes with the first picture from the network |
| While a picture loads and when there is none, `AlbumArt` shows a music note on a plain tile | An empty space; a spinner | A row looks the same with and without a picture, and nothing jumps when the picture comes |
| Every item of the queue is made by `playableItem` in `core:playback/mapper`, which adds the picture address. An address a caller sent is dropped as before (ADR 0004) | Set the picture only on items that come from the library | The saved queue and an item another app asks for are made from an address alone. With one maker they all get the picture, and the three copies of the same builder are gone |

## Consequences

- The notification and the lock screen load the picture through Media3 from the same address as the app. A song with no picture has no picture there, which is the normal look of a media notification. The note tile is for our own screens.
- Another app that reads the session, such as a car, gets an address it may not be able to open. Pictures for the car are a later issue.
- An album has no picture address yet. It comes with the albums screen, which can take the picture of the first song of the album.
- A song with no picture is asked for again each time its row comes back on screen, because a failed load is not kept. If the scroll check on a phone shows a cost, the loader gets a short memory of the addresses that failed.
- The picture is asked for only at the exact address of one audio file. An address with anything more in it gets no picture.
- Radio stations have no picture address. They show letter tiles, which come with the radio screens.
- The checks that need a phone are in `docs/MANUAL-TESTS.md`.
