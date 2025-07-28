# Tile Molester

Tile Molester is a multi-format, user-extensible graphics data editor that lets you create, view and edit graphics in arbitrary binary files, with a particular focus on binaries for game consoles.

It was originally developed SnowBro and later improved by Central MiB and Lab313. Mewster merged those changes and updates into the original source code and added some other improvements as well, which I used as a base to add UI improvements.


## Changelog

### v0.23 (by hansbonini a.k.a Anime_World)
- Add swizzle modes for texture visualizations as tile
- Fix Block Dimensions setting up canvas dimensions, now they work individually.

### v0.22 (by hansbonini a.k.a Anime_World)
- Add support for Mesen and Exodus CRAM Dump as Palette
- Add new codecs for visualization
- Add support for custom tile dimensions
- Add easy block size configuration
- Add statusbar configuration for tile and block dimensions

### v0.21 (by toruzz)
- New themes. Old custom system removed
- Fractional scale support
- MacOS support (Menu Bar moved to the top, Dock Icon native theme, etc.)
- New application icon
- UX improvements
- Dark mode toggle added
- Edit color implemented
- Tons of fixes and changes
- Used Maven for building (PR by devnewton)

### v0.20 (by toruzz)
- Native theme loading in Windows and Linux
- New icon theme
- Modified settings file to allow custom window colors
- Added support for character files, 2bpp raw files and raw palette files
- Splash screen removed

### v0.19 (by Mewster)
- Various undo/redo fixes
- A selection application can be undone and redone
- A paste can be undone and redone
- Fixed the move undo/redo actions
- No more silly “Undo/Redo encoding” messages

### v0.18 (by Mewster)
- Merged Central MiB’s “Tile Molester Alternate 0.15a” and by Lab313’s “Tile Molester Mod 0.17″ versions to the original SnowBro’s “Tile Molester 0.16″
- Compatibility with Nintendo DS *.nds format
- Now you can enter an hexadecimal value in Palette/Import From/This File/Offset field
- Added a palette shifter near the palette viewer panel; you can choose the amount of pixel to shift (decimal values) (only with palettes loaded from the same file)

### v0.17 - v0.17.2 (by Lab313)
- Saves last opened dir (only through File/Open)
- Compatibility with *.smd and *.bin Sega Genesis/Mega Drive/32x formats
- Splash Screen showing time halved
- Max number of tile expansion now is 1024 (was only 32 before)
- Merging Central MiB’s Tile Molester Alternate (0.15a) features:

### v0.15a (by Central MiB)
- Compatibility with *.pat and *.tpl palette files