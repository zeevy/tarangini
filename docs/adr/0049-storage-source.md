# ADR 0049: Storage source hook

- Status: accepted, 21 Nov 2026.
- Rules: A1, A9.
- Decision D78.

## Context

Playing from the person's own cloud storage (WebDAV, S3, OneDrive) is a premium feature in the private module. The public code must define the interface it plugs into before that work starts, with the files of the phone as its first version. The other premium hooks (`PremiumGate`, `CrashReporter`, `EqualizerBands`) exist already.

## Decisions

| Decision | Options looked at | Reason |
|---|---|---|
| `StorageSource` in `core:domain/premium`: an `id`, `list(folder)` for one folder, and `open(path, ByteRange)` that gives a `java.io.InputStream` from an offset with an optional length | A Media3 `DataSource` in the interface. A flow of all files at once | `core:domain` is plain Kotlin with no Android or Media3 type. A cloud folder can hold thousands of files, so it is listed one folder at a time. A range is what an HTTP range request and a seek both need |
| The sources are a Hilt set. The local source is in it in both builds; the premium module adds its sources with `@IntoSet` | One source bound with `@Binds`, replaced by the premium one | The phone's files stay when cloud sources are added |
| `LocalStorageSource` lists from the same media index query as the library scan (`AudioFileSource`) and opens a file by its media id through the content resolver; a file moves to the offset at once | Read file paths from the disk | The app has no file path access on new Android versions, and the same query means the same files |
| A path of the local source is the media id; anything else is refused as not found, and so is a file the app may not read | Accept a content address | No other file can be opened through it. It can open any audio file of the media index, also one in a folder the library settings hide, which the app may read anyway |
| For now the library scan and the player keep their own path for the phone's files: the scan reads `AudioFileSource`, the player reads `content:` addresses | Move both onto `StorageSource` now | The current paths are tested on real phones, also for gapless playback and seeking. Moving them gives nothing until a second source exists, and that work belongs to the first cloud source, together with its address form for the queue |

## What follows

- The first cloud source also brings a way to name its files in the queue and a player data source that reads them through `open`, with the disk cache of section 2.10 of `docs/FEATURES.md`.
- How the premium module binds each hook is in `docs/PREMIUM-HOOKS.md`.
