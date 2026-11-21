# Premium hooks

The public code defines a few interfaces that the private premium module fills in. The `core` build binds each one to its own version, so it builds and runs alone. The `play` build adds the premium module (`:premium`, checked out in `premium/`), which binds the real versions through Hilt. Public code never imports the package `com.siliconcircuits.tarangini.premium`; `checkPremiumBoundary` and the module rules fail the build when it does.

| Hook | Package | Core version | How the premium module binds it |
|---|---|---|---|
| `PremiumGate` | `core.domain.premium` | `NoPremiumGate` (`app/src/core`): always locked | `@Binds` in a module of the `play` build; the core one is not part of that build |
| `CrashReporter` | `core.domain.premium` | `NoCrashReporter` (`app/src/core`): sends nothing | `@Binds`, the same way |
| `EqualizerBands` | `core.domain.premium` | `FiveEqualizerBands` (`app/src/core`): 60 Hz to 14 kHz | `@Binds`, the same way, for example with ten bands. The presets follow any band count |
| `StorageSource` | `core.domain.premium` | `LocalStorageSource` (`core:data`): the media of the phone, id `local` | `@Binds @IntoSet`: the source is added to the set. The local source stays in the set in both builds |

## One binding per build

The core versions of `PremiumGate`, `CrashReporter` and `EqualizerBands` are in `CorePremiumModule` under `app/src/core`. That folder is part of the `core` flavour only, so the `play` flavour must bind all three itself, in a module under `app/src/play` or in the premium module. A missing binding fails the Hilt step of the `play` build, never at run time.

```kotlin
@Module
@InstallIn(SingletonComponent::class)
internal abstract class PlayPremiumModule {
    @Binds
    abstract fun bindPremiumGate(gate: BillingPremiumGate): PremiumGate

    @Binds
    abstract fun bindCrashReporter(reporter: CrashlyticsReporter): CrashReporter

    @Binds
    abstract fun bindEqualizerBands(bands: TenEqualizerBands): EqualizerBands
}
```

## A storage source

A source has an `id` that never changes, lists one folder at a time with `list(folder)` and opens a file from an offset with `open(path, ByteRange)`. A path is the source's own text; only the source that gave it reads it again. Add it to the set:

```kotlin
@Module
@InstallIn(SingletonComponent::class)
internal abstract class CloudStorageModule {
    @Binds
    @IntoSet
    abstract fun webDavStorage(source: WebDavStorageSource): StorageSource
}
```

Rules for a source: HTTPS only, credentials encrypted with the Android Keystore and never logged, read only scopes. `open` blocks and is called on a background thread; `list` must not block the caller. See section 2.10 of `docs/FEATURES.md` and ADR 0049.

## Rules for every hook

- A core feature never asks `PremiumGate`.
- A hook takes and gives only types of `core:domain`, so the public code never sees a premium class.
- Every hook has a test of its core version in this repo.
