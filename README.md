# Analog Airwaves

A small NeoForge 1.21.1 radio-station extension for [Analog Audio](https://modrinth.com/mod/analog-audio), adding broadcast transmitters and portable frequency-tuned radios.

Place a transmitter directly on top of an Analog Audio radio, tune it to a frequency, then tune a portable radio to the same frequency to listen — either in your hand or placed down as a stationary receiver.

## Controls

Tuning uses Analog Audio's rotary tuner: **hold** right-click and drag to turn the dial. Releasing the button closes the tuner and saves the frequency.

| Context | Right-click | Shift + right-click |
| --- | --- | --- |
| Portable radio in hand, aimed at air | Open the tuner | — |
| Portable radio aimed at a surface | Place the radio | Open the tuner instead of placing |
| Placed portable radio | Open the tuner | Toggle power on/off |
| Transmitter | Open the tuner | — |

When both hands hold a portable radio, the **main hand** is the one that receives and tunes.

## Behaviour

- **Transmitters** broadcast the radio beneath them across the dimension. They only broadcast while their chunk is loaded, and listeners must be in the same dimension.
- **Portable radios** receive while held, or while placed and powered on. A placed radio plays at its own block position, so several radios can carry the same station around your base.
- Placed radios use the station's own broadcast volume — there are no per-radio volume controls.
- Frequency and power state are preserved when a placed radio is broken and put back down.
- Multiple loaded transmitters on the same frequency cause interference, and receivers on that frequency hear static instead of a station.

Requires [Analog Audio 0.1.5-hotfix.1](https://modrinth.com/mod/analog-audio/version/BshMjyFt). [Source](https://github.com/palmmc/AnalogAudio)

## Development

Textures are generated from a single palette shared with Analog Audio's radio:

```
python tools/generate_textures.py
```
