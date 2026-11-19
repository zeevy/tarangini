# ADR 0005: How the screens reach the player

- Status: accepted. Decided by dev in auto mode, 19 Nov 2026.
- Rules: A3, A5, K2, K3, T3.

## Context

ViewModels must read what the player does and send it commands without holding a player or any Media3 type. The connection to the playback service must not keep the service alive when it is not needed, and it must come back when the service is started again.

## Decisions

| Decision | Options looked at | Reason |
|---|---|---|
| `PlaybackController` and `PlaybackState` live in `core:domain`. The implementation is in `core:playback/controller`, bound with Hilt | A ViewModel holds a `MediaController` | ViewModels stay plain classes with fast tests, and Media3 stays in one module (A5). `docs/ARCHITECTURE.md` already says so |
| One connection for the whole app (`PlayerLink`), not one per screen | A connection per ViewModel or per screen | Every screen then shows the same state, and there is one place that connects, waits and connects again |
| The connection is open while a screen of the app is open. It is released when the last screen closes, but not for a rotation | Keep it for the life of the process; follow the foreground and background state of the app | A connection that is never released keeps the service alive after the app is swiped away while paused (ADR 0004). Following the foreground state would empty the state each time the user leaves the app for a moment |
| The app tells the connection about its screens through `PlaybackScreenWatcher`, registered once in `SwaraTarangApp` | A lifecycle library for the whole process | No new library, and the watcher sees a rotation as it is |
| When the service stops while a screen is open, the connection is made again at once. A connection that could not be made is not tried again by itself; the next command or the next screen tries | Always try again at once | A refused connection would be refused again and again in a tight loop |
| A command sent while the player is not reachable waits and runs when it is | Lose the command; make every command a suspend function | A screen can send a command at any time and needs no code for the short time before the connection is there |
| The position is read from the player twice a second while it plays | A position with a time stamp that each screen moves forward by itself | The player sends no event while the position moves. One timer in one place is simpler for every screen than the same sum in each of them |
| `play` takes the MediaStore addresses of the files | Song ids of the library | The library does not exist yet. The session accepts only MediaStore audio items (ADR 0004). Song ids replace the addresses when the library is built |
| The state has shuffle and repeat, but there are no commands for them yet | Add the commands now | The issue asks for play, pause, seek, next and previous. The other commands come with the screen that needs them |

## Result

- A ViewModel asks for `PlaybackController` and nothing else. In a test it uses `FakePlaybackController` from `core:testing`.
- After the service was started again the player is empty: the queue is not saved yet. The state shows that.
- Media3 does not report a position past the length the file states. A file with a wrong length in its header shows a position that stops early.
- The position is read only while something collects the state, so nothing wakes up twice a second while the app has no screen to show.
- A command sent with no screen open is ignored. A connection opened for it would have no screen to release it.
- The queue is in the order it was loaded. The order the player picks with shuffle on is not in the state yet; the queue screen adds it when it needs it.
- `playWhenReady` in the state is what a play and pause button shows. `isPlaying` is false while the player loads.
- A connection that is refused loses the commands that waited for it, with no error in the state. The session never refuses this app, so this is left as it is.
- A screen test of the app that opens a screen uses `NoPlaybackServiceRule`. Robolectric answers a service connection with an empty one, which Media3 cannot handle.
- The texts of a queue item that is not the current one are empty for now. The player knows the tags of the current file only; the library gives the others when it is built.
