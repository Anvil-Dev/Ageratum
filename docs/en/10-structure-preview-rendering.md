# Structure Preview Rendering

This document explains how `MDNBTStructureComponent` renders `.nbt` / `.snbt` (SNBT) structures inside the guide UI, including the core pipeline and class responsibilities.

---

## Overview

The structure preview pipeline is:

1. Resolve the structure target path (including relative paths)
2. Open NBT/SNBT data from resource packs or preview files
3. Place the template into `SandboxRenderLevel`
4. Render it through `StructurePreviewRenderer` into the markdown UI

---

## Key Classes

### `MDNBTStructureComponent`

- Parses `<structure id="..."/>` extension parameters
- Opens structure streams and loads `StructureTemplate`
- Places templates into the preview sandbox level
- Invokes preview rendering during markdown component render

### `SandboxRenderLevel`

- Lightweight preview-only `Level` implementation
- Tracks non-air block positions and initialized lighting sections
- Provides render-time entity iteration and frame clock state

### `StructurePreviewRenderer`

- Builds camera projection/view matrices
- Renders blocks, block entities, and entities in a vanilla-like order
- Splits opaque and translucent passes to keep blending stable

### `ViewportCameraRig`

- Stores preview camera settings
- Provides default isometric presets
- Produces `view` and `projection` matrices

### `RelativePathResolver`

- Resolves relative paths from guide documents
- Supports `.` and `..` segments
- Clamps attempts to escape above logical root

---

## Render Order (Simplified)

1. Update lightmap and drain pending light engine work
2. Apply preview matrices and fog parameters
3. Render opaque blocks
4. Render block entities
5. Render entities
6. Flush render buffers
7. Render translucent blocks

This order follows vanilla `LevelRenderer` behavior closely to reduce visual artifacts.

---

## Notes

- The preview level is not a full gameplay world and should not host game logic ticks.
- Lighting priming depends on `refreshLightingAround`; keep calling it when adding new placement paths.
- If new render layers/post effects are introduced, re-check buffer flush ordering.

