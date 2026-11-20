# ADR 0029: Cover pictures and error states in the car

- Status: accepted, 20 Nov 2026.
- Rules: P1, P4, P8.

## Context

Android Auto shows a cover only when the item names a `content://` or `android.resource://` address; a bitmap inside an item is not allowed. The items of this app named the album art address of MediaStore. The car is another app with no access to the audio files of the phone, so it cannot load that address. The car also needs a clear text when it has nothing to show: when the audio permission is not given on the phone, and when the library is empty.

Media3 passes a library error on to the car only for two error codes (authentication expired, and parental control), and only from the calls that read a list, an item or a search. A note sent with `sendError` reaches the car once and does not stop the screen.

## Decisions

| Decision | Options looked at | Reason |
|---|---|---|
| A content provider of the app (`ArtworkProvider`, `core:playback/artwork`) serves the cover of a song and of an album | Keep the MediaStore address. Put bitmaps into the items | The car cannot read MediaStore, and bitmaps in items are not allowed and break the 1 MB limit of a call |
| The picture is read with `ContentResolver.loadThumbnail` at 320 pixels and sent through a pipe as JPEG. Nothing is saved to disk | A cache folder of picture files | The phone already keeps thumbnails. No folder of ours means no path a request could reach, and nothing to clean up |
| The provider serves `/song/<number>` and `/album/<number>` for a read and refuses everything else: other paths, write modes, query, insert, update, delete | A wider path such as a file name | A number is turned into a MediaStore address and never into a path, so no request can name a file |
| Only this app, Android Auto and the Google app (checked by user id and signing certificate, `CertificateCallers`, as for the session) and apps of the system that hold `MEDIA_CONTENT_CONTROL` may read. The media panel of the phone is such an app and loads the cover by its address | Open to every app, as the sample of Google is | A cover tells what music a person has. An app with no audio permission must not learn that from us |
| Every item of the queue and of the car list names the address of the provider (`ArtworkAddress`). The phone lists keep the MediaStore address | Two addresses per item | The queue item is what the car, the lock screen and the notification read. The lists of the phone are drawn by the app itself |
| Without the audio permission every car list answers with the error "authentication expired" and the text "Allow access to audio files on your phone, when it is safe". The session passes library errors on as fatal ones | An empty list. A note only | It is the one error Media3 shows in the car for a list, and nothing can play without the permission. The error goes away with the next list that is read, and a list that was refused is told when the library changes |
| An empty library gives an empty Library tab and a one time note "There are no songs in your library yet" | An error. The four empty lists | Nothing is wrong, so the screen must not stop. Four lists that are all empty are four taps to learn the same |

## What follows

- "No internet" and "station not available" are not built: radio is out of the first release (decision D48).
- Station letter tiles are not built for the same reason.
- Which app of the car reads the pictures is not stated by Google. If covers do not show on a head unit (row 83 of `docs/MANUAL-TESTS.md`), the caller check of the provider is the first place to look.
- The permission state is read through `AudioAccess` in `core:domain`.
