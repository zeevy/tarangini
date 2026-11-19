# ADR 0002: Screenshot tests with Robolectric and Roborazzi

- Status: accepted. Decided by dev in auto mode, 19 Nov 2026.
- Rules: T4, U9, B1.

## Context

Every screen needs screenshot tests in light and dark (T4). CI has no emulator yet, so the tests must run on the JVM with `./gradlew test`.

## Options

| Option | For | Against |
|---|---|---|
| Robolectric with Roborazzi | Stable. Runs in the unit test task. The same Robolectric setup also runs the Compose behaviour tests (taps, Back) | Robolectric runs Android 16 (API 36) at most today, one version behind the target |
| Paparazzi | Stable and fast | Draws layouts only. It cannot run taps or navigation, so behaviour tests would need a second tool |
| Compose Preview Screenshot Testing (Google) | Uses the previews | Still alpha, which B1 does not allow |

## Decision

- Robolectric with Roborazzi. The libraries are test only and are added by the `tarangini.android.compose` convention plugin.
- The saved pictures live in `src/test/screenshots/` of the module and are committed.
- `./gradlew test` compares with the saved pictures and fails on a difference or a missing picture. `./gradlew test -PrecordScreenshots` saves new pictures. A change to a saved picture is looked at in the PR.
- `config/robolectric/robolectric.properties` sets the Android version for every module.

## Result

- Robolectric downloads its Android runtime from Maven Central the first time the tests run on a machine.
