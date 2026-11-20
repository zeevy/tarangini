# ADR 0012: Rescan by itself

- Status: accepted, 20 Nov 2026.
- Rules: A8, D3, H9, K2.

## Context

New or deleted songs must show without the user doing anything: while the app is open within a few seconds, and after the app was closed the next time it opens. The scanner of ADR 0010 runs one scan at a time and writes nothing when nothing changed. This decides who asks it to scan, and when.

## Decisions

| Decision | Options looked at | Reason |
|---|---|---|
| `LibraryWatcher` from `core:domain` is started once by the application class. `DefaultLibraryWatcher` in `core:data/mediastore` does the work | Start it from the first screen; start it from the Library tab | The app process also starts for the playback service and for the rescan job, with no screen. The library must be in step there too |
| A scan runs at every start of the app process | Only when the last scan is old | A scan with no change reads one list and writes nothing (ADR 0010), so it costs little. A rule about age would be one more thing that can be wrong |
| While the app runs, a content observer on the audio files of MediaStore asks for a scan. `MediaStoreAudioSource` holds the observer and gives the changes as a flow | A timer; a file observer on folders | MediaStore is where the library comes from, and it says when it changed. Only one class talks to MediaStore |
| A scan waits until the phone has said nothing for 2 seconds, and at most 30 seconds when the notices never stop. A change that comes while a scan runs gives one more scan after it | Scan on every notice; scan at most once every N seconds | The phone sends many notices for one copied file. Ten quick changes give one scan. A scan is never stopped half way for a new notice |
| While the app is closed, one WorkManager job waits for a change of the audio files (a content trigger), with 30 seconds of quiet, at most 5 minutes, and not while the battery is low. When it ran, it asks for the next one | A job every few hours; no job, scan only at the next start | A job with a timer wakes the phone also when nothing changed. This one runs only after a change. The library is then ready when the app opens, and the car and the session see new songs |
| The job gets the scanner through a Hilt entry point | `hilt-work` with a worker factory | One small interface, and no extra library or start-up code. It is the way the playback service takes its parts too |
| WorkManager starts when it is first used, with the settings the application class gives (`Configuration.Provider`) | Its start-up provider, which starts it at every app start | The documented way to start it on demand. The start does not depend on the order in which the system makes providers, and the tests of the app run the same path |
| The watcher and the job only ask. The lock that allows one scan at a time stays in the scanner, and "Rescan" in Settings calls `LibraryScanner.scan()` | A second lock in the watcher; a rescan method on the watcher | The job, the watcher and the button all reach the same scanner, so the lock must be there. The scanner already returns the result and shows its progress as state |
| An error in a scan that nobody waits for (at the start, after a change, in the job) is dropped, and the next scan tries again | Let it end the app | The watcher starts with every start of the app. A full disk must not stop the app from opening or the music from playing |

## Consequences

- WorkManager is a new library of the app (Apache-2.0). Its manifest adds a start-up provider, its own database and the normal permissions `WAKE_LOCK`, `ACCESS_NETWORK_STATE`, `FOREGROUND_SERVICE` and `RECEIVE_BOOT_COMPLETED`. The app asked for the first three already or will for playback and radio; the last one lets WorkManager put its jobs back after a restart of the phone. None of them shows a question to the user.
- When the app is open and the files change, the watcher scans and the job runs too. The second one finds nothing to write.
- The phone gives no notice when the user allows the audio permission. The screen that asks for it starts a scan after the answer; that comes with the library screens.
- The job asks for the next job only when its scan ended. A change in that short gap is seen at the next change or the next start of the app.
- An error in such a scan is not reported anywhere yet. It goes to the crash reporter when that exists (issue #45).

## Open points

- The checks on a real phone are in `docs/MANUAL-TESTS.md`, section "Library: scan": a copied file with the app open, and with the app closed.
