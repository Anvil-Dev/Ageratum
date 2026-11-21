package dev.anvilcraft.resource.ageratum.client.layout;

import dev.anvilcraft.lib.v2.config.util.TranslatableEnum;

/** Player preference for the panel containing document navigation. */
public enum DirectorySide implements TranslatableEnum {
    LEFT,
    RIGHT;

    public String edge() { return this.name().toLowerCase(java.util.Locale.ROOT); }

    @Override
    public String getTranslationKey() { return "ageratum.configuration.directory_side." + this.edge(); }
}
