# ADR 0013: Reading the library

- Status: accepted, 20 Nov 2026.
- Rules: A6, A7, A9, U5.

## Context

The phone screens and the playback service, which builds the tree for the car, must read the library through the same code. No list may load a whole table: a library can have 20,000 songs and more.

## Decisions

| Decision | Options looked at | Reason |
|---|---|---|
| One `LibraryRepository` in `core:domain` for songs, albums, artists and folders. `DefaultLibraryRepository` in `core:data/repository` reads Room | One repository for each of the four | The four are one library, written by one scanner and read from one DAO. A screen for an artist needs albums and songs together. Four interfaces would mean four fakes and nothing gained |
| A list that can be long takes a `Page` (limit and offset, at most 500 rows) and returns that part. The albums of an artist and the folders in a folder come whole | The Paging library; a `Flow` of the whole list | Limit and offset are what the DAO has since the first schema, and the car needs plain lists. `Page` refuses a limit above 500, so no caller can ask for a whole table by mistake. The lists that come whole are short by their nature |
| The songs directly in a folder and the songs of an album are paged too | The whole folder or album | Many people keep every song in one folder, and the songs with no album tag share one album |
| `changes()` gives a value when the library changed. A list then reads its pages again | Each page as a `Flow` | One signal for every list. Room runs a one row query again after each write to the songs table, and that is the signal. Albums, artists and folders change only together with songs |
| The order of the list of all songs is saved in the settings store under `song_sort`. It is read and written through `SettingsRepository`, a second small interface | Keep it in the ViewModel; put it into `LibraryRepository` | The choice must survive a restart, and the settings store is the one place for such choices. It is a setting and not library data, and the Settings screen will add its other choices to the same interface. Albums, artists and folders have one order, so nothing is saved for them |
| No use case yet, although the issue names them. A screen calls the repository | A use case for each read | Rule A9: no use case that only passes a call through. A use case comes when a screen needs logic that is more than one read |
| Rows become domain models in `core:data/mapper`. A `Song` becomes a Media3 item in `core:playback/mapper`; the browse tree for the car is its first user | Media3 items from the data layer | Data and playback do not depend on each other. Both depend on the domain model |
| `Song.mediaUri` is the MediaStore address of the file, and it is the id of the song in the queue | The library id as the queue id | The player takes only MediaStore addresses (ADR 0004), and the saved queue finds its songs by the same address (ADR 0011) |

## Consequences

- A screen that shows a long list keeps the pages it has read and asks for the next one near the end. That code comes with the library screens.
- A deep page costs more than a first page, because the database skips the rows before it. Every order has an index, so nothing is sorted. A rough number: the last page of 20,000 songs takes under a tenth of a second, the last page of 100,000 songs up to half a second. Paging by the last key seen would fix that and would change `Page`; it waits until such a library is a real case.
- Each page is its own read. While a scan writes, rows move, so a reader reads its pages again after `changes()` and never shows one id twice. A scan gives many signals in a short time; a reader waits a moment for the last one.
- A settings file that cannot be read gives the default of each setting. A broken file is replaced by an empty one.
- Songs with no artist or album tag have an empty text there. A screen shows its own words for it.
