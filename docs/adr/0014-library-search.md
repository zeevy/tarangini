# ADR 0014: Library search

- Status: accepted, 20 Nov 2026.
- Rules: A6, A7, A9, U5.

## Context

Search runs while a person types on the phone, from the search screen of the car and from a spoken "play ...". All three must find the same things. The library already keeps a full text table of the songs (title, artist and album) that the library DAO writes together with each song.

## Decisions

| Decision | Options looked at | Reason |
|---|---|---|
| One `SearchRepository` in `core:domain` with one function, `search(text, limit)`. `DefaultSearchRepository` in `core:data/repository` asks the full text table | A function on `LibraryRepository`; a use case | It is the one entry for the phone, the car and voice. A use case in front of it would only pass the call through (rule A9). `LibraryRepository` is as large as an interface may be |
| The result has three groups: songs, albums, artists. Each has at most `limit` rows (20 when nothing is said, 100 at most) | One mixed list | A screen shows the groups under headings, and the car shows them as three short lists |
| A song is found when every word is in its title, artist or album. An album is found by its title only, an artist by the name only | Albums and artists of every found song | "delhi" must not list every artist who has a song with Delhi in its title |
| Order inside a group: a name that starts with the whole text, then (songs) every word in the title, then the rest; inside these by name | The rank function of the search engine | The full text table of Android (FTS4) has no rank function. These three steps are what a person expects, and they are cheap |
| Albums and artists are found through the same table, with the words looked for in one column | Two more search tables | No new table and no schema change. The table has the album title and the artist name of every song already |
| Words for one column are sent lower case and not quoted | Quoted words, as in the search of all columns | The table refuses a quoted word after a column name. Its operators are written in capitals, so a lower case word is never one. Only letters, digits and their marks get this far in any case |

## Numbers

Measured once with 20,000 songs on a development computer: a search of one letter takes about 20 ms for the three groups and a search of a word about 10 ms. A phone is some times slower, so a word should stay under 100 ms and one letter may not. The check on a real phone is in `docs/MANUAL-TESTS.md` and can be done once the search screen is there. If one letter is too slow there, the search field starts at two letters.

The names of a song are read and the songs of an album or artist are counted only for the rows that are returned, not for every match. A search text is cut at 256 characters and 8 words.

## Consequences

- A word must start a word of the name: "aga" does not find "Raga". That is how the full text table works, and it keeps the search fast.
- The start of a name is compared in the form the lists are sorted by, so "Rock roll" does not count as the start of "Rock - Roll". Such a song is still found, only not first.
- The full text table splits a word of an Indian script at its vowel signs. A search in Telugu or Hindi finds the right songs, and also some that only share the first consonants. A name that starts with the text still comes first. A better split needs a change of the table and is its own issue.
- The reader of a search field waits a short moment after the last key before it asks. That code comes with the search screen.
