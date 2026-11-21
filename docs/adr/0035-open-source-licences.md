# ADR 0035: Open source licences screen

- Status: accepted, 21 Nov 2026.
- Rules: A9, U4.

## Context

The app ships about 200 libraries, nearly all under Apache-2.0, and the Manrope font under the SIL Open Font License. Their licences ask that the text goes with the app. The build already runs a licence check (licensee) that fails when a library has a licence outside the allowed list or no known licence at all.

## Decisions

| Decision | Options looked at | Reason |
|---|---|---|
| The list on the screen is the report of the licence check, put into the app as the asset `app/cash/licensee/artifacts.json` | A list kept by hand. A second plugin that writes its own list. The Google Play services licences plugin | The check and the screen read one list, so they cannot differ, and a new library shows with no other step. The Play services plugin brings a closed library, which the `core` build must not have |
| Each licence text ships once, as `licences/<id>.txt` in the assets of `core:ui`, taken from the SPDX licence list | One text per library, downloaded at build time. A link to a web page | 200 libraries share a handful of texts. The build stays offline and the texts can be read with no network |
| A test in the app fails when the list names a licence with no text file. Only the texts in use ship: Apache-2.0, BSD-3-Clause with the copyright line of its one library, MPL-2.0 and the font licence | Ship a text for every licence of the allow list | MIT and BSD ask for the copyright line of the library, which a template text does not have. So the first library under such a licence fails CI until its text, with that line, is added |
| The font is added to the list in code | A hand written second list file | It is the one part that is not a library of the build |
| A library with two licences is listed under the first | Show both | None of the libraries has two today, and the first is the one the check accepted |
| The screen starts with the notice of the app: GPL-3.0 (version 3 only), no warranty, and where the source is | A separate "About" screen | The first plan named an MPL-2.0 notice. The core is under GPL-3.0 since decision D31, so the notice says that |
| The licence text opens inside the same screen, and Back returns to the list | A route per licence | The text needs nothing from the navigation, and an id from a route would be one more input to check |
| The list is read through `LicenceCatalog` in `core:domain`, with the reader in `core:data` | Read the assets in the feature | A feature depends only on `core:domain` and `core:ui` |

## What follows

- A debug build lists its debug only libraries as well. The Play build lists what that build ships.
- Turning the asset on makes the build read the description file of every library of each build type, so those files are in `gradle/verification-metadata.xml`.
- A library under a licence that is new to the app needs the id in the allow list of the application build plugin (if it is not there) and the text as `core/ui/src/main/assets/licences/<id>.txt`, with the copyright line of the library where the licence asks for it.
- The licence check now runs before the assets of every build are merged. The Play build therefore does not assemble until its closed libraries are allowed or left out in the check on purpose.
- The `NOTICE` files of the libraries are collected by the build (D72): the task `collect<Build>Notices` of the application build plugin reads the downloaded file of every library of the build, takes a `NOTICE`, `NOTICE.txt` or `NOTICE.md` from the top or from `META-INF` (for an Android library also from the jar inside it) and writes them into the asset `licences/notices.json`. The screen shows the notice of a library above its licence text. A new library with such a file needs no step by hand. A test of the app fails when a collected notice belongs to no library of the list.
- The id of a licence is checked against a plain pattern before it becomes part of a file name.
