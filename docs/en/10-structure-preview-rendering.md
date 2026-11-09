# Structure Preview Rendering

This document explains how `MDNBTStructureComponent` renders `.nbt` / `.snbt` (SNBT) structures inside the guide UI, including the core pipeline and class responsibilities.


## Block information and mouse controls

- Left-click a block to inspect its name, registry ID, structure position, state and saved block entity data. Pulse generators show trigger mode, delay, pulse duration and output inversion. Comparator, detector and collector settings are translated; inventories, filters, marked items, fluids, sign text and other nested data are expanded.
- Hold Shift and scroll to page through long details. Text wraps to the window width. Selecting another block resets the page; clicking empty space clears the selection. Details use the original structure snapshot, before preview loading callbacks can change it.
- Drag with the left button to pan, drag with the right button to rotate, and scroll to zoom. Dragging does not select a block.
- Hide upper layers with the layer buttons or PageUp / PageDown to select exposed blocks below. Hiding the selected block's layer clears the selection.
- Picking shares the rendering camera transform and uses block outlines, including under guide scaling, layouts, and row components.


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


## Block entity field translations

All mods' saved fields are supported without depending on their Java classes. Resource packs can translate field names with `tooltip.ageratum.structure_preview.nbt.<namespace>.<block_path>.<NBT_path>`, enum values with the `.value.<stored_value>` suffix, and units with a `.format` translation containing `%s`. For example, `tooltip.ageratum.structure_preview.nbt.anvilcraft.pulse_generator.ExtraData.WaitingTime.format` is `%s ticks`. Common field names use `tooltip.ageratum.structure_preview.nbt.field.<field_name>`.

Unknown fields retain their paths and values. Only root type/position metadata and `keepPacked` are hidden; nested item IDs and coordinates remain visible. Details reflect saved data and do not infer missing defaults. Expansion is limited to 4096 entries and 32 nested levels per block, with an explicit notice when exceeded.
