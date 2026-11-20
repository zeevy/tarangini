# ADR 0028: Search in the car and spoken requests

- Status: accepted, 20 Nov 2026.
- Rules: P1, P4, P8.

## Context

In the car a driver finds music by voice: "play Bhairavi on Tarangini". Android Auto also has a search screen for the app. The voice assistant sends a spoken request to the media session as an item with a search text and no address; on the phone alone it can open the app with the action `MEDIA_PLAY_FROM_SEARCH`. The text comes from another app.

## Decisions

| Decision | Options looked at | Reason |
|---|---|---|
| The car search and a spoken request both go through `SearchRepository`, in `CarSearch` (`core:playback/browse`) | A second query for the car | One search means the phone and the car find the same things, and the text is cleaned in one place (`FtsQuery`) |
| A spoken request plays the first that fits: a song whose title has every spoken word (the other songs found follow it), then the first artist (the songs of the artist), then the first album, then the songs found by another text. A kind the assistant names (`android.intent.extra.focus`) is tried first, with the name it understood as the text | Always the first song. Ask the driver to pick | People name a song most often. The assistant knows when "songs by ..." was said, and that is better than a guess. A driver cannot pick from a list by voice |
| With no words: the saved queue goes on from its place; with no saved queue, up to 500 songs of the library play in a mixed order | Start nothing | "Play music" must give music |
| Words that match nothing start nothing, and the session sends an error with a text (`sendError`) | Play something else. Fail without a word | Wrong music is worse than none, and silence with no reason looks like a fault |
| The search text is cut to 200 characters, a text with no letter and no digit is not searched, and a request with an address is never treated as spoken: it goes to the address check | Trust the caller | The text is from another app. It is only ever a text to search the library for, and nothing in it can name a file or a stream |
| A spoken request can only replace the queue (`onSetMediaItems`), not add to it | Also in `onAddMediaItems` | The assistant only sends "play". An item with no address stays refused in `onAddMediaItems` |
| On the phone the activity takes `MEDIA_PLAY_FROM_SEARCH` and hands the words to `PlaybackController.playSpoken`, which sends them to the session as the assistant does | Search in the activity and play the result | One path for the phone and the car |

## What follows

- Only this app and the car callers (`CallerAccess.CAR`: Android Auto and the Google app) can search or send a spoken request. A caller with transport controls only cannot change the queue.
- The search screen of the car shows 20 results of each kind at most, as the phone does.
- The checks with a head unit and with the assistant are rows 77 to 82 of `docs/MANUAL-TESTS.md`.
