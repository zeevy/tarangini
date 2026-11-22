# ADR 0051: Rail and now playing pane on large screens

- Status: accepted, 22 Nov 2026.
- Rules: A1, U2, U7, P1.
- Decision D80. It builds on ADR 0001 and ADR 0019.

## Context

On a tablet, an open foldable or a phone turned on its side, the bottom bar stretches across a wide window and the lists leave most of the width empty. Folding or turning the device makes the activity again, and the screen, its scroll place and the playback must stay as they were.

## Decisions

| Decision | Options looked at | Reason |
|---|---|---|
| The shell picks one of three layouts from the width of its window, at the limits of the Material window size classes: under 600 dp the bottom bar, from 600 dp a rail at the left, from 840 dp the rail and a now playing pane of 400 dp at the right of the tab screens. The pane also needs a window of at least 480 dp high: a large phone on its side is about 914 dp wide but only 411 dp high, too low for the cover and the buttons, so it gets the rail only | The `material3-adaptive` libraries (`NavigationSuiteScaffold`, `ListDetailPaneScaffold`) | The width is read with `BoxWithConstraints`, so no new library is needed. The libraries bring their own bar and rail, which would need the glass look added again |
| The pane is the now playing screen itself (`NowPlayingPane` in `feature:player`) with no back arrow, and the mini player is hidden while the pane shows | A smaller copy of the controls | One screen to keep right. The queue and the equalizer still open as their own screens from the pane |
| The rail and the pane show only on the tab screens. A screen outside the tabs (an album, Settings, now playing) fills the window | The rail on every screen | The same rule as the bottom bar, so the navigation does not change with the window |
| State across a fold comes from what already saves it: the navigation back stack, the saved list states and the playback service | Handling configuration changes in the activity | Nothing new to keep in step. A test makes the activity again and checks the open tab |

## What follows

- A list and its detail side by side (the albums and the open album) is not part of this change. The library screens keep one pane; that can come with a later issue.
- The check on a real foldable is row 146 of `docs/MANUAL-TESTS.md`.
