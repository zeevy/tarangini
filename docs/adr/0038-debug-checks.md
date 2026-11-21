# ADR 0038: Checks of a debug build

- Status: accepted, 21 Nov 2026.
- Rules: A9.

## Context

The first release must be stable in daily use. Two kinds of fault do not show in a unit test: work on the main thread that makes the screen wait, and objects that stay in memory after their screen is gone.

## Decisions

| Decision | Options looked at | Reason |
|---|---|---|
| A debug build switches StrictMode on at the start: every thread check, and the checks for a file, cursor, receiver or screen left behind | No StrictMode. StrictMode in every build | It costs nothing in a debug build and names the line at fault. A release build must not carry it |
| StrictMode only writes to the log | End the app on a finding | A library that reads a file on the main thread would end the app of every developer |
| LeakCanary 2.14 is a library of the debug build only | Look for leaks by hand with the profiler | It starts by itself, watches screens, ViewModels and services, and shows a notice with the path of the leak |
| The switch is the function `installDebugChecks()`, which exists once in `app/src/debug` and once, empty, in `app/src/release` | A check of a debug flag at run time | The release build then holds no line of the debug code at all |

## What follows

- The debug build lists LeakCanary on its open source licences screen. The release build does not have it.
- LeakCanary asks for write access to shared files and for notifications. The debug manifest of the app removes the write access, so its files stay in the folder of the app. The notification permission comes from the manifest of LeakCanary and stays in the debug build, for its notice.
- LeakCanary does nothing in the JVM tests: it switches itself off when JUnit is on the class path.
- A StrictMode line in the log of a debug build is a finding: fix it, or write down why it is from a library.
- The long runs on a real phone are rows in `docs/MANUAL-TESTS.md`.
