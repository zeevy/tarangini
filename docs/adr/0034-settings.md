# ADR 0034: Settings

- Status: accepted, 21 Nov 2026.
- Rules: A9, U4.

## Context

The scanner has read three values from the settings file since it was written: the shortest track, the folders to scan only, and the folders to skip. Nothing could set them. The app has one dark look (decision D51), so there is no theme to choose.

## Decisions

| Decision | Options looked at | Reason |
|---|---|---|
| Settings has three groups: Library (hidden folders, shortest track, rescan), History (clear history) and About (version, source code) | Theme and colour rows as in the first plan | There is one look. A row that changes nothing is noise |
| Folders are hidden, not picked: the dialog lists every folder that holds audio, and a tap hides one with the folders inside it | Also a list of "scan only these folders" | One list answers "keep the voice notes out". Two lists that work against each other are hard to explain. The key for the second list stays in the settings file and the scanner still reads it |
| The list of folders comes from the library (`FolderDao.pathsWithSongs`), where a hidden song keeps its row | Walk the storage. Ask the system folder picker | The library already knows which folders hold audio, and a hidden folder stays in the list, so it can be shown again |
| The shortest track is picked from Off, 15 and 30 seconds, 1 and 2 minutes. The value is kept between 0 and 10 minutes | A number field | A pick needs no keyboard and cannot be wrong |
| A change of what is left out is saved and then scans at once | Scan at the next start | The lists and the car change while the person still looks at them |
| "Clear history" asks first and forgets the plays and the counts | Undo in a bar | It cannot be brought back |
| The frame of Settings takes its rows as a slot, as Home does | The screen builds its own ViewModel | The tests of the app shell open Settings without Hilt |
| "Source code" opens the page of the repository in the browser | Show the address only | One tap. A phone with no browser shows the address on the row |

## What follows

- A folder inside a hidden folder shows as hidden and cannot be changed by itself.
- A link to a privacy text comes with the store listing, when the text has an address.
- The open source licences have their own screen and a row here (`docs/adr/0035-open-source-licences.md`).
- `PlaceholderScreen` is removed: every screen is built.
