# ADR 0040: Shuffle and repeat buttons, album grid, and the last item for the media panel

- Status: accepted, 21 Nov 2026.
- Rules: P8, S1.

## Context

In the car the driver could mark a favourite but could not switch shuffle or repeat. The albums showed as a list of small rows. After a restart of the phone the media panel showed nothing of this app, although the saved queue was there.

## Decisions

| Decision | Options looked at | Reason |
|---|---|---|
| Shuffle and repeat are two more buttons of the session, each with its own command that carries no value. `SessionButtons` builds them from the state of the player and joins them with the heart | The shuffle and repeat commands of the player, which the car does not show as buttons | The car, the notification and the lock screen show the buttons a session names. A command with no value cannot be misused |
| A button shows the state by its icon, and its name says what a press does | A name that says the state | It is the rule of the heart button already, and what a screen reader should say |
| Repeat goes off, all, one, off | Only off and all | The same three modes as on the phone |
| While nothing is loaded there is no button, and a press that still arrives changes nothing | Buttons that do nothing | A press would change a mode the driver cannot see |
| Each album row carries the hint "show me as a tile with my cover" | A hint on the Albums node for all its rows. A hint for every list from the root | Covers help to find an album. A hint on the node would also turn the letters of a long list into tiles with no cover. Songs, artists, folders and playlists read better as a list. A car is free to ignore the hint |
| The media panel of the phone (`com.android.systemui`, trusted by the system) gets the two browse commands that Media3 needs to answer "what played last". Every list of the library is refused for it in the callback | Give it the level of the car. Leave it without | Media3 answers that one request itself from the saved queue, before the list code is called. Without the commands the panel gets no answer and shows no card |

## What follows

- The buttons are handed to the session only when they changed, since every call makes the notification again.
- The same three buttons show in the notification and on the lock screen as far as there is room.
- Another caller the system trusts, such as Bluetooth or an app with notification access, still gets no browse command.
- Stations are out of the release, so there is no grid for them.
- The checks that need a head unit or a restart of the phone are rows 116 to 118 of `docs/MANUAL-TESTS.md`.
