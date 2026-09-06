# Brand assets

## App launcher icon

The icon is a **vector adaptive icon** (min SDK 26, so no legacy raster mipmaps).

| Piece | File |
|---|---|
| Master vector | `brand/ic_launcher.svg` |
| Foreground (colour) | `app/src/main/res/drawable/ic_launcher_foreground.xml` |
| Foreground (themed / notification) | `app/src/main/res/drawable/ic_launcher_monochrome.xml` |
| Background | `app/src/main/res/drawable/ic_launcher_background.xml` → `@color/ic_launcher_background` (`#F5F7F7`) |
| Adaptive icon | `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml` and `ic_launcher_round.xml` |

**Concept** — three fanned tender sheets behind a teal magnifying glass whose
lens holds a checkmark, plus a small AI spark. Palette: teal `#0E9AAB`
accent, slate `#10191A` line, off-white `#F5F7F7` ground.

**Play Store listing icon (512×512)** — export from `ic_launcher.svg`
(e.g. `rsvg-convert -w 512 -h 512 brand/ic_launcher.svg > play_store_512.png`
or Android Studio → *Image Asset*). Not committed because it is a store
upload, not an app resource.

Keep the three drawables and the SVG in sync when editing.
