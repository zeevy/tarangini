# ADR 0059: The look of a hardware player

- Status: accepted, 24 Nov 2026. Adds to ADR 0053.
- Rules: U9.
- Decision D90.

## Context

The screens used round cards on paper, the same in every place. The new design takes the look of a hardware music player: a display, lamps and small corners. Every screen moves to it in steps, so the parts it needs come first.

## Decisions

- The Material colour scheme keeps the screens working: `background` is the ground, `surfaceContainer` is the frame colour of cards and bars.
- `LookColors` holds what Material has no place for: the display (`lcd`, its texts, accent, glow, edge and tag), the plain list card, the lamp that is off, the heart, the delete colour and the snackbar. `TaranginiTheme` provides it, and `lookColors` reads it.
- The display is dark in both looks. In Night the delete colour is a lighter red, because the red of Daylight does not reach 4.5:1 on the dark frame.
- Shared parts in `core:ui/component`: `Lamp`, `LedChip` (on the ground or on the display, as a tab, a switch or a button, always 48 dp to touch), `FrameCard`, `LcdPanel`, `ListCard` and `LedSwitch`.
- Corners are 4 dp for a chip, 6 dp for the display and 8 dp for a card.
- The bottom bar and the rail share `TabItem`: a pill behind the icon of the current tab, and the name under it.

## Results

- `ColorsTest` checks every text colour of the new parts for 4.5:1, and lit lamps and icons for 3:1, in both looks.
- Screens that are not rebuilt yet already take the new ground and frame colours.
