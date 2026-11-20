# ADR 0017: Search screen

- Status: accepted, 20 Nov 2026.
- Rules: A10, A12, U1, U8, U9, T2.

## Context

One search field finds songs, albums and artists while the person types. The search itself is in the repository (ADR 0014). This is about the screen: where it opens from, when it searches and what a tap does.

## Decisions

| Decision | Options looked at | Reason |
|---|---|---|
| The search is its own screen. A button in the top bar of the Library tab opens it | A fifth tab; a field on top of the lists | The bottom bar has its four tabs (ADR 0001). A field on top of the lists would take a row from every list. Home gets the same button when it is built |
| The route is outside the group of the Library tab, and the bottom bar is hidden on it | A route inside the group | The search will also find stations once radio is built, so it does not belong to one tab. Without the bar the results have the whole screen above the keyboard |
| A search starts 200 ms after the last key. A text that is typed over is not searched, and a search that still runs is dropped | Search on every key; a longer pause | Results must be there within 300 ms of the last key, and a search takes 10 to 20 ms on a large library (ADR 0014). Fast typing of a word costs one search |
| An emptied field, or a text with no letter and no digit, clears the results at once | Wait for the pause | There is nothing to search for, and old results under an empty field look wrong |
| The results show in three groups with a title each: songs, albums, artists. A group with nothing in it is left out | One mixed list | The repository gives the groups already in the order that fits (ADR 0014), and a person looks for one kind of thing |
| A tap on a song plays it with the found songs as the queue. A tap on an album or an artist opens the screen of the library | Play the song alone | A search for an artist or a word of a title gives a list a person wants to hear on. The app joins the search with the library screens, because a feature does not depend on another feature |
| The keyboard closes when the results are scrolled, and the search key only closes the keyboard | Keep the keyboard open | The keyboard covers half of the results. The results are there before the key is pressed |
| The screen is not behind the question for the audio permission | Wrap it as the library lists are | The search reads our own database, not the files. Today it opens only from the Library tab, which shows only with the permission. Without it the database is empty and nothing is found |

## Consequences

- The rows are the ones of the library (`SongRow`, `AlbumRow`, `ArtistRow` in `core:ui`), so a song has the same menu here.
- The text of the field is cut at 256 characters, which is what the search reads.
- A search that fails shows a message. The next change of the text searches again.
- The text and the results are kept while an album or an artist is open, and are gone when the search screen is left. Back from the album returns to the search, also after a change of tab in between. The field does not take the cursor again then, so the keyboard stays away.
- The text of the field is state of its own, next to the state with the results. A text field must get its text back in the moment it changes. The results say which text they belong to, and the "nothing found" message names that text.
- Stations join the results with the radio screens.
- The checks that need a phone are in `docs/MANUAL-TESTS.md`.
