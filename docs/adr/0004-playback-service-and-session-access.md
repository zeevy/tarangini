# ADR 0004: The playback service and who may use its session

- Status: accepted, 19 Nov 2026.
- Rules: H1, P1, P2, S2, S7.

## Context

One service plays everything. Its media session must be reachable by the system (notification, lock screen, Bluetooth) and later by the car, so the service is exported. An exported service must decide who may use it and what they may ask it to play.

## Decisions

| Decision | Options looked at | Reason |
|---|---|---|
| Media3 1.11.1: `MediaLibraryService`, one ExoPlayer, one `MediaLibrarySession` | The older media compat library | Media3 is the current library and Android Auto supports it. One session serves every surface (H1, P1) |
| This app (same user id and package) may do everything. A caller the system trusts for media control may connect, but gets the transport controls only: play, pause, seek and skip in the queue that is there. It cannot change what is in the queue. Every other app is refused | Allow every caller with fewer commands; a list of package names; give trusted callers every command | The system flag covers the system itself, Bluetooth and apps the user gave notification access. Those can control any media session on the phone anyway, but an app with notification access is still outside code, so it gets no way to load items. A package name alone can be faked, so the user id is checked with it (P2) |
| Android Auto and Google Assistant are not let in yet | Let them in now by package name | They are let in by signing certificate together with the Auto setup. Until then they are refused, which is the safe side (S7) |
| A caller can ask the player to open only one audio item of MediaStore (`content://media/<volume>/audio/media/<number>`). The player gets a new item with only the id and that address; subtitles, artwork, advert and licence addresses a caller sent are dropped | Pass every URI a caller sends to the player; check only the main address | A caller must not be able to make the app read its own private files, another app's content or a web address, and an item can carry such addresses in more than one field. Library and station ids replace raw URIs when the library and the radio are built |
| The service is not guarded by a permission | A signature permission | The system and the car hold no permission of ours. The caller check in the session does the job |
| The service keeps Media3's own handling when the app is swiped away | Our own `onTaskRemoved` | Media3 keeps the service while music plays and stops it when nothing plays. Checked on the emulator |
| The player sets the audio usage to media, asks for audio focus by itself and pauses when headphones are unplugged. It keeps the processor awake for a file and the network too for a stream | Our own audio focus code | ExoPlayer already does what the app needs: pause for a call and go on after it, a lower volume for a short prompt, a pause for good when another app plays. Less code of ours to get wrong. The checks a person must do are in `docs/MANUAL-TESTS.md` |

## Result

- A screen that controls playback must build its controller with the application context and release it when the screen is closed. A controller that is never released keeps the service alive after the app is swiped away while paused. A controller built with the screen's own context crashes the app when the service stops.
- A song with no tags shows no title in the notification until the library gives the session its own titles.
- A media key (play, pause, next, previous) sent to the service does not pass the caller check. This is left as it is: Android lets any app send media keys to the active session, so a check here would add no safety. Such a key can only control the queue that is there.
- A tap on the notification or the lock screen card opens the app through an immutable pending intent.
- A headset or Bluetooth button works while the service is alive. A button pressed after the service has stopped does nothing: starting playback again from a button needs the saved queue, which is not built yet.
- The wake mode follows the current item (`WakeModeSwitch`): local for a file, network for an `http` or `https` stream.
- Rule P8 asks for a run on the Desktop Head Unit before a change to the player or session is merged. The app cannot show there before the Auto setup is built, so this change was checked on the phone emulator, and the head unit run of the service is part of the Auto setup.
