# ADR 0024: A file with less sound than it states

- Status: accepted, 20 Nov 2026.
- Rules: P1, P2.

## Context

An MP3 that was cut short can keep the header of the whole song. A ringtone made from a song is the common case: the file I found holds 60 seconds of sound, and its header states 138 seconds. Media3 believes the header. After the last sound it stays in the playing state with no sound until the stated end, and the position creeps at less than half the speed of the clock. Nothing fails, so no error tells about it. To the person the app looks like it plays while the phone is silent.

The place that is saved for resume can also fall inside that silence. Starting there fails with "read position out of range", because the file has nothing at that place.

## Decisions

| Decision | Options looked at | Reason |
|---|---|---|
| While a file plays, the position is compared with the clock every 2 seconds. When it moved less than three quarters of that time twice in a row, with more than 2 seconds left, the sound has ended: the next item plays, or playback stops at the start of the last item (`SkipSilentEnd`) | Read the header of every file before it plays and work out the real length. The Media3 option that takes the length from the file size | The watch needs no knowledge of a file format and also covers other formats with the same fault. Reading headers ourselves is a second parser to keep right. The file size option gives wrong lengths for files with a changing bitrate |
| One slow window is not enough | Act on the first slow window | A new audio route, such as Bluetooth, can hold the position for about a second while the sound is fine |
| A failure with "read position out of range" on a file, at a place after its start, is treated the same way | Start the same item from its beginning | The place was after the last sound, so the item has ended for the person |
| The file is not changed | Offer to repair the header | The app has read access only. A repair needs a write request per file and changes files of the person, so it is a feature to decide on its own |

## What follows

- The person hears about 4 seconds of silence before the next item starts.
- With repeat on one item, such a file starts again after that silence.
- The screens still show the stated length for such a file, since it comes from the file itself.
- A stream is not watched. It has no stated end, and its pauses are buffering.
- Check 63 in `MANUAL-TESTS.md` needs a real phone and a file that was cut short.
