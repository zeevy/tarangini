# ADR 0056: Equalizer faders on now playing

- Status: accepted, 23 Nov 2026. Changes the band part of ADR 0047. Replaced by ADR 0060 on 24 Nov 2026.
- Rules: U1, U7, U8.
- Decision D87.

## Context

The new now playing screen shows the equalizer under the card of ADR 0055. The equalizer screen had one horizontal slider per band and chips for the presets.

## Decisions

| Decision | Options looked at | Reason |
|---|---|---|
| One `EqualizerPanel`: a title row ("Equalizer", "Bass · 5 bands", Reset, the switch), one vertical fader per band in a strip with the gain above and the band below, and the presets. Now playing and the equalizer screen both show it, each with the state of `EqualizerViewModel` | A second, smaller equalizer for now playing | One panel is drawn, tested and saved one way. The equalizer screen keeps bass boost, volume levelling and the preset per genre under it |
| A band moves in steps of 1 dB from -12 to +12 | Steps of 0.5 dB, as on the old sliders | A fader of 132 dp has room for 25 clear stops; half steps could not be told apart by a finger or by TalkBack |
| Each strip is at least 48 dp wide. When the bands do not fit, the faders scroll sideways and start with the lowest band | Make the strips thinner for 10 bands | 48 dp is what a finger needs. Five bands always fit on a phone, so the core build never scrolls |
| The presets wrap onto a second line | One row that scrolls, as drawn | In a scrolling row the picked preset could be out of view |
| Reset picks Flat | Reset to the last saved preset | Flat is the one state that is the same for every song |
| With the equalizer off the panel is dimmed and only the switch works | Hide the faders | The bands stay in view, so the person sees what turning it on will do |

## What follows

- TalkBack reads each fader as its band name and gain, such as "250 Hz, +3 dB".
- The numbers on a fader get smaller to stay on one line at the largest font.
