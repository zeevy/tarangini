# ADR 0063: The crossfade hook

- Status: accepted, 24 Nov 2026. Builds on ADR 0052 (the trial), ADR 0007 (gapless) and ADR 0024 (a file shorter than stated).
- Rules: H1, P1.
- Decision D95.

## Context

The crossfade trial (ADR 0052) showed that a wrapper in front of the audio sink of the one player can hold the end of a song and mix it into the start of the next one. Crossfade is a premium feature, so the public core needs a hook that the premium module fills in. The trial left open points: the song end, the order the player really plays, gapless albums, the equalizer and levelling during a fade, float output and other speeds, and a fade that is longer than a short song.

## Decisions

| Decision | Options looked at | Reason |
|---|---|---|
| The hook is `CrossfadeLength` in `core:domain/premium`: a flow of the fade length in milliseconds. The core binding `NoCrossfade` gives 0 | The premium module brings its own sink wrapper | A hook may take and give only types of `core:domain`, and a sink wrapper is a Media3 type. The fade itself is plain sample work that the core needs anyway to keep gapless playback byte for byte, so the core does the mix and the premium module only turns it on and sets the length |
| `CrossfadeSink` (a `ForwardingAudioSink`) is always in front of the sink. `FadeMixer` holds the end and mixes on equal power curves (cosine out, sine in) | Wrap the sink only when a fade is set | With the length at 0 every buffer goes on untouched, so one player setup serves both builds and the length can change while music plays |
| `CrossfadePlan` works out the fade on the main thread from the player: the next item the player will play (shuffle and repeat all included), none at the end of the queue or with repeat one | Next period in the timeline, as in the trial | The player knows the real order. The playback thread reads the result from one volatile field |
| No fade between two songs of one album (same album name and album artist) | A fade between all songs | Album tracks are often made to run into each other. This keeps gapless albums as they are |
| No fade for a station, at a speed other than 1x, or for sound that is not 16 bit | Fade them too | A station has no end. Other speeds and float output were not tried, so they play as before |
| A fade takes at most half of each of the two songs, and at most 12 seconds | Cut the fade only when the song runs out | A short song keeps its middle, and the held end always fits |
| While it holds, the newest samples stay and only the samples that no longer fit go on | Pass the rest on unmixed, as in the trial | The trial passed the later sound on while the earlier sound was still held, so the order could change. The ring keeps the order |
| When the next song has another sample rate or channel count, the held end plays as it is, and the sink takes the new format only after it | Mix anyway | A mix across two formats would change the pitch of one song |
| The end of a song comes from its length in the timeline. A file that states a longer length than it has gets no fade | Use the silence check of `SkipSilentEnd` | The fade must start before the end, while `SkipSilentEnd` finds the end only after 3 seconds of silence. Such a file then ends as before and the next song follows with no gap |
| The equalizer and volume levelling stay after the mix, so the end of the old song gets the level of the new one during the fade | Run them before the mix | Since D91 the bands are not applied, so only the levelling gain changes, for a few seconds while the old song fades out. Manual test 152 checks that it cannot be heard |

## Results

- The core build plays as before: `NoCrossfade` gives 0 and every buffer passes the wrapper untouched.
- The Play build binds `CrossfadeLength` to the setting of the person and gets crossfade with no other change.
- `FadeMixerTest`, `WaitingSoundTest` and `CrossfadePlanTest` cover the hold, the order, the curves, clipping, a seek and every rule above. A person must still listen on a real phone before the feature ships (manual test 152).
