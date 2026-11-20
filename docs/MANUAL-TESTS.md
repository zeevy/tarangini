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

## Playback: gapless

Copy the folder `core/playback/src/test/resources/gapless` to the Music folder of the phone. Each album is one tone cut into three tracks, so any gap is heard as a tick or a break.

| No. | Steps | Expected |
|---|---|---|
| 16 | Play `with-delay-header-1.mp3`, `-2` and `-3` in order | One even tone for 6 seconds. No tick and no break at 2 and 4 seconds |
| 17 | Play `with-edit-list-skip-1.m4a`, `-2` and `-3` in order | The same |
| 18 | Play a real album that runs on without a pause (a live recording or a mix), as MP3 from LAME and as M4A | No gap between the tracks |

## Library: scan

Run on a mid range phone. A scan is started from the library screen.

| No. | Steps | Expected |
|---|---|---|
| 19 | Put 10,000 songs on the phone. Install the app new, allow access and time the first scan. Scroll a list while it runs | The scan ends in under 10 seconds and the list scrolls with no stutter |
| 20 | Mark a song on a memory card as a favourite. Take the card out, scan, put the card back and scan | The song leaves the lists and comes back, and it is still a favourite. It is in the lists once |
| 25 | Open the app with the library showing. Copy a song to the phone from a computer | The song is in the library within a few seconds, with no tap |
| 26 | Swipe the app away. Copy a song to the phone and delete another one. Wait 10 minutes and open the app | The new song is there and the deleted one is gone when the library shows |
| 27 | With about 20,000 songs on the phone, open the search from the Library tab. Type one letter, then a word | The results change with no delay that is felt (under a tenth of a second) |
| 28 | Play a song that has a cover picture, then one that has none. Pull down the notifications and look at the lock screen | The first shows its picture in the notification and on the lock screen. The second shows none, and nothing crashes |
| 29 | When the library lists are there: with about 5,000 songs, scroll the songs list fast up and down | The list scrolls with no stutter. Pictures come in after a moment, and a song with no picture shows the music note |

## Library: screens

Run with about 10,000 songs on a mid range phone.

| No. | Steps | Expected |
|---|---|---|
| 30 | Open the Library tab. Scroll the songs list to its end, fast, then do the same in Albums and Artists | Each list opens at once and scrolls with no stutter. New rows come in before the end is reached |
| 31 | Tap a song in the middle of the songs list. Then open an album and tap its third song | The song plays. Next and Previous follow the order of the list that was on screen |
| 32 | Choose "Shortest first" in the sort menu. Swipe the app away and open it again | The songs list is still in that order |
| 33 | Open an artist, then one of its albums. Tap the Library tab in the bottom bar, go to Radio and come back | The Library tab is marked on all three screens. Coming back shows the screen that was open |
| 34 | Switch TalkBack on. Swipe through the songs list, the menu of a song, the tabs and the sort menu | Every row says its title and artist, every button says what it does, and the sort menu says which order is on |
| 35 | Install the app new. Open Library and allow the access | The songs show after a moment, with no restart of the app |

## Search

