# Changelog

## 0.1.1

### Added
- Transmitters now render broadcast waves while they're on air, the same as the portable radio.
- **Create:** the Portable Radio is now made by sequenced assembly instead of a crafting table recipe. Start with a Copper Sheet and run it through:
  1. Deploy a Create Transmitter
  2. Deploy Redstone
  3. Deploy a Copper Sheet
  4. Press
  5. Deploy an Iron Nugget
- New Incomplete Portable Radio item. It only appears partway through the sequenced assembly and shows a progress bar.

- **Create:** new `recipes.portableRadioRecipe` config option picks which Portable Radio recipe is used when Create is installed:
  - `CREATE_SEQUENCED_ASSEMBLY` (default): the sequenced assembly above.
  - `CREATE_CRAFTING`: the crafting table recipe with a Create Transmitter, as in 0.1.0.
  - `VANILLA`: the vanilla crafting table recipe.

  Without Create, the vanilla recipe is always used. Changes take effect on server restart or `/reload`.

### Changed
- **Create:** the Portable Radio now defaults to sequenced assembly instead of the crafting table recipe. Set `portableRadioRecipe` to `CREATE_CRAFTING` to keep the 0.1.0 recipe. The Radio Broadcaster recipe is unchanged.

### Fixed
- The Portable Radio recipe now unlocks in the recipe book as soon as you pick up redstone. Before, the advancement needed redstone *and* the recipe already unlocked, so it never fired.

## 0.1.0
- Initial release.
