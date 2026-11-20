# ADR 0023: Stations the user adds

- Status: accepted, 20 Nov 2026.
- Rules: A2, S5, S6, U8.

## Context

A person can play any stream they know: a name, a stream address, a way to try the address, and a list of the saved stations with edit and remove. The address is typed by hand or pasted, so it is checked before it is stored and again before it is opened.

## Decisions

| Decision | Options looked at | Reason |
|---|---|---|
| The check of the form is plain Kotlin in `core:domain` (`checkStreamAddress`, `checkStationName`): `http` or `https` only, at most 2,000 characters, no white space, control character or backslash, a host with a port that can be right, no user name or password. A name loses the characters that cannot be seen or that turn text around | Check only in the player | The form can say what is wrong at once, and the rule has one home. The repository runs the same check before it writes, and the player checks every address again before it opens it |
| The Test button opens the address the way the player does (`DefaultStreamTester` in `core:playback`): same address check, same HTTP client, a playlist file is followed first. It reads the first answer only | Start the real player | The person hears nothing by surprise, and the test cannot disturb what plays. The answer says what the player would meet |
| An HLS answer counts only when the address ends in `.m3u8`, and a playlist answer only when the address ended in `.pls` or `.m3u` | Go by the kind the server names | The player knows these kinds by the ending of the address. A test that says "found" for something that then does not play would be worse than none |
| A station is saved whatever the test said, and with no test | Save only after a good test | A station can be off air for an hour, and the test is one tap more |
| Every text the server sends in the test is cleaned and cut before it is shown | Show what comes | The same rule as for song titles |
| A stream item gets its title from the stored station (`StationStreams.namesByAddress`, in `LibraryTexts`) | Send the name with the play command | Texts never come from the caller. Every controller that plays a stored address gets the stored name |
| A tap on a station plays it with all saved stations as the queue | A queue of one | "Next" on the steering wheel goes to the next station |
| Remove asks first | Remove at once with an undo | A removed station takes its favourite place and its play count with it |
| One row for a station in every list (`StationRow` in `core:ui`). It shows a radio picture until the letter tile is built | A row in the feature | The search screen and the favourites will list stations too |
| The store announces a change itself (`StationRepository.changes`) | Room flows | The stations DAO is at the function limit of the code rules, and a list of a few stations is cheap to read again |

## Consequences

- One address belongs to one station of the person. The form says so when an address is taken, and the store refuses it.
- A form saves one address per station. The table can hold more, and the player moves to the next one when the first fails.
- An address with a user name or a password cannot be saved. It would be stored and shown as plain text.
- An old Shoutcast server and a stream with an odd kind of answer can pass the test and still not play, and the other way round. The test reads the first answer, not the sound.
- The car lists the saved stations when the browse tree is built. Until then a station plays in the car when it was started on the phone.
- What is typed into the form is lost when the system ends the app in the background.
- The form route is outside the tab, so the bottom bar is hidden while it is open.
