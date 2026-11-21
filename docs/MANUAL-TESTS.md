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
| 73 | In the settings of the phone take away the audio permission of Tarangini, then open Library in the car | The car shows the text of row 85. The app does not crash and the car shows no endless loading |
| 74 | Press next, previous and play or pause on the steering wheel, once over Android Auto and once over Bluetooth alone | The phone does the same. The volume buttons change the volume of the car |
| 75 | Play a song in Tarangini, disconnect the phone from the car, then connect it again | Tarangini is still the player the car shows, with the same song at the same place. It does not start to play by itself unless the car is set to resume |
| 76 | With nothing playing, press play on the steering wheel after Tarangini was the last player | The last queue goes on from where it stopped |
| 77 | In the car open the search of Tarangini and type or say a part of a song title | Songs, then artists, then albums are listed. A tap on a song plays it |
| 78 | Say "play (a song title) on Tarangini" | That song plays |
| 79 | Say "play songs by (an artist) on Tarangini" | The songs of the artist play |
| 80 | Say "play (words that match nothing) on Tarangini" | Nothing plays, and the car shows that nothing matches. No silence with a playing state |
| 81 | Say "play music on Tarangini" once with a queue from before and once after the app data was cleared | The last queue goes on. With no queue, songs of the library play in a mixed order |
| 82 | On the phone alone, with the app closed, say "play (a song title) on Tarangini" to the assistant | The app opens and the song plays |
| 83 | Open Albums and a list of songs in the car, with albums that have a cover and some that have none | Covers show in the lists. A row with no cover shows the placeholder of the car, and no row loads for ever |
| 84 | Play a song with a cover and open the now playing screen of the car | The cover shows there, and on the lock screen and the notification of the phone |
| 85 | Take away the audio permission of Tarangini in the settings of the phone, then open Library in the car | The car shows "Allow access to audio files on your phone, when it is safe" and no endless loading |
| 86 | Give the permission back on the phone and open the app once | The lists come back in the car without a reconnect |
| 87 | Use a phone with no music (or hide all folders), then open Library in the car | The tab is empty and the car shows that there are no songs yet |
| 88 | On the phone open the menu of a song and choose "Add to favourites", then open the Favourites tab | The row shows a small heart at once, in every list. The Favourites tab lists the song, the one marked last first |
| 89 | Play a song and press the heart on the now playing screen, then look at the notification and the lock screen | The heart is filled in all three places. A press on the heart of the notification takes the mark away again |
| 90 | Open Favourites in the car and tap a song | The tab lists the favourites, the one marked last first, and the song plays with the favourites as the queue |
| 91 | Play a song in the car and press the heart on the player screen of the car | The heart changes, and the song shows in the Favourites tab of the car and of the phone |
| 92 | Play three songs for more than 10 seconds each and skip a fourth after 3 seconds, then open Recent in the car | The tab lists the three songs, the one heard last first. The skipped song is not there |
| 93 | Tap a song in Recent in the car | The song plays with the recent songs as the queue |
| 94 | Make a playlist, add three songs from the Library and one from now playing, then close the app from the recent apps and open it again | The playlist is there with its four songs in the order they were added |
| 95 | In the playlist move a song to the top from its menu, remove another, then tap Play and after that Shuffle | The order changes at once, the removed song is gone, Play starts with the first song, and Shuffle starts in another order |
| 96 | Delete a playlist: answer Cancel first, then Delete | Cancel keeps it. Delete removes it and goes back to the list of playlists. The songs are still in the Library |
| 97 | Open Library, then Playlists in the car, open a playlist and tap its second song | The car lists the playlists and the songs in their order, and the song plays with the playlist as the queue |
| 98 | Install the app new and open it | Home shows the welcome text and "Open library", and no empty section. The button opens the Library tab |
| 99 | Play a song for more than 10 seconds, pause it, and go to Home. Then close the app from the recent apps and open it again | Home shows "Continue listening" with the song and "Recently played" with it. After the restart both are still there, and the button goes on with the song |
| 100 | With 10,000 songs in the library, open the app from the launcher | Home is on screen in under 1 second |
| 101 | In Settings hide a folder that holds songs. Look at the Library, at search and at the car list | The songs of the folder are gone from every list and from the car. Showing the folder again brings them back, with their favourites and playlist places |
| 102 | Set the shortest track to 1 minute, then close the app from the recent apps and open it again | Files shorter than a minute are gone from the lists. After the restart Settings still says 1 minute |
| 103 | Tap "Clear history" and answer Clear. Open Home and the Recent tab of the car | "Recently played" is gone from Home and the Recent tab is empty |
| 104 | In Settings open "Open source licences". Scroll the list, open the licence of a library and of Manrope, scroll each text to its end, press Back | The list holds the libraries by name with version and licence. Each text opens and scrolls to its end. Back returns to the list, and Back again to Settings |
| 105 | Switch TalkBack on. Go through Home, Favourites, a playlist, now playing, the mini player, Settings with its three dialogs and the licences screen by swipes | Every control is reached in a sensible order and says what it is and what it does. A row offers its menu entries as actions. Nothing is read twice and nothing is silent |
| 106 | Set the font size and the display size of the phone to the largest. Open every screen, in portrait and in landscape | No text is cut in the middle of a letter or lies over another text. Every control can still be reached, by scrolling where needed |
| 107 | Charge the phone to 100 percent. Play a queue of files for 4 hours with the screen off, on the phone speaker or headphones | Playback never stops by itself. Write down the battery percent used. No crash line and no ANR in the log |
| 108 | While a song plays, take the audio permission away in the system settings. Then give it back | The app does not crash. The library shows the reason screen, and after the permission is back the songs are there with their favourites and playlists |
| 109 | Play a song from a memory card or a USB drive, and remove the storage while it plays | The app does not crash. Playback goes to the next item that can be played or stops. The songs of that storage leave the lists and come back with it |
| 110 | Open ten other large apps so the system ends this one in the background while it is paused. Open it again | The queue and the place are back, paused. Nothing is lost |
| 111 | Install a debug build, use every screen for ten minutes and play a few songs. Read the log for "StrictMode" and watch for a LeakCanary notice | No LeakCanary notice. Every StrictMode line is from a library, or has its own issue |
| 112 | Close the app from the recent apps. Tap its icon and time until the first screen can be used. Do it five times | Under 1.5 seconds each time on the test phone. Write the times down |
| 113 | Build the release app (`assembleCoreRelease`), sign it with a test key and install it. Scan, play a song, use search, a playlist, favourites, Settings with the licences screen, and connect to Android Auto | Everything works as in the debug build. No crash from a class or resource that R8 removed |
| 114 | Look at the app icon in the launcher, as a round and a square shape, and with themed icons switched on | The five bars are whole in every shape, and the themed icon takes the colours of the wallpaper |
| 115 | In Settings tap "Privacy policy" | The browser opens the policy page |
| 116 | In Android Auto play a song and press the shuffle and the repeat button on the now playing screen. Then change both on the phone | Each button shows its state and changes the playback. A change on the phone shows in the car |
| 117 | In Android Auto open Library, Albums | The albums show as a grid of covers (a head unit may still choose a list) |
| 118 | Play a song, pause it, and restart the phone. Pull down the quick settings without opening the app | The media panel shows the last song. Play starts it at the saved place, with the queue as it was |
| 119 | Play one song three times and two others once, each for more than 10 seconds. Open Home, then the Most played tab in Android Auto. Run "Rescan" in Settings and look again | Both lists start with the song played three times. After the rescan the lists are the same |
| 120 | Play a queue. In the menu of now playing set the sleep timer to 15 minutes, switch the screen off and wait. Then set "At the end of this song" and wait for the song to end | After 15 minutes the sound fades for a few seconds and stops; the next play is at full volume. With the second choice the song plays to its end and the next one does not start. A timer that is turned off stops nothing |
| 121 | Play a long recording and set the speed to 0.5x, 1.5x and 2x. Then play another album from the Library | Each speed plays with the natural pitch of the voice. The new album starts at 1x |
| 122 | With songs on the phone and on a memory card, open Library, Folders | The built-in storage shows as "Phone storage". The card shows with the name Android gives it, or as "Memory card" |

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
