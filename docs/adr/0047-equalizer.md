# ADR 0047: Our own equalizer and bass boost

- Status: accepted (the band part of the screen is changed by ADR 0056), 21 Nov 2026.
- Rules: A9, P1.
- Decision D76. It closes the trial build of check C5 for the equalizer (decision D12).

## Context

The equalizer must sound the same on every phone, so it does not use the `Equalizer` and `BassBoost` effects of Android. Media3 has no equalizer of its own. Before the screen was built, the processor was tried for its cost, for clipping, for clicks on a change and for gapless playback.

## Decisions

| Decision | Options looked at | Reason |
|---|---|---|
| An `AudioProcessor` in the audio sink of the player (`EqualizerProcessor`), given by `GaplessRenderersFactory` | The effects of Android. A second player for the sound | One player for the phone and the car, so the car hears the same. The effects of Android differ per phone |
| Five bands at 60, 230, 910, 3,600 and 14,000 Hz, each a bell (peaking) filter with Q 0.9. The bass boost is a low shelf below 100 Hz, 0 to 12 dB. The formulas are the ones of the "Audio EQ Cookbook" | Shelves at the two ends. Bands one octave apart | The bands are the ones most five band equalizers use, and two bells at 0.9 meet with no gap. The cookbook formulas are known and can be tested against worked numbers |
| The sound is turned down first by the most the filters lift any tone (measured at 128 tones from 20 Hz up, and at each band) | A limiter after the filters. Let it clip | A song at full level does not clip at full boost, and the sound is not squeezed. The cost: with a boost the whole sound is a little lower |
| A change of the setting is taken at the next buffer. For 20 ms the old and the new filters both run and the sound fades from one to the other; the new filters start from the state of the old ones | Change the numbers of the filters at once | A change at once makes a click. The fade costs one extra chain for 20 ms |
| The number of samples never changes. A flat or off setting copies the samples as they are | | Gapless playback depends on the length of each track |
| Presets are curves over the octaves. Each band takes the value of the curve at its centre | A table per number of bands | The premium module can offer 10 bands through `EqualizerBands`, and the same presets work for them |
| The setting is saved in the one settings store (`EqualizerRepository`). The playback service follows it with a coroutine, and the screen saves each move of a slider | The screen talks to the service through a custom command | The setting is the same after a restart and in the car, with no connection to keep |
| The screen opens from the menu of now playing | A row in Settings | The equalizer is changed while a song plays |

## Trial numbers

Measured with the unit tests on the build machine (a laptop CPU, the JVM):

| Check | Result |
|---|---|
| Cost of 60 s of stereo 44.1 kHz sound, five bands and the bass on | About 0.2 s, so about 0.35 % of one core |
| Full boost of every band and of the bass, a tone at 90 % of full level, at 40 Hz to 14 kHz | Peak under 95 % of full level: no clipping |
| A band moved by 12 dB while a 1 kHz tone plays | No step between two samples larger than the tone makes: no click |
| Output length against input length, in parts of 1,000 samples | The same |
| The filter numbers against the worked values of the cookbook | Equal to 8 places |

A phone CPU is a few times slower than a laptop, so the cost on a phone is expected to stay near 1 % of one core. The result is a go: the processor is part of the app.

## What follows

- The cost and the battery use on two real phones, for files and for radio, are rows 136 and 137 of `docs/MANUAL-TESTS.md`. They are not measured yet. If a phone shows a real cost, the filters can move from `Double` to `Float`.
- A radio stream goes through the same processor.
- A change is heard after the audio the sink holds already, about a quarter of a second.
- The checks of sound on a real phone are rows 136 to 139 of `docs/MANUAL-TESTS.md`.
