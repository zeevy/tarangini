# ADR 0054: Level meter

- Status: accepted, 23 Nov 2026.
- Decision D85.

## Context

The now playing card (#162) has a left and a right level meter. A meter that is only an animation would lie about the sound, and the meter must not change what is heard or do real work while the screen is closed.

## Decisions

| Decision | Options looked at | Reason |
|---|---|---|
| A `LevelMeterProcessor` in the sink chain of the one player, right after `EqualizerProcessor`. It reads each buffer and hands it on unchanged | The platform `Visualizer`. A level worked out from the file. An animation | `Visualizer` needs the record audio permission and measures the whole output of the phone. Our own processor sees what the player sends after the equalizer and volume levelling, needs no permission, and adds nothing to the gapless path (ADR 0007) |
| The peak of each channel, held for 40 ms windows and read about 25 times a second, in dB from -60 to 0. A level falls at most 20 dB a second | RMS level. A faster rate | A peak meter shows what clips, as the drawing has it. 25 readings a second look smooth, and the fall keeps a short peak in view so the bar does not jump |
| `LevelMeter` is one singleton, bound as `AudioLevels` from `core:domain`. The service takes a new processor from it for its player; screens collect `levels()` | Send levels over the media session | The service runs in the process of the app, so a shared object is enough. The session would send 25 messages a second to every caller, also to the car |
| The processor measures only while `levels()` is collected; otherwise it only copies the buffer, as the equalizer does when it is flat. One loop reads the peaks for every screen that watches | Measure always. A loop per screen | Only the copy is left while the now playing screen is closed. A loop per screen would take the peaks from each other |

## What follows

- The meter reads the sound before the volume and the speed change of the player, so the fade of the sleep timer does not show on it.
- A mono song gives the same level on both sides. More than two channels show the front left and right.
- A pause, the end of the queue or an error sends no buffers, so the levels fall to silence within 3 seconds, as on a real meter.
- Row 149 of `docs/MANUAL-TESTS.md` checks the meter on a phone.
