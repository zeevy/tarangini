# Manual tests

Checks that need a real phone, a real accessory or a person listening. Automated tests cannot do them. Run the list of an area before a release, and after a change to that area.

For every check: start a song first, unless the steps say something else. "Pass" means what the "Expected" column says, with no crash.

## Playback: audio focus, headphones and media buttons

Run on a real phone with a SIM. The emulator can do checks 1, 8 and 9 only, with a simulated call and media keys sent by the system.

| No. | Steps | Expected |
|---|---|---|
| 1 | Play a song. Get a phone call and answer it. End the call | The song pauses when the call is answered, or already when the phone rings. It goes on by itself after the call, from the same place |
| 2 | Play a song. Get a phone call and reject it | The song goes on after the ringing stops |
| 3 | Play a song. Start navigation in a maps app and wait for a spoken turn | The song gets quieter while the voice speaks and comes back to full volume after it. It does not pause |
| 4 | Play a song. Start a video or music in another app | The song pauses. It does not start again by itself when the other app stops |
| 5 | Play a song. Let an alarm or a timer ring and dismiss it | The song pauses or gets quieter while it rings and goes on after it |
| 6 | Play a song with wired headphones. Pull the plug out | The song pauses at once. No sound comes from the speaker. It stays paused until play is pressed |
| 7 | Play a song on a Bluetooth headset or speaker. Switch the Bluetooth device off | Same as check 6 |
| 8 | With the screen off, press the button on a wired headset: once, then twice quickly, then three times quickly | Once: pause, and play again on the next press. Twice: next song. Three times: previous song or the start of this one |
| 9 | With the screen off, press play, pause, next and previous on a Bluetooth headset or a car stereo | Each button does what it says |
| 10 | Play a song, switch the screen off and leave the phone for 10 minutes | The song and the songs after it keep playing with no gap or stop |
| 11 | Pause a song. Plug headphones in, or connect a Bluetooth device | Nothing starts to play by itself |

## Playback: queue

Run on a real phone with a large library. Until the library is built, the list is made by a trial build that repeats the same files.

| No. | Steps | Expected |
|---|---|---|
| 12 | Play all songs of a library of 20,000 songs, starting in the middle. Measure the time from the tap to the first sound | Under 1 second |
| 13 | While a song plays, move items in the queue, also the current one, and remove items that are not the current one | The song goes on with no gap |
| 14 | With 20,000 songs in the queue, use the app for a minute: scroll, open the queue, press next | No freeze of more than a moment. The phone does not get warm |
| 15 | With 20,000 songs in the queue, connect a Bluetooth headset or a car stereo | Play, pause, next and previous work. The title shows |

## Results

Write each run here: the date, the phone and Android version, the app version, the checks that failed, and who ran it.

| Date | Phone | App version | Failed checks | Run by |
|---|---|---|---|---|
| 19 Nov 2026 | Emulator, Android 17 (API 37) | Build of the audio focus change | None of the ones tried. Tried: 1 with a simulated call (kept playing while it rang, paused on answer, went on after the call). Not tried in this build: 8 and 9 (play, pause, next and previous worked as media keys sent by the system in the builds before it, with the screen on). Not possible on the emulator: 2 to 7, 10, 11 | dev |
| 19 Nov 2026 | Emulator, Android 17 (API 37) | Trial build of the queue change, 20,000 items made from 2 files | None of the ones tried. Tried: 12 (first sound after 670 ms, whole list loaded after 4.6 s; on a slow emulator, not a phone), 13 (a move, a remove, shuffle and repeat while playing: the song went on). Not tried: 14, 15 | dev |
