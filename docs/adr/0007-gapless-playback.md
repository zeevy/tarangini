# ADR 0007: Gapless playback of MP3 and AAC

- Status: accepted. Decided by dev in auto mode, 19 Nov 2026.
- Rules: H1, K14, T6.

## Context

Album tracks must play with no gap. An MP3 or AAC encoder adds silence at the start and at the end of every file. A player can cut it only when the file says how long it is: an MP3 file in the LAME part of its first frame, an AAC file in MP4 in its edit list (or in the iTunes gapless tag). Media3 reads these fields. This had to be proven, not assumed.

## How it was measured

- The test albums are in `core/playback/src/test/resources/gapless`. Each has three tracks cut from one 441 Hz tone, 88,200 samples a track (2 seconds at 44.1 kHz). A gap, or a sample too many or too few, breaks the wave at the track change.
- The tracks were encoded with GStreamer (`lamemp3enc` at 64 kbit/s and `avenc_aac` in `mp4mux`). The files `with-delay-header-*.mp3` got a LAME header with delay 576 and padding 1,080, as the LAME encoder writes it. The files `with-edit-list-skip-*.m4a` got an edit list that skips the first 1,024 samples, and a sample table that counts the last frame in full. The `no-...` files are the same sound with these fields left out.
- A trial build wrote the sound that the player hands to the system into a file, on the emulator (Android 17, API 37, decoder `c2.android.mp3.decoder`). The trial code is not in the app.

| Album | Before the fix | After the fix |
|---|---|---|
| MP3 with LAME header | 12 ms of silence at each track change, and the first 12 ms of each track cut off | No silence, the wave goes on in step |
| MP3 with no delay header | 36 ms of silence | The same: a known limit |
| M4A with edit list | No silence, the wave goes on in step | The same |
| M4A whose edit list skips nothing | 41 ms of silence | The same: a known limit |

## Decisions

| Decision | Options looked at | Reason |
|---|---|---|
| The audio renderer moves 529 samples from the start silence back to the end silence, for MP3 on the two MP3 decoders of Android (`c2.android.mp3.decoder`, `OMX.google.mp3.decoder`). The code is `Mp3DecoderDelay.kt` in `core:playback/player` | Leave it and write it down as a limit; change the file reader instead | An MP3 decoder gives out 529 samples before the first real one. Media3 1.11.1 adds them to the start silence of a file from LAME or FFmpeg. The decoder of Android cuts them by itself, so they were cut twice. The renderer is the only place that knows which decoder plays the file |
| Any other decoder keeps what Media3 says | Apply it to every MP3 decoder | A phone maker's decoder may not cut its own delay. Wrong by 12 ms in either direction is the same size of error, so only the decoders that were read and measured are changed |
| No setting for gapless playback | A switch in Settings | There is nothing to choose: cutting encoder silence is always right |
| A test pins what Media3 does (`GaplessInfoTest`) | Trust the next Media3 version | When Media3 changes how it handles the decoder delay, the test fails and names the code that must go |

## Known limits

- An MP3 file with no LAME (or Lavf, Lavc) header plays with a gap of about 36 ms at each end. Files from old or simple encoders are like this. Nothing in the file says how long the silence is, so the app cannot cut it.
- An AAC file in MP4 with no edit list and no iTunes gapless tag plays with a gap of about 41 ms. A raw `.aac` file (ADTS) never has these fields, so it always has the gap.
- An MP3 file whose header was written by an encoder other than LAME or FFmpeg, but in the same place, gets the 529 samples moved although Media3 did not add them. Its track changes are then wrong by 12 ms. How many such files exist was not measured.
- An MP3 file whose header states less than 529 samples of end silence loses its last samples up to that number (at most 12 ms). Media3 has already thrown the stated number away at that point, so the app cannot tell. This can happen; how often was not measured.
- An MP3 decoder of a phone maker is not corrected. If it also cuts its own delay, that phone still has the 12 ms tick, and its decoder name must be added to the list in `Mp3DecoderDelay.kt`.
- An MP3 file with a VBRI header has no silence fields that Media3 reads, so it plays like a file with no delay header.
- HE-AAC files were not tried. The test album is AAC-LC.
- What the decoder does right after a seek was not measured.
- The last track of a queue keeps its end silence. Nothing comes after it, so nobody hears it.
- Shuffle, repeat one and a seek do not change this: the silence is cut whenever one track runs into the next.
- "No gap can be heard" was measured, not heard. The emulator runs with no sound device. A person must listen on a real phone: checks 16 to 18 in `docs/MANUAL-TESTS.md`.
- Crossfade is not part of this. It is premium and needs its own trial build (check C5).
