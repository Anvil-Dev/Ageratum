package dev.anvilcraft.resource.ageratum.data;

import dev.anvilcraft.lib.v2.registrum.providers.ProviderType;

import static dev.anvilcraft.resource.ageratum.Ageratum.REGISTRUM;

public class AgeratumDatagen {
    public static void init() {
        REGISTRUM.getDataGenInitializer();
        REGISTRUM.addDataGenerator(ProviderType.LANG, AgeratumLangHandler::init);
    }
}
