# ADR 0004: The playback service and who may use its session

- Status: accepted. Decided by dev in auto mode, 19 Nov 2026.
- Rules: H1, P1, P2, S2, S7.

## Context

One service plays everything. Its media session must be reachable by the system (notification, lock screen, Bluetooth) and later by the car, so the service is exported. An exported service must decide who may use it and what they may ask it to play.

## Decisions

| Decision | Options looked at | Reason |
|---|---|---|
| Media3 1.11.1: `MediaLibraryService`, one ExoPlayer, one `MediaLibrarySession` | The older media compat library | Media3 is the current library and Android Auto supports it. One session serves every surface (H1, P1) |
| A caller may connect when it is this app (same user id and package), or when the system trusts it for media control | Allow every caller with fewer commands; a list of package names | The system flag covers the system itself, Bluetooth and apps the user gave notification access. Those can control any media session on the phone anyway. A package name alone can be faked, so the user id is checked with it (P2) |
| Android Auto and Google Assistant are not let in yet | Let them in now by package name | They are let in by signing certificate together with the Auto setup. Until then they are refused, which is the safe side (S7) |
| A caller can ask the player to open only an audio file known to MediaStore (`content://media/...`) | Pass every URI a caller sends to the player | A trusted caller must not be able to make the app read its own private files, another app's content or a web address. Library and station ids replace raw URIs when the library and the radio are built |
| The service is not guarded by a permission | A signature permission | The system and the car hold no permission of ours. The caller check in the session does the job |
| The service keeps Media3's own handling when the app is swiped away | Our own `onTaskRemoved` | Media3 keeps the service while music plays and stops it when nothing plays. Checked on the emulator |
| The player takes a wake lock for local playback and sets the audio usage to media. Audio focus is not handled yet | Handle audio focus now | Audio focus, headphones and media buttons are one piece of work with its own checks |

## Result

- A screen that controls playback must build its controller with the application context and release it when the screen is closed. A controller that is never released keeps the service alive after the app is swiped away while paused. A controller built with the screen's own context crashes the app when the service stops.
- A song with no tags shows no title in the notification until the library gives the session its own titles.
- `RULES.md` P8 asks for a run on the Desktop Head Unit before a change to the player or session is merged. The app cannot show there before the Auto setup is built, so this change was checked on the phone emulator, and the head unit run of the service is part of the Auto setup.
