# Analog Airwaves 0.1.0 Plan

## Release goal

Make Analog Airwaves feel like a cohesive extension of Analog Audio: reliable handheld tuning, a placeable 3D portable radio, clear interaction hints, persistent receiver state, and visuals that match Analog Audio's hardware style.

## Decisions for 0.1.0

- The main-hand portable radio is authoritative when both hands contain radios.
- The offhand radio is used only when the main hand does not contain a portable radio.
- Handheld frequency tuning uses Analog Audio's rotary tuner.
- Placed portable radios are functional receivers, not transmitters.
- Placed portable radios use the station's broadcast volume. They do not have independent per-radio volume controls.
- Shift is reserved for alternate block interaction and expanded tooltip information.
- Per-radio volume is explicitly out of scope for 0.1.0.
- Texture and model revisions are part of the release, not post-release polish.

## Interaction contract

| Context | Right-click | Shift + right-click |
| --- | --- | --- |
| Portable radio in hand, aimed at air | Open frequency tuner | Reserved/no-op for now |
| Portable radio aimed at a valid placement surface | Place the radio block | Preserve normal sneaking placement behavior |
| Placed portable radio, empty hand | Open frequency tuner | Toggle power on/off |
| Transmitter, empty hand | Open frequency tuner | Reserved for future station controls |

The existing Analog Audio tuner remains a hold-and-drag interface. The tooltip must explain that right-click is held while dragging, because releasing the button closes and saves the tuner.

## Workstream 1: interaction and handheld receiver correctness

1. Make main-hand receiver selection authoritative on both server and client.
2. Keep offhand reception available when the main hand is not holding a portable radio.
3. Verify tuning in the main hand, offhand-only, and dual-wield cases.
4. Confirm that the selected hand's frequency is the one displayed, saved, and used for reception.
5. Preserve server-side validation for frequency updates and transmitter tuning.
6. Add tests for main-hand precedence and receiver hand changes.

## Workstream 2: tooltip and control communication

Follow the visible conventions used by Analog Audio and Create:

- Keep the frequency near the top of the tooltip.
- Use the same compact gray/dark-gray information hierarchy.
- Show a short default tooltip with a Shift hint.
- Show expanded control instructions while Shift is held.
- Use translatable keybind components instead of hard-coded key names where possible.
- Keep all tooltip text in the language file.
- Ensure the Shift check is safe for dedicated-server class loading.

The expanded portable-radio tooltip should describe:

- Right-click/hold-and-drag to tune.
- The current frequency.
- Reception while held.
- Right-click placement behavior.
- Placed-radio Shift-right-click power behavior.

The transmitter tooltip should describe its placement requirement, tuning interaction, and its broadcast role.

## Workstream 3: placeable portable radio

1. Add a portable-radio block and block entity.
2. Convert the item to a block item while preserving handheld tuning behavior.
3. Copy the handheld frequency into the block entity when placing the radio.
4. Preserve frequency and enabled state when breaking and picking up the radio.
5. Add persistent enabled/on state to the block entity.
6. Start newly placed radios enabled unless testing shows that an off-by-default behavior is more appropriate.
7. Add clear powered/off visual states.
8. Ensure placement, breaking, chunk unloading, and block removal clean up receiver playback.

## Workstream 4: placed-radio audio behavior

1. Treat a placed portable radio as a receiver at its block position.
2. Send receiver state to nearby players in the same dimension.
3. Use the block position as the stable client playback identity.
4. Reuse the station's cassette, start time, looping state, and broadcast volume.
5. Pass the station's broadcast volume through unchanged; do not apply a receiver-specific volume multiplier.
6. Stop playback when the radio is disabled, broken, unloaded, out of range, or loses its signal.
7. Keep no-signal and interference behavior consistent with handheld radios.
8. Verify multiple placed receivers can listen to the same station without interfering with one another.

## Workstream 5: model and texture redesign

### Style reference

Audit Analog Audio's radios, speakers, cassette decks, and walkie-talkies for:

- Pixel density and texture resolution.
- Palette and material separation.
- Edge highlights and shadow direction.
- Panel, dial, grille, antenna, and indicator treatment.
- Item, ground, hand, and block presentation.

Use Analog Audio as the visual reference without directly copying its textures.

### Transmitter

- Rework the current slab-and-pin design into a more deliberate broadcast device.
- Improve the silhouette and readable hardware details.
- Revise idle, broadcasting, and interference textures/models.
- Add a clear visual signal indicator or status detail.
- Recheck its placement on top of the Analog Audio radio.

### Portable radio

- Create a proper 3D handheld radio model.
- Create intentional first-person, third-person, GUI, ground, and placed-block transforms.
- Add readable controls, antenna, speaker/grille, dial, and status details.
- Provide powered-on and powered-off visual states.
- Make sure the model reads correctly from all sides when placed.

### Art iteration and QA

1. Block out geometry before final texturing.
2. Apply a first texture pass for material separation.
3. Apply a second pass for palette, highlights, shadows, and detail.
4. Add state-specific visual variants.
5. Compare the assets in-game beside Analog Audio blocks.
6. Check scale, lighting, seams, rotations, GUI scale, hand scale, and ground scale.

## Workstream 6: verification

Test the complete release in single-player and multiplayer:

- Main-hand and offhand receiver authority.
- Dual-wield frequency changes.
- Handheld tuning and tooltip instructions.
- Portable-radio placement and pickup persistence.
- Placed-radio tuning and power toggling.
- Station broadcast volume being heard correctly by both receiver types.
- No signal, valid signal, and interference states.
- Multiple receivers and multiple transmitters.
- Chunk unload/reload and dimension changes.
- Breaking, disabling, and removing active radios.
- Model transforms and all visual state variants.

## Release checklist

- Update README controls and behavior documentation.
- Add release notes for 0.1.0.
- Confirm all new language keys have fallback text.
- Confirm no per-radio volume controls or misleading tooltip text remain.
- Run the full test suite and a clean build.
- Perform a final in-game visual review of every new model and texture.
