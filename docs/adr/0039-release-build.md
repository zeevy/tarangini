# ADR 0039: Release build

- Status: accepted, 21 Nov 2026.
- Rules: A9.

## Context

The public repository must give a release build that is small, correct and can be built again from a tag. The signed Play bundle is built from the private repository and is not part of this.

## Decisions

| Decision | Options looked at | Reason |
|---|---|---|
| The release build type shrinks code and resources with R8, with the optimizing default rules | No shrinking. Shrinking without optimizing | 5.7 MB against 50 MB. The libraries bring their own keep rules, and R8 fails the build when a class it needs is missing |
| `app/proguard-rules.pro` only keeps line numbers and hides the file names | Keep rules for whole packages | A stack trace can be read with the mapping file. Nothing else was needed for the build |
| CI builds the release app in every run | Build it only for a release | A library update that breaks R8 shows in its own pull request |
| The version has one place, `appVersion`. The version code is `major * 1000000 + minor * 1000 + patch` | A version code kept by hand | It cannot be forgotten and always grows with the name |
| A tag `v<version>` starts the release workflow, which fails when the tag and `appVersion` differ. It runs the tests, builds the unsigned release app and keeps it with its SHA-256 and the R8 mapping file for 90 days | Publish a GitHub release with the file. Sign in the workflow | An unsigned file cannot be installed as an update, so it is for checks only. No signing key lives in this repository |
| The workflow runs on a hosted Linux runner, as CI does | The self hosted Mac runner of the first plan | The build needs nothing from macOS, and a public repository must not run on a private machine |
| The launcher icon is an adaptive icon: five bars of a sound wave on dark blue, with a one colour layer for themed icons | A picture per density | One vector serves every size and shape |
| Android backup stays off (`allowBackup="false"`, empty data extraction rules) | Back up the settings and the lists | The library index belongs to the files of one phone. Backup and restore to a file is its own feature |

## What follows

- The release build was built and its size recorded. It was not started on a device here: the shrunk app must be run once through the main flows before a release (row 113 of `docs/MANUAL-TESTS.md`, and the end to end test when it exists).
- Plain `http` is still allowed app wide for radio streams, which are out of the first release. Turning it off for the release is its own issue.
- The Play build signs and uploads from the private repository.
