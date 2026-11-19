# ADR 0008: Asking for the audio permission

- Status: accepted. Decided by dev in auto mode, 19 Nov 2026.
- Rules: H9, U2, U8, U9.

## Context

The app cannot list songs without the permission to read audio files. The request must say why, take a no, and never stand in the way of radio.

## Decisions

| Decision | Options looked at | Reason |
|---|---|---|
| Only the Library tab stands behind the permission (`AudioPermissionGate` in `feature:library/permission`). Home, Radio, Favourites and Settings open without it | Ask when the app starts | A user who only wants radio is never asked. The question comes where its reason is plain |
| A reason screen comes first. The question of Android shows only after a tap on "Allow access" | Show the system question at once | Android shows its question two times at most. A user who knows why is more likely to say yes the first time |
| Four states: not asked, granted, denied, denied for good. A second no to the question of Android is for good at once. A refusal that comes with no word from Android (the user closed the question, or Android did not show it) keeps the button that asks; a second one in a row is taken as for good | Save "we asked before" in the settings store; take every such refusal as for good | Android does not tell "never asked", "closed without an answer" and "refused for good" apart. A saved flag goes wrong when the user resets the permission in the system settings. Taking the first one as for good would send a user to the settings who only tapped beside the question |
| No ViewModel for the gate | A ViewModel with a `UiState` | The state is what Android says now, read on every return to the screen. There is nothing from the domain to hold |
| The permissions are declared in the manifest of `feature:library`: `READ_MEDIA_AUDIO`, and `READ_EXTERNAL_STORAGE` with `maxSdkVersion="32"` | The app manifest | The module that asks for a permission declares it, as `core:playback` does |
| The body of the screen is the new shared `MessageState` in `core:ui` | A layout of its own in the feature | Empty and error states of later screens need the same picture, title, text and button (U8) |

## Result

- The permission is read again each time the screen comes back to the front. Android stops the app when a permission is taken away in the settings, so the next start shows the reason screen.
- `ManifestPermissionsTest` in the app fails when any module asks for `MANAGE_EXTERNAL_STORAGE` or for pictures or video.
- The text that names the permission in the system settings differs: "Files and media" on Android 12, "Music and audio" from Android 13 (`values-v33`).

## Open points

- After a restart, a user who refused for good sees the first screen again and needs two taps on "Allow access" to reach the settings button. The first tap changes the text, so it is not a dead button.
- A user who closes the question two times without an answer is sent to the app settings, although Android would still ask.
- The flow was run on the emulator with Android 17 only. The emulator images for API 31 and 33 are not on the build machine. The gate tests run the blocked screen on API 31 and 33 under Robolectric; a run on both emulator images is still to do.
- The partial media access of Android 14 ("selected photos and videos") does not exist for audio, so nothing is done for it.
