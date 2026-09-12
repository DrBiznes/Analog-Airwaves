# Changelog

## 0.1.0

### Added
- **Placeable portable radios.** The portable radio is now a block as well as an item. Placing it
  carries the frequency it was tuned to in hand into the placed radio, and breaking it carries the
  frequency back onto the dropped item.
- Placed radios are stationary receivers: they play their station at their own block position, so
  several radios can cover an area and multiple radios can share one station.
- Shift + right-click a placed radio to toggle its power. Powered and unpowered radios have
  distinct textures, and the power state persists.
- Expanded tooltips. Radios and transmitters show their frequency and a short summary, with the
  full control list behind a Shift hint.

### Changed
- The main hand is now authoritative for reception on both client and server, so dual-wielding two
  radios no longer disagrees about which one is playing. The offhand is used only when the main
  hand has no radio.
- Reworked the transmitter textures: the slab is framed on every edge with the radio's own border
  and its top ring is copied from the radio, the aerial is copper and shaped like the lightning rod
  it is crafted from, and a small lamp on each side shows idle, broadcasting or interference.
- New portable radio model and textures, drawn as a miniature of the Analog Audio radio (frame,
  highlight row, speaker grille, dial and lamp), with the block outline matching the model
  exactly.
- All block textures now use Analog Audio's radio palette, including its exact `#2C2C21` border
  colour, so this mod's blocks frame identically to the radio they sit beside.

### Notes
- Placed radios use the station's broadcast volume. Per-radio volume is out of scope for this
  release.
