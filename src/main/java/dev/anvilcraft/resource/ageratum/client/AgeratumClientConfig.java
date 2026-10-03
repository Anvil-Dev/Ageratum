package dev.anvilcraft.resource.ageratum.client;


import dev.anvilcraft.lib.v2.config.BoundedDiscrete;
import dev.anvilcraft.lib.v2.config.Comment;
import dev.anvilcraft.lib.v2.config.Config;
import dev.anvilcraft.resource.ageratum.Ageratum;
import dev.anvilcraft.resource.ageratum.client.gui.GuideScale;
import dev.anvilcraft.resource.ageratum.client.layout.DirectorySide;
import dev.anvilcraft.lib.v2.config.ConfigManager;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.LinkedHashMap;
import java.util.Map;

@Config(
    name = Ageratum.MOD_ID,
    type = ModConfig.Type.CLIENT
)
public class AgeratumClientConfig {
    private static ModConfigSpec configSpec;
    private static ModConfigSpec.DoubleValue scaleValue;
    private static ModConfigSpec.BooleanValue darkModeValue;

    @Comment("Directory panel position: LEFT puts navigation left and content right; RIGHT reverses them. Applies to open guides")
    public DirectorySide directorySide = DirectorySide.LEFT;

    @Comment("Use dark guide layouts; applies to open guides and persists across pages")
    public boolean darkMode = false;

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

    @Comment("Continuous guide zoom from 0.5 to 4.0 (for example 0.75 or 1.25); applies to open guides")
    @BoundedDiscrete(min = 0.5, max = 4.0)
    public double scale = 1.0;

    /**
     * Retain AnvilLib's config names, comments, ranges and the standard config screen. Its pinned
     * BoundedDiscrete.minDouble floors 0.5 to 0, so define this one fractional range explicitly.
     */
    public void register(ModContainer container, IEventBus bus) {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        Map<Field, ModConfigSpec.ConfigValue<?>> values = new LinkedHashMap<>();
        for (Field field : AgeratumClientConfig.class.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers())) continue;
            String key = field.getName().replaceAll("([a-z0-9])([A-Z])", "$1_$2").toLowerCase(java.util.Locale.ROOT);
            builder.translation("ageratum.configuration." + key);
            Comment comment = field.getAnnotation(Comment.class);
            if (comment != null) builder.comment(comment.value());
            try {
                if (field.getName().equals("scale")) {
                    scaleValue = builder.defineInRange(key, this.scale, GuideScale.MIN, GuideScale.MAX);
                    values.put(field, scaleValue);
                } else if (field.getName().equals("darkMode")) {
                    darkModeValue = builder.define(key, this.darkMode);
                    values.put(field, darkModeValue);
                } else values.put(field, ConfigManager.define(builder, key, field, field.get(this)));
            } catch (IllegalAccessException exception) { throw new IllegalStateException(exception); }
        }
        ModConfigSpec spec = builder.build();
        configSpec = spec;
        java.util.function.Consumer<ModConfigEvent> load = event -> {
            if (event.getConfig().getSpec() != spec || !spec.isLoaded()) return;
            values.forEach((field, value) -> {
                try {
                    if (field.getType() == double.class) field.setDouble(this, GuideScale.clamp(((Number) value.get()).doubleValue()));
                    else field.set(this, value.get());
                } catch (IllegalAccessException exception) { throw new IllegalStateException(exception); }
            });
        };
        bus.addListener((ModConfigEvent.Loading event) -> load.accept(event));
        bus.addListener((ModConfigEvent.Reloading event) -> load.accept(event));
        container.registerConfig(ModConfig.Type.CLIENT, spec, "ageratum-client.toml");
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }

    /** Persist the shared theme preference without changing unrelated config values. */
    public void setDarkMode(boolean darkMode) {
        this.darkMode = darkMode;
        if (configSpec != null && configSpec.isLoaded() && darkModeValue != null) {
            darkModeValue.set(darkMode);
            darkModeValue.save();
        }
    }

    /** Persist once at the end of an interactive zoom gesture, not on every pointer move. */
    public void saveScale() {
        this.scale = GuideScale.clamp(this.scale);
        if (configSpec != null && configSpec.isLoaded() && scaleValue != null) {
            scaleValue.set(this.scale);
            scaleValue.save();
        }
    }
}
