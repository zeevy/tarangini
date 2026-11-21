# ADR 0037: Accessibility checks in the tests

- Status: accepted, 21 Nov 2026.
- Rules: U4.

## Context

Every screen must work with TalkBack, with large fonts and on small phones. A run by hand finds faults, but only when someone does it. The Compose tests run on the JVM with Robolectric, where the accessibility checks of the Compose test library do nothing: a test with a 10 dp button and no label passed with them on.

## Decisions

| Decision | Options looked at | Reason |
|---|---|---|
| The Compose test of each main screen calls `checkAccessibility()`, which runs the Accessibility Test Framework through the Roborazzi checker. A warning fails the test | `enableAccessibilityChecks()` of the Compose test library. Tests on an emulator in CI | The Compose one does nothing on the JVM. An emulator makes CI slow and is one more thing that breaks. The Roborazzi checker caught the faulty button in a trial |
| A content test shows the content inside the frame of its screen | Show the content alone | The frame paints the dark background and sets the text colour. Without it the contrast check measures the light test window |
| The buttons of the mini player are measured in their own test (48 dp), not with the framework check | Leave the mini player out | On the JVM the framework reads these buttons as 46 dp high, while Compose gives them a 48 dp touch area. The own test checks that area |
| Each main screen has a saved picture at font scale 2 on a 320 dp wide phone, and one at 640 by 320 dp | Check the large font only by hand | A text that is cut or lies over another shows in the review of the picture |
| With a font scale of 1.5 or more a text of a list row may take two lines (`rowTextLines`) | One line always. Two lines always | At scale 2 a 320 dp phone fits about ten letters on a line. At the normal size one line keeps the rows even |

## What follows

- A new screen gets a call of `checkAccessibility()` in its content test and a picture at the largest font.
- The helper is a small file in the test folder of each module, as the other test helpers are. Sharing them is its own piece of work.
- The checks do not replace a run with TalkBack: the order of the items and what an action says can only be judged by a person. Those runs are rows in `docs/MANUAL-TESTS.md`.
- The mini player keeps one line of text at every font size, so the bar keeps its height.
