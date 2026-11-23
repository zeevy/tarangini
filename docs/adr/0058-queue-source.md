# ADR 0058: The list a queue came from

- Status: accepted, 24 Nov 2026. Adds to ADR 0006, ADR 0011 and ADR 0055.
- Decision D89.

## Context

The now playing top bar showed "Now playing" and the album of the song. When a queue starts from a playlist or an artist, the person cannot see that, and after a restart nothing says where the songs came from.

## Decisions

`QueueSource` in `core:domain` holds the kind (album, artist, playlist, genre, folder, favourites) and the name, cut to 200 characters. `PlaybackController.play` takes it, and `QueueLoader` writes it into the playlist metadata of the player: the title is the name, and an extra holds the kind. `PlaybackState.source` reads it back, so the phone, the widget and the car all see the same value.

- The library screens pass their list. A song played alone, Home and search pass none.
- `PlaybackSessionCallback.onSetMediaItems` clears it for a queue the car, a spoken request or another app starts.
- Adding songs to the end or as next takes it away, as the queue is no longer that list.
- `resume_state` has two new columns, `source_kind` and `source_name` (version 4). A kind that an older or newer version does not know is read as no source.

## Results

- The top bar says "Playing from album" and the name, or "Now playing" and the album when there is no source.
- Metadata from another app is never shown as a source: without the extra of this app it reads as none.
