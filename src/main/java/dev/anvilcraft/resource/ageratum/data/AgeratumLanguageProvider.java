package dev.anvilcraft.resource.ageratum.data;

import dev.anvilcraft.lib.v2.config.ConfigData;
import dev.anvilcraft.resource.ageratum.Ageratum;
import dev.anvilcraft.resource.ageratum.client.AgeratumClientConfig;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;

public class AgeratumLanguageProvider extends LanguageProvider {
    public AgeratumLanguageProvider(PackOutput output) {
        super(output, Ageratum.MOD_ID, "en_us");
    }

    @Override
    protected void addTranslations() {
        ConfigData.readConfigClass(this, AgeratumClientConfig.class);
        this.add("commands.ageratum.preview.disable", "Preview is not enabled");
        this.add("system.ageratum.share.tip", "Player %s has shared a guide with you:");
        this.add("system.ageratum.share.button", "[CLICK TO OPEN]");
    }
}
