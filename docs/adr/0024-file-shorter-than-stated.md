# ADR 0024: A file with less sound than it states

- Status: accepted, 20 Nov 2026.
- Rules: P1, P2.

## Context

An MP3 that was cut short can keep the header of the whole song. A ringtone made from a song is the common case: the file I found holds 60 seconds of sound, and its header states 138 seconds. Media3 believes the header. After the last sound it stays in the playing state with no sound until the stated end, and the position creeps at less than half the speed of the clock. On a second such file the position went on at the speed of the clock, so the position tells nothing safe. Nothing fails, so no error tells about it. To the person the app looks like it plays while the phone is silent.

The place that is saved for resume can also fall inside that silence. Starting there fails with "read position out of range", because the file has nothing at that place.

## Decisions

| Decision | Options looked at | Reason |
|---|---|---|
| The audio renderer notes each time the decoder gives sound (`LastSound`). While a file plays, a listener looks at that once a second. When no sound came for 3 seconds and more than 3 seconds are left to the stated end, the sound has ended: the next item plays, or playback stops at the start of the last item (`SkipSilentEnd`) | Compare the position with the clock (the first try: it missed a file whose position went on at full speed). Read the header of every file before it plays and work out the real length. The Media3 option that takes the length from the file size | The decoder is the one place that knows whether sound is left, whatever the format and whatever the position does. Reading headers ourselves is a second parser to keep right. The file size option gives wrong lengths for files with a changing bitrate |
| A silence shorter than 3 seconds is not the end | Act at once | A busy phone or a new audio route can hold the decoder for a moment while the file is fine |
| A failure with "read position out of range" on a file, at a place after its start, is treated the same way | Start the same item from its beginning | The place was after the last sound, so the item has ended for the person |
| The file is not changed | Offer to repair the header | The app has read access only. A repair needs a write request per file and changes files of the person, so it is a feature to decide on its own |

## What follows

- The person hears about 3 seconds of silence before the next item starts.
- With "repeat one" such a file gives way to the next item of the queue, and stops at its start when it is the only one. Starting it again would repeat the silence for ever on a file that has no sound at all.
- The screens still show the stated length for such a file, since it comes from the file itself.
- A stream is not watched. It has no stated end, and its pauses are buffering.
- Check 63 in `MANUAL-TESTS.md` needs a real phone and a file that was cut short.
