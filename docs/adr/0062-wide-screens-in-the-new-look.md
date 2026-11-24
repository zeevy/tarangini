# ADR 0062: Wide screens in the new look

- Status: accepted, 24 Nov 2026. Changes ADR 0051.
- Decision D94.

## Context

ADR 0051 gave wide windows a rail and, on a tablet held flat, the player pane. The screens inside kept the layout of a phone. With the new look the player, the library and the rail get their own wide layouts.

## Decisions

- The rail has the mark of the app at the top, the three tabs in the middle and a Settings button at the foot, so Settings is one tap away on a wide screen as it is from the Library header on a phone.
- A window at least 600 dp wide and wider than it is high (a phone on its side, a tablet held flat) shows the player card and Up next side by side. The card takes the height that is left under the header, as it does above Up next on a phone.
- A window at least 600 dp wide and 840 dp high (a tablet held upright) keeps the card above Up next, with a larger time (52 sp) and a taller visual (104 dp).
- When the songs card of the library is at least 640 dp wide, the songs show as a table with Title, Artist, Album and Time columns under a header. `LookRow` takes the extra columns, and a row with no time or no heart keeps their place, so the columns stay in line. The other lists keep their rows, and the albums stay a grid.

## Results

- Screenshot tests cover a phone on its side, a tablet held flat and upright and an open foldable, in both looks.
- The table is built only for the library songs; Favourites, an album and search keep the phone rows.