| No. | Steps | Expected |
|---|---|---|
| 36 | Open the search and type a word that finds many songs. Scroll the results | The keyboard closes when the scroll starts. The text stays in the field |
| 37 | Tap an album in the results, go back, then tap an artist, go back | Each opens its screen. Back returns to the search with the same text and results, and the keyboard stays closed |
| 38 | Tap the second song of the results. Press Next in the notification | The second song plays, then the third one of the results |
| 39 | Play a song. Open each tab, the search, an album and Settings. Scroll each list to its end | The mini player shows on each screen. The last row of every list is above it |
| 40 | With a song playing, pause in the notification, then press play on the mini player, then pause on the lock screen | The button of the mini player and the notification always show the same state |
| 41 | Play a song and watch the mini player for a minute | The thin line grows evenly, and the picture, title and artist are those of the song |
| 42 | Tap the mini player on Settings (gesture navigation on) | The now playing screen opens with no mini player. Back returns to Settings. The bar does not sit under the gesture bar |
| 43 | Play the last song of a short list to its end, with repeat off. Then press play on the mini player | At the end the mini player and the notification both offer play. The press starts the last song again |
| 44 | On now playing, drag the seek bar to the middle of a long song and let go | The sound goes on from that place at once. The thumb stays where it was let go and does not jump back |
| 45 | Open the queue. Drag the third row to the top by its handle, then press Next | The row follows the finger and the others make room. Next plays the song that is now after the current one |
| 46 | In the queue, swipe a row that is not playing to the side. Then swipe the row that plays | The first row goes and the music does not change. After the second swipe the next song plays |
| 47 | Turn the phone on its side on now playing and on the queue, then back | The cover is beside the controls, every button can be reached, and the place of the seek bar and of the list is kept |
| 48 | With TalkBack on, open the queue and use the actions of a row | "Move up", "Move down" and "Remove from the queue" are offered and work. The row that plays is read as "Playing now" |
| 49 | Tap shuffle and repeat on now playing and look at the notification of a car or watch that shows them | The buttons show their state, repeat goes off, all, one, and the order of play follows |
| 50 | Needs a station added in the app. Play one station of each kind: Icecast MP3, Shoutcast, AAC, HLS (`.m3u8`), a `.pls` address and a `.m3u` address | Each one plays within a few seconds. Now playing shows "Live" and no seek bar |
| 51 | Play a song, then a station, then a song again | Each change works with one tap. The notification follows, and the song has its seek bar again |
| 52 | Play a station that has only an `http` address, on mobile data and on Wi-Fi | It plays on both |
| 53 | Play a station for 10 minutes with the screen off, on Wi-Fi | The sound does not stop |
| 54 | Needs a station added in the app. Play a station that sends song titles and wait for the next song | The mini player, now playing, the notification and the lock screen show the new title within a few seconds, with the station under it |
| 55 | Play a station that sends no titles | Its name is the title, and the line under it says "Live" in the mini player. Nothing says "Unknown artist" |
| 56 | Needs a station added in the app. Play a station, turn the network off for 30 seconds, turn it on | The screens show that it loads. The station plays again with no tap, a few seconds after the network is back |
| 57 | Play a station and leave the network off for more than 2 minutes | Now playing shows "No connection to the station" with "Try again", and the notification shows an error. With the network back, "Try again" plays |
| 58 | Store a station with a dead first address and a good second one, and play it | The station plays from the second address after a short wait |
| 59 | Radio tab, add a station with a real Icecast address. Tap Test, then Save, then the station | Test says that a stream answers. The station is in the list and plays, with its name in the mini player and the notification |
| 60 | Add one station each with an HLS (`.m3u8`), a `.pls` and a `.m3u` address and play them | Each one plays |
| 61 | Edit a station, then remove it | The new name shows in the list. Remove asks first, and the station is gone after "Remove" |
| 62 | Play a saved station, then press Next on a headset or in the car | The next saved station plays |
| 63 | Play an MP3 that was cut short and still states the length of the whole song, with another song after it | About 3 seconds after its last sound the next song plays. The screen can be off |
| 64 | Play a song that has a silent passage of more than 5 seconds in it | The song plays through the passage to its end |

## Playback: resume

| No. | Steps | Expected |
|---|---|---|
| 21 | Play a queue of a few songs to the middle of the third song. Force stop the app in the settings of the phone. Open the app | The same queue, the third song and the position, at most 5 seconds back. Nothing plays and no media notification shows until play is pressed |
| 22 | Play a song, pause it, and swipe the app away. Press play on a Bluetooth headset or in the car | The song goes on from where it was |
| 23 | Put three songs in the queue and play the first. Delete the file of the second song with a file manager. Let the first song end | The third song plays |
| 24 | Force stop the app. From a computer, send a media play key to the app's `MediaButtonReceiver` with `adb shell am broadcast` | The app does not crash. If it starts, it plays only the last queue |

## Android Auto

These need the Desktop Head Unit or a car. The steps are in [ANDROID-AUTO.md](ANDROID-AUTO.md).

