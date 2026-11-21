# ADR 0041: Sleep timer and playback speed

- Status: accepted, 21 Nov 2026.
- Rules: A9, P1.

## Context

People fall asleep to music and long recordings, and listen to talks faster or slower. Both settings belong to the player and not to a screen: they must hold while the app is closed.

## Decisions

| Decision | Options looked at | Reason |
|---|---|---|
| `SleepTimer` in `core:domain` has one implementation, `DefaultSleepTimer`, a singleton that the playback service hands its player while the service lives | Commands of our own through the media session, with the state in the session extras | The screens and the service are one process. A direct object is less code, has no caller to check, and the timer still lives as long as the player does |
| "At the end of the song" asks the player itself to pause at the end of the item | Pause when the next item starts | Nothing of the next song is heard |
| A timed stop gets quieter over the last 5 seconds, and the volume is back to full after the pause | A hard stop | A sound that ends at once wakes a person who is nearly asleep |
| The times are 15, 30, 45 and 60 minutes. A new timer takes the place of the one before | A field to type any time | Four taps cover the use. The interface takes any time up to 12 hours, so another choice needs no new code below the screen |
| A timer ends with the playback service | Keep it for the next start | With no player there is nothing to stop |
| The speed is the playback speed of Media3 with the pitch left at 1, set through `PlaybackController.setSpeed` and held between 0.5 and 2 | An audio processor of our own | Media3 already changes the speed with the pitch kept |
| A new queue starts at 1x. A song added to the queue, or the next song of the same queue, keeps the speed | Back to 1x at every song. Keep the speed for ever | A speed is picked for one recording or one book. The next thing a person starts is most often music |
| A live stream has no speed line | Offer it and ignore it | A stream cannot play faster than it arrives |
| Speed, sleep timer and "Add to playlist" are lines of one menu in the top bar of now playing | A row of buttons under the player buttons. One icon each | The row fell below the fold on a small phone, and five icons leave no room for the title at a large font |

## What follows

- The car and the notification have no control for the speed or the timer. A speed that is set applies to them too, since there is one player.
- The time left counts down once a second by the timer of the app. It is not an alarm: Android may delay it by a few seconds, which does not matter here.
- The checks that need a real phone are rows 120 and 121 of `docs/MANUAL-TESTS.md`.
