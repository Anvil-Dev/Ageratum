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

    @Comment("Show line numbers in code blocks")
    public boolean showCodeBlockLineNumbers = true;

    @Comment("Allow line breaks in code block content")
    public boolean allowCodeBlockLineContentLineBreaks = true;
}
