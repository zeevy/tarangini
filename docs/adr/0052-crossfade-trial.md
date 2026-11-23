# ADR 0052: Crossfade trial

- Status: accepted, 23 Nov 2026.
- Rules: H1, P1.
- Decision D82. Check C5 for crossfade. It builds on ADR 0007 and ADR 0047.

## Context

Media3 has no crossfade. A crossfade plays the end of one song and the start of the next at the same time, one fading out and the other fading in. It must not break the rule of one player and one session for the phone, the car and the notification, and gapless albums must still play with no gap when it is off. Crossfade is a premium feature (section 2.10 of `docs/FEATURES.md`), so this trial decides only whether and how it can be built.

## How it was measured

- Three test files of 6.06 seconds, each one tone (440, 660 and 880 Hz) at the same level, MP3 at 128 kbit/s. Each tone can be measured on its own in the output, so the fade shows as one tone going down while the next goes up.
- The gapless album of ADR 0007 (`with-delay-header-*.mp3`) for the gapless check.
- A trial build wrote the sound the player hands to the system into WAV files, on the Android 17 emulator. The files were cut into 250 ms windows and each tone and the RMS level measured. The trial code is not in the app.

## Results

| Way | What happened |
|---|---|
| A Media3 `AudioProcessor` that holds the end of a song and mixes it into the next | Does not work. The audio sink of Media3 1.11.1 drains and resets its whole processor chain at every song change, even when the format stays the same. The held end was lost: each song lost its last 1.97 seconds and no fade happened |
| A wrapper in front of the audio sink that holds the end of a song and mixes it into the first buffers of the next | Works. Two crossfades of 2 seconds and two of 4 seconds, no underrun. The tones show an even fade: at the middle both are at 66 % of full level, and the RMS level stays between 10,715 and 11,011 through the whole queue. The output is 14.16 seconds for 18.18 seconds of songs, the two overlaps less. The player moves to the next song 0.16 to 0.28 seconds after its fade-in starts, so the time on the screen stays right: the sink sees the next song start early and moves its clock on by itself |
| Two players, the second one started when the first has 2 seconds left, with volume ramps | The second player starts sound 74 to 80 ms after it is asked, steady over 5 rounds, so the overlap can be timed. But the session, the queue, the notification and the car all follow one player. Two players need a stand-in player that joins both for the session, with two decoders and the audio focus to keep in step |
| Gapless with crossfade off | The output is the same byte for byte as without any crossfade code, for the processor and for the sink wrapper |

## Decisions

| Decision | Options looked at | Reason |
|---|---|---|
| Crossfade is a go, built as a wrapper in front of the audio sink of the one player | An audio processor; two players | The processor way cannot work in Media3 1.11.1. Two players break the rule of one player and one session and need a stand-in player for the session. The wrapper keeps one player, so the phone, the car and the notification stay as they are |
| The renderer tells the wrapper how much of a song is left, from the length in the timeline, and when the next song starts | Read the end from the file in the wrapper | The renderer already knows the song that plays (ADR 0047) and its length; the wrapper only sees bytes |
| The fade uses equal power curves (cosine out, sine in) | Straight lines | The level stays even through the fade, as the RMS numbers show |

## What follows

- A build issue for crossfade as a premium feature: the public core gets a hook for the sink wrapper and the song end, the crossfade itself is in the premium module.
- The end of a song comes from its length in the timeline. A file that states a wrong length (ADR 0024) starts its fade at the wrong place; the build must use the same end as `SkipSilentEnd`.
- "Next song" in the trial is the next period in the timeline. The build must use the order the player plays: shuffle, repeat one and the end of the queue.
- The equalizer and volume levelling run inside the sink, after the mix. During a fade the end of the old song gets the preset and level of the new song. The build must decide if this can be heard.
- The trial covered 16 bit sound at 1x speed. Float output and other speeds were not tried.
- A gapless album should not crossfade: two tracks that run into each other would be mixed. The build needs a rule, such as no crossfade between tracks of the same album.
- A crossfade that is longer than the planned end held more than its room on the 2 second gapless tracks and played the rest unmixed. The build must hold the whole fade, or shorten the fade for a short song.
- "Sounds right" was measured, not heard: the emulator has no sound device. A person must listen on a real phone before the feature ships.
