# ADR 0036: Crash reporter interface

- Status: accepted, 21 Nov 2026.
- Rules: A9.

## Context

Crash reports go through a closed source service, which may only be in the private premium module (decision D10). The public code needs a way to tell that module about an error it caught and went on from. Such a report must never carry personal data, a file path or the address of a stream.

## Decisions

| Decision | Options looked at | Reason |
|---|---|---|
| `CrashReporter.report(problem, kind)` takes a `ReportedProblem` from a fixed list and a fixed name from the code. It takes no exception | Pass the exception. Pass a free text | The message of an exception often holds a path or an address, and the error of a player names the file. What cannot be passed cannot leak, and no review has to catch it |
| The public build binds `NoCrashReporter`, which does nothing | No binding in the public build | Every caller is the same in both builds |
| The scanner reports a scan that got no file list from the phone, with the name of the failure | Also report a missing permission | The permission is the choice of the person, not a fault |
| The playback service reports a player error with the name of its error code. A file that is gone is not reported | Report every error | The person removed that file, and the player skips it |

## What follows

- A new caller adds an entry to `ReportedProblem` and passes a constant as the kind.
- An app test reads the list of shipped libraries and fails when the public build holds a Firebase or Play services library.
- The premium module makes its own exception from the problem and the kind, so the reports group by both.
