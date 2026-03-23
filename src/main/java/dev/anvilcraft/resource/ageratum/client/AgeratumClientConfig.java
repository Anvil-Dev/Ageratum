package dev.anvilcraft.resource.ageratum.client;


import dev.anvilcraft.lib.v2.config.Comment;
import dev.anvilcraft.lib.v2.config.Config;
import dev.anvilcraft.resource.ageratum.Ageratum;
import net.neoforged.fml.config.ModConfig;

@Config(
    name = Ageratum.MOD_ID,
    type = ModConfig.Type.CLIENT
)
public class AgeratumClientConfig {
    @Comment("Do breadcrumbs record jumps from sidebar tabs")
    public boolean breadCrumbsHasLabel = false;
}
