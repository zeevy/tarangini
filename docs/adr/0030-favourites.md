# ADR 0030: Favourites

- Status: accepted, 21 Nov 2026.
- Rules: A9, P1, P4.

## Context

A person marks songs as favourites and finds them again in one list, on the phone and in the car. The tables exist since the first schema (`favourite_songs`, one row per song with the time it was marked). Radio is out of the first release (decision D48), so favourite stations and their order are not built now.

## Decisions

| Decision | Options looked at | Reason |
|---|---|---|
| `FavouriteRepository` in `core:domain` gives the ids of all favourite songs as a flow, the favourite songs page by page, and one call to set or take away a mark | A flow per song. A flag on `Song` | Every list marks its rows from one small set, so a new mark shows in all of them at once and no list is read again. A flag on `Song` would need every list query changed |
| A mark is written only for a song the library knows, in one statement | Insert and catch the foreign key error | A song can go between the tap and the write. One statement cannot fail on that |
| A row shows a small heart when its song is a favourite, and its menu has "Add to favourites" or "Remove from favourites". `SongRowActions` carries the set and the action | A heart button in every row | A row already has a tap and a menu. A third target in a row is easy to hit by mistake |
| The Favourites tab lists the songs, the one marked last first, behind the audio permission like the library | A list by title | The song a person just marked is the one they look for |
| The media session has one button of its own, a heart (`FavouriteButton`, `core:playback/session`). It shows for a song of the library and its press marks the song that plays | A command that names a song | The notification, the lock screen and the car get the button from the session with no code of their own. A press that names nothing cannot be used to mark another song |
| The heart command is given to this app, to the car and to callers the system trusts | Only this app | The media panel of the phone is a caller the system trusts and shows the button of the notification |
| The car has a Favourites tab after Library (`MediaId.Favourites`). A tap on a song plays the favourites from that song on | Favourites as a fifth list under Library | A driver reaches it with one tap. The car allows four tabs and two are used now |
| After a mark changed the car is told about the Favourites list only, when it has it open | Tell every open list, as after a library change | The list the driver looks at must not be read again and lose its place for a heart press |

## What follows

- Favourite stations, their order by drag and their own copy of name and addresses come back with radio.
- The car shows the 500 favourites marked last, as every car list has at most 500 rows.
- A favourite whose file is gone is not listed and keeps its mark for 30 days, as the scan keeps the song.
- The heart of the session is one button. More buttons (shuffle, repeat) are for the car work in v1.1.
