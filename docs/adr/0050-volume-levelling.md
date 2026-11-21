# ADR 0050: Volume levelling with ReplayGain tags

- Status: accepted, 21 Nov 2026.
- Rules: A9, P1.
- Decision D79. It builds on ADR 0047 and ADR 0048.

## Context

Songs from different albums are mastered at different loudness, so a mixed queue makes the person reach for the volume again and again. Many music tools write ReplayGain tags into the files: a gain in dB and the loudest sample, for the track and for its album. Media3 reads the tags but does nothing with them. The change must keep gapless playback, make no click at the start of a song and never make a song clip.

## Decisions

| Decision | Options looked at | Reason |
|---|---|---|
| The tags are read by the player from each file as it opens it, in `GaplessAudioRenderer.onInputFormatChanged`, from the ID3 `TXXX` frames of an MP3 and the iTunes `----` atoms of an AAC or M4A file | Read them during the library scan and store them in Room, as the issue first said | The extractors read these tags anyway before the first sound, so this costs nothing and needs no new database version or scan of every file. A file that was changed plays with its new tags at once |
| The gain is part of the equalizer target (`EqualizerTarget.levelDb`) and goes into the pre-gain of the same processor, chosen at the same renderer hook as the genre preset | A second audio processor | One processor means one place that fades: a change of level at the start of a song uses the same 20 ms fade and the same sample count, so gapless playback stays as it is |
| A mode in the equalizer settings: off (the first value), the same loudness for every song (track gain), or for every album (album gain). Without a tag of the asked kind the other kind is used | A switch with track gain only | Album gain keeps the quiet and loud parts of an album as the artist made them; track gain suits a shuffled mix. Both are what other players offer |
| No clipping: a lift stops where the peak of the tag reaches full scale; with no peak the song is only turned down. A gain is held within -24 and +12 dB | A limiter that bends the loud samples | Turning the whole song down or up by a fixed amount does not change the sound. A limiter changes it and costs more |
| Levelling works also while the equalizer is off, and the choice sits on the equalizer screen under the bass boost | A row in Settings | The equalizer screen, opened from the menu of now playing, is where the sound is set. The levelling rows do not turn off with the equalizer switch |
| A song with no tags plays as it is | A loudness measure of our own | Measuring loudness needs the whole file before it plays. That can come later |

## What follows

- The first song of a queue gets its tags a moment after its start is told, but before its first sound, so it starts at its level. Only tags that come after the sound has started are reached with the 20 ms fade.
- A radio stream has no tags and plays as it is.
- The phone checks are rows 143 to 145 of `docs/MANUAL-TESTS.md`.
