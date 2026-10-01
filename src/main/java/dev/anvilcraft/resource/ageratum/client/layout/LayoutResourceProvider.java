package dev.anvilcraft.resource.ageratum.client.layout;

import net.minecraft.resources.Identifier;
import java.util.Map;

/** Optional resource declarations on component factories. Existing lambda factories remain compatible. */
public interface LayoutResourceProvider {
    default Map<Identifier, LayoutTexture> layoutTextures() { return Map.of(); }
    /** Component key -> semantic color name -> default ARGB value. */
    default Map<Identifier, Map<String, Integer>> layoutColors() { return Map.of(); }
}
