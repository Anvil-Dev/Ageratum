package dev.anvilcraft.resource.ageratum.data;

import dev.anvilcraft.lib.v2.config.ConfigData;
import dev.anvilcraft.lib.v2.registrum.providers.RegistrumLangProvider;
import dev.anvilcraft.resource.ageratum.client.AgeratumClientConfig;

public class AgeratumLangHandler {
    public static void init(RegistrumLangProvider provider) {
        ConfigData.readConfigClass(provider, AgeratumClientConfig.class);
        provider.add("commands.ageratum.preview.disable", "Preview is not enabled");
        provider.add("system.ageratum.share.tip", "Player %s has shared a guide with you:");
        provider.add("system.ageratum.share.button", "[CLICK TO OPEN]");
        provider.add("tooltip.ageratum.bind_item_hold", "Hold %s to get more info");
        provider.add("key.ageratum.more_info", "Get More Info");
        provider.add("key.ageratum.structure_projection.layer_up", "Increase Projection Layers");
        provider.add("key.ageratum.structure_projection.layer_down", "Decrease Projection Layers");
        provider.add("key.ageratum.structure_projection.remove", "Remove Structure Projection");
        provider.add("key.category.ageratum.key", "Ageratum");
    }
}