| No. | Steps | Expected |
|---|---|---|
| 65 | Start the head unit and open the app list | Tarangini is in the list with its icon |
| 66 | Play a song on the phone, then open Tarangini in the head unit | The car shows the song with its title, artist and cover |
| 67 | Press pause, play, next and previous in the head unit | The phone does the same, and the car shows the new state |
| 68 | Install the Media Controller Test app and pick Tarangini in it | The test app lists Tarangini and cannot connect |
| 69 | Open Tarangini in the head unit and open Library, then Songs, Albums, Artists and Folders, down to a song in each | Every list opens within 2 seconds and shows the same names as the phone. No list is empty that has rows on the phone |
| 70 | With more than 100 songs on the phone, open Songs | The car shows letters, with "#" first when a title starts with a digit or a sign. Each letter opens its songs |
| 71 | Tap a song in an album, then press next in the car | The song plays, and next plays the song after it in that album |
| 72 | With a list open in the car, copy a new song onto the phone and wait a few seconds | The song shows in the car after leaving and opening the list again, without a restart of the app or the car |
| 73 | In the settings of the phone take away the audio permission of Tarangini, then open Library in the car | The lists show what the library last held or are empty. The app does not crash and the car shows no endless loading |
| 74 | Press next, previous and play or pause on the steering wheel, once over Android Auto and once over Bluetooth alone | The phone does the same. The volume buttons change the volume of the car |
| 75 | Play a song in Tarangini, disconnect the phone from the car, then connect it again | Tarangini is still the player the car shows, with the same song at the same place. It does not start to play by itself unless the car is set to resume |
| 76 | With nothing playing, press play on the steering wheel after Tarangini was the last player | The last queue goes on from where it stopped |

## Results

Write each run here: the date, the phone and Android version, the app version, the checks that failed, and who ran it.

| Date | Phone | App version | Failed checks | Run by |
|---|---|---|---|---|
| 19 Nov 2026 | Emulator, Android 17 (API 37) | Build of the audio focus change | None of the ones tried. Tried: 1 with a simulated call (kept playing while it rang, paused on answer, went on after the call). Not tried in this build: 8 and 9 (play, pause, next and previous worked as media keys sent by the system in the builds before it, with the screen on). Not possible on the emulator: 2 to 7, 10, 11 | dev |
| 19 Nov 2026 | Emulator, Android 17 (API 37) | Trial build of the queue change, 20,000 items made from 2 files | None of the ones tried. Tried: 12 (first sound after 670 ms, whole list loaded after 4.6 s; on a slow emulator, not a phone), 13 (a move, a remove, shuffle and repeat while playing: the song went on). Not tried: 14, 15 | dev |
| 19 Nov 2026 | Emulator, Android 17 (API 37) | Trial build of the gapless change that writes the sound of the player to a file | None by ear: the emulator has no sound device. Measured for 16 and 17: no silence at the track changes and the wave goes on in step. Not tried: 18 | dev |
| 20 Nov 2026 | Emulator, Android 17 (API 37) | Build of the automatic rescan change | None of the ones tried. Tried: 25 without a screen for the library, read from the database of the app (a copied song was in the library within 6 seconds with the app on screen, a deleted one was marked as gone). Partly tried: 26 (the rescan job is registered with the trigger for audio files and "battery not low", ran after a file was copied while the app was in the background, and asked for the next job). Not tried: 26 with the app process gone | dev |
| 20 Nov 2026 | Pixel 6a, Android 17 (API 37) | Build of the change for files with less sound than they state | None of the ones tried. Tried: 63 with an MP3 of 60 seconds that states 138 seconds (the next song started at second 60, with the screen on and off; a song of 18 seconds after it played to its end, twice) | dev |
| 20 Nov 2026 | Pixel 6a and emulator, Android 17 (API 37) | Build of the change that tells the end of a file by the sound of the decoder | None of the ones tried. Tried on the phone: 63 with two MP3 files, 60 seconds that state 138 and about a minute that states 281 (the next song started about 3 seconds after the last sound, also from a start inside the silent part; a file of 32 seconds with a right header played to its end). Tried on the emulator: 64 with an MP3 of 20 seconds whose seconds 6 to 14 are silent frames (it played to 19.97 seconds) | dev |
