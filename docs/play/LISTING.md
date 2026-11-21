# Play Store listing (draft)

A draft for review. Nothing here is live until it is entered in Play Console.

## Texts

| Field | Limit | Text |
|---|---|---|
| App name | 30 | Tarangini: Music Player |
| Short description | 80 | Play the music on your phone, with Android Auto. No ads, no account. |

Full description (4000 at most):

> Tarangini plays the MP3 and AAC files on your phone. It is made for the car first: your whole library is in Android Auto, with search and voice.
>
> - Your songs, albums, artists and folders, with fast search
> - Gapless playback for live albums and mixes
> - A queue you can reorder, shuffle and repeat
> - Favourites, playlists and the songs you played last
> - Android Auto: browse your library, pick a playlist, or say "play ... on Tarangini"
> - Picks up where you stopped, also after a restart
> - Hide folders such as voice notes and call recordings
> - One dark look that is easy on the eyes at night
>
> No ads. No account. Your music and your lists stay on your phone.
>
> Tarangini is open source: github.com/zeevy/tarangini

## Console answers

| Topic | Answer |
|---|---|
| Category | Music & Audio |
| Target API level | 37. Play asks for 36 or higher since 31 August 2026 (checked 21 Nov 2026) |
| Foreground service type | `mediaPlayback`. Reason: the app plays audio while the screen is off or another app is open. The video shows: start a song, press Home, the song goes on with the media notification |
| Data safety: data collected | Crash logs and diagnostics, in the Play build only. Not shared, not linked to the person, sent encrypted |
| Data safety: everything else | Nothing collected. Audio files are read on the phone and never leave it |
| Ads | No |
| Target audience | 13 and over. Not for children |
| Privacy policy | https://github.com/zeevy/tarangini/blob/main/docs/PRIVACY.md |
| Android Auto | Opt in under Advanced settings, Form factors, Android Auto. The review needs the media app to pass the Android Auto quality rules |

## Pictures still to make

| Picture | Size | State |
|---|---|---|
| App icon | 512 x 512 | To export from the launcher icon |
| Feature graphic | 1024 x 500 | Not made |
| Phone screenshots, at least 4 | 1080 x 2400 | Not made: Home, Library, now playing, a playlist |
| Android Auto screenshots, at least 1 | From the head unit | Not made |
| Short video of the foreground service | Under 30 seconds | Not made |
