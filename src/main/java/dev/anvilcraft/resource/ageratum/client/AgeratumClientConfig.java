package dev.anvilcraft.resource.ageratum.client;


import dev.anvilcraft.lib.v2.config.BoundedDiscrete;
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

    @Comment("Whether to enable preview mode (enabling it will allow real-time previews of changes to documents in the specified path)")
    public boolean enablePreview = false;

    @Comment("Preview mode path")
    public String previewPath = "ageratum_preview";

    @Comment("Share your guide only with player from the same team")
    public boolean shareGuideOnlyInTeam = false;

    @Comment("Hold duration (milliseconds) for W-key item binding navigation, default 750ms")
    @BoundedDiscrete(min = 100, max = 5000)
    public int itemBindingHoldDurationMs = 750;

    @Comment("The scaling ratio of the interface")
    @BoundedDiscrete(min = 1, max = 4)
    public int scale = 2;
}
