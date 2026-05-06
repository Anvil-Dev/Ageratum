package dev.anvilcraft.resource.ageratum.data;

import dev.anvilcraft.lib.v2.config.ConfigData;
import dev.anvilcraft.resource.ageratum.Ageratum;
import dev.anvilcraft.resource.ageratum.client.AgeratumClientConfig;
import dev.anvilcraft.resource.ageratum.client.constants.AgeratumConstants;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;

public class AgeratumLanguageProvider extends LanguageProvider {
    public AgeratumLanguageProvider(PackOutput output) {
        super(output, Ageratum.MOD_ID, AgeratumConstants.I18n.DEFAULT_LANGUAGE_CODE);
    }

    @Override
    protected void addTranslations() {
        ConfigData.readConfigClass(this, AgeratumClientConfig.class);
        this.add("commands.ageratum.preview.disable", "Preview is not enabled");
        this.add("system.ageratum.share.tip", "Player %s has shared a guide with you:");
        this.add("system.ageratum.share.button", "[CLICK TO OPEN]");
        this.add("tooltip.ageratum.bind_item_hold", "Hold %s to get more info");
        this.add("key.ageratum.more_info", "Get More Info");
        this.add("key.ageratum.structure_projection.layer_up", "Increase Projection Layers");
        this.add("key.ageratum.structure_projection.layer_down", "Decrease Projection Layers");
        this.add("key.ageratum.structure_projection.remove", "Remove Structure Projection");
        this.add("key.categories.ageratum", "Ageratum");
        this.add("itemGroup.ageratum.default", "Ageratum");
        this.add("item.ageratum.guidebook", "Ageratum Guidebook");
    }
}
