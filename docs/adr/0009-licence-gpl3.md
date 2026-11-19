# ADR 0009: GPL-3.0 with a contributor agreement

- Status: accepted, 19 Nov 2026. Replaces the licence part of decision D5.
- Rules: H3, K14, B2.

## Context

The app is open core: a free public core and a closed premium module that only the Play Store build includes. The core was under MPL-2.0, which lets anyone join the core with closed code. The aim is a licence under which nobody else can do that, while the premium features are still sold by subscription.

## Decisions

| Decision | Options looked at | Reason |
|---|---|---|
| The public core is under the GNU General Public License, version 3 only | Stay with MPL-2.0; GPL-3.0 "or any later version" | Under GPL-3.0 everyone who ships the core must publish all of what they ship. A fork cannot add closed features. "Version 3 only" keeps the terms fixed; the owner can still move to a later version, because the owner can relicense |
| The owner ships the Play build with the closed premium module under the owner's own terms (dual licensing) | Publish the premium code under GPL too; a separate closed add-on app; a paid server | The owner of a work is not bound by the licence the owner gives to others. The modules, the two builds and the premium boundary stay as they are. Open premium code can be built with the payment check removed. An add-on app can hold only two or three of the ten features. A server goes against decisions D15 and D16 |
| Every outside contributor agrees to `docs/CLA.md` before the first merge | A signed-off-by line in each commit; a sentence in the README | The owner may ship a contribution in the closed build only with the right to relicense it. A signed-off-by line does not give that right. A sentence in a README is no act of agreeing |
| For now the agreement is the tick line in the pull request form. A CLA bot with a required check is set up before outside contributions are invited | Set the bot up today | The bot is a GitHub app that the owner installs. Until then the owner is the only author |
| GPL and AGPL code of other people stays forbidden (rule H3) | Allow GPL libraries, as GPL-3.0 itself would | The Play build mixes in closed code. The owner can relicense only code the owner holds the rights to |
| The file header is three lines: the copyright line, the licence in words and the SPDX id `GPL-3.0-only` | The long notice of the GPL appendix | Short, and tools read the SPDX id. The header has no year, so a file made next year passes the same check |

## Result

- `LICENSE` is the official GPL-3.0 text from gnu.org. Every source file starts with the new header and `checkLicenceHeaders` looks for it.
- The allowed licences of shipped libraries are unchanged: Apache-2.0, MIT, BSD and MPL-2.0. All of them may be joined with GPL-3.0 code and with the closed build.
- The closed Google libraries (Play Billing, Cast, Crashlytics) stay in the premium module only. The `core` build has none of them, so anyone can build and share it under GPL-3.0 as it is.
- The name "Tarangini" and the logo are not covered by the licence. A fork needs another name.

## Open points

- A lawyer has not reviewed the agreement text, the dual licensing setup or the trademark note. This is check C9, and it must be done before the first outside contribution is merged.
- One merged contribution with no agreement makes the Play build a breach of that contributor's licence. The branch rule that requires the CLA check is the guard, once the bot is there.
- Code of the owner that was public under MPL-2.0 before this change (19 Nov 2026, for a few hours) stays usable under MPL-2.0 by anyone who took a copy then.
