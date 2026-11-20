# ADR 0020: Radio streams

- Status: accepted, 20 Nov 2026.
- Rules: P1, P2, P6, S4, S5, S6.
- Changes [ADR 0004](0004-playback-service-and-session-access.md): a caller may now also ask for an `http` or `https` stream.

## Context

A station is a name and an address that the user types in. The address can be an Icecast or Shoutcast stream (MP3 or AAC), an HLS playlist (`.m3u8`), or a small playlist file (`.pls`, `.m3u`) that only says where the stream is. Many stations have no `https` address. The address and everything behind it come from servers we do not know.

## Decisions

| Decision | Options looked at | Reason |
|---|---|---|
| Streams play through the one player of the app. Its data source reads `http` and `https` with the Media3 OkHttp data source, and everything else as before | A second player for radio | One player means one session for the notification, Bluetooth and the car, and a change between a song and a station is a normal change of item |
| One OkHttp client for the whole app (decision D9), built when the first stream is opened. It lives in `core:playback` for now | A client per use | Shared connections and one place for timeouts and the `User-Agent` (`Tarangini/<version>`). It moves to its own place when another module needs the network |
| OkHttp 5.5.1, pinned in the version catalog. Media3 1.11.1 asks for 4.12.0 | Take the version Media3 asks for | 5.x is the line that gets fixes. It keeps the API that Media3 uses |
| HLS comes from `media3-exoplayer-hls`. The player picks it by the `.m3u8` ending | Our own HLS reader | It is the tested part of Media3 |
| A playlist file is known by its ending (`.pls`, `.m3u`). It is exchanged for its first `http` or `https` entry just before the stream is opened, on the loading thread of the player (`PlaylistResolver` behind a `ResolvingDataSource`) | Resolve when the station is saved | A station may change its stream address at any time. Reading the file at each play follows it |
| Limits for a playlist file: 64 KiB, 15 seconds for one read, 3 files that point to the next one, and the 20 redirects the HTTP client follows. A file that leads back to itself is an error | No limits | The file comes from an unknown server. Without limits it could keep the phone loading for ever or fill its memory |
| Only `http` and `https` are followed: in the address of a station, in an entry of a playlist file and in a redirect. An entry with another scheme is skipped | Follow what the file says | A playlist file must never make the app open a file of the phone or the content of another app |
| Every address is checked again just before the player opens it: an audio file of the phone, or `http` or `https`. Anything else is refused (`RefusedAddressException`) | Check only the item | An HLS playlist comes from the server and names the addresses of its parts and keys itself. Without the second check it could name a file of this app or the content of another app |
| An address with a user name or password in it is refused, and one with a backslash | Allow them | The address is the id of the item, which every app that may see what plays can read. Android and the HTTP client do not agree on the host of an address with a backslash |
| Plain `http` is allowed for the whole app in the network security settings | Allow it per host | Android can allow it only for the whole app or for hosts known in advance, and a station can be on any host. The rule for the rest is in the code: everything that is not a stream uses `https` |
| The session accepts a stream item only as a complete `http` or `https` address with a host. As for a file, the player gets a new item with the id and the address and nothing else the caller sent | Pass the item on | The same reason as in ADR 0004. Only this app may add items at all (`CallerPolicy`) |

## Consequences

- A stream has no length, so the screens show it as live (ADR 0019).
- A stream item has no cover picture address. The radio screens show a letter tile.
- The name of a station is not known to the player yet. It comes with the stations of the user.
- The wake mode follows the item: Wi-Fi stays on for a stream (ADR 0004).

## Known limits

- A playlist file is known only by its ending. An address with another ending that answers with a playlist file does not play.
- An HLS stream behind a `.pls` or `.m3u` file does not play, because the kind of stream is chosen from the address of the station before the file is read.
- A station that needs a user name and password does not play yet.
- A stream may name any `http` or `https` host, also one in the home network. That is wanted: a station can run at home.
- Only the first usable entry of a playlist file is used. A change to the next entry when the first fails belongs to the work on weak networks.
- The song title a station sends (ICY) is not read yet.
- The tests open each kind of station against a local server and check which part of the player reads it. Sound from a real station, and the change between a song and a station, are checked on a phone once a station can be added in the app (`docs/MANUAL-TESTS.md`).
