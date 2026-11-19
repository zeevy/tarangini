# Contributing to Swara Tarang

Thank you for helping. This page says how to build the app, what a change must follow and what we need from you before we can merge it.

## Before you start

- For a bug, open an issue with the bug report form. For a new feature, open an issue first and wait for a reply before you write code. A feature that does not fit [docs/FEATURES.md](docs/FEATURES.md) may not be merged.
- A security problem is never reported in a public issue. Follow [SECURITY.md](SECURITY.md).
- The paid features are built in a private module. A pull request that adds one of them to this repo, or that puts a free feature behind a payment, is not accepted.

## Build and test

You need JDK 21 and the Android SDK with platform 37. The steps are in the [README](README.md#build).

| Task | Command |
|---|---|
| Build the debug app | `./gradlew assembleCoreDebug` |
| All unit tests and the build checks | `./gradlew test` |
| Android lint | `./gradlew lint` |

Run all three before you open a pull request. They must pass.

## Rules for a change

[RULES.md](RULES.md) is the full list, and [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) says where code goes. The points that matter most:

- One issue, one branch, one pull request. Do only what the issue asks.
- Put the code in the right module. A feature module depends only on `core:domain` and `core:ui`. The build fails when a module breaks this.
- Every change comes with tests in the style of the tests next to it. A bug fix starts with a test that fails.
- Every new Kotlin, Java, Gradle and XML source file starts with the MPL-2.0 header. Copy it from any other file. `./gradlew checkLicenceHeaders` fails without it.
- Commit messages follow [Conventional Commits](https://www.conventionalcommits.org/) (`feat:`, `fix:`, `docs:` and so on).
- No ads, no analytics, no account and no secrets in the code.

## Third party code and libraries

The Play Store build mixes this code with closed code, so the licence of everything we add matters.

| Licence | Allowed |
|---|---|
| Apache-2.0, MIT, BSD, MPL-2.0 | Yes |
| GPL, AGPL (any version) | No |
| Anything else, or no licence | Ask in the issue first |

- A new library needs a clear reason in the pull request, and an exact version in `gradle/libs.versions.toml`.
- Do not copy code from a web page, a book or another project unless its licence is in the "Yes" row. Say where it came from in the pull request.
- Do not add a station list, station logos, album art, fonts or sounds that you do not have the right to share.

## Contributor agreement

Before we merge your first pull request, you agree to the contributor agreement in [docs/CLA.md](docs/CLA.md). In short: you keep the copyright in your work, and you give the project the right to use it and to license it under other terms later.

To agree, tick the agreement line in the pull request form. One time is enough for all your later pull requests.

> [!NOTE]
> The agreement text is a draft until a lawyer has reviewed it. If it changes, we ask you to agree again.

## The pull request

- Fill in the pull request form. Link the issue with `Closes #<number>`.
- Add screenshots for a change that people can see, in light and dark.
- A maintainer reviews it. Small pull requests are reviewed faster.
