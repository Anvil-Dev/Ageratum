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
        this.add("ageratum.guide.zoom.default", "Default");
        this.add("commands.ageratum.preview.disable", "Preview is not enabled");
        this.add("commands.ageratum.item.empty_hand", "No item in main hand");
        this.add("commands.ageratum.item.id_copy_hint", "Click to copy item id");
        this.add("commands.ageratum.item.ref_copy_hint", "Click to copy ref tag");
        this.add("system.ageratum.share.tip", "Player %s has shared a guide with you:");
        this.add("system.ageratum.share.button", "[CLICK TO OPEN]");
        this.add("tooltip.ageratum.bind_item_hold", "Hold %s to get more info");
        this.add("tooltip.ageratum.structure_projection.layer_shortcut", "Press %s / %s to adjust projection layers");
        this.add("tooltip.ageratum.structure_projection.remove_shortcut", "Press %s to close projection");
        this.add("tooltip.ageratum.dark_mode", "Switch to dark mode");
        this.add("tooltip.ageratum.light_mode", "Switch to light mode");
        this.add("tooltip.anvillib_rendering.structure_preview.nbt.anvilcraft.pulse_generator.ExtraData.StartMode", "Trigger mode");
        this.add("tooltip.anvillib_rendering.structure_preview.nbt.anvilcraft.pulse_generator.ExtraData.StartMode.value.0", "Rising edge");
        this.add("tooltip.anvillib_rendering.structure_preview.nbt.anvilcraft.pulse_generator.ExtraData.StartMode.value.1", "Falling edge");
        this.add("tooltip.anvillib_rendering.structure_preview.nbt.anvilcraft.pulse_generator.ExtraData.StartMode.value.2", "Loop");
        this.add("tooltip.anvillib_rendering.structure_preview.nbt.anvilcraft.pulse_generator.ExtraData.WaitingTime", "Delay");
        this.add("tooltip.anvillib_rendering.structure_preview.nbt.anvilcraft.pulse_generator.ExtraData.WaitingTime.format", "%s ticks");
        this.add("tooltip.anvillib_rendering.structure_preview.nbt.anvilcraft.pulse_generator.ExtraData.SignalDuration", "Pulse duration");
        this.add("tooltip.anvillib_rendering.structure_preview.nbt.anvilcraft.pulse_generator.ExtraData.SignalDuration.format", "%s ticks");
        this.add("tooltip.anvillib_rendering.structure_preview.nbt.anvilcraft.pulse_generator.ExtraData.OutputMode", "Invert output");
        this.add("tooltip.anvillib_rendering.structure_preview.nbt.anvilcraft.pulse_generator.ExtraData.OutputMode.value.0", "Off");
        this.add("tooltip.anvillib_rendering.structure_preview.nbt.anvilcraft.pulse_generator.ExtraData.OutputMode.value.1", "On");
        this.add("tooltip.anvillib_rendering.structure_preview.nbt.anvilcraft.pulse_generator.ExtraData.Inputting", "Input signal");
        this.add("tooltip.anvillib_rendering.structure_preview.nbt.anvilcraft.pulse_generator.ExtraData.Inputting.value.0", "Off");
        this.add("tooltip.anvillib_rendering.structure_preview.nbt.anvilcraft.pulse_generator.ExtraData.Inputting.value.1", "On");
        this.add("tooltip.anvillib_rendering.structure_preview.nbt.anvilcraft.pulse_generator.ExtraData.State", "Saved phase");
        this.add("tooltip.anvillib_rendering.structure_preview.nbt.anvilcraft.pulse_generator.ExtraData.State.value.0", "Idle");
        this.add("tooltip.anvillib_rendering.structure_preview.nbt.anvilcraft.pulse_generator.ExtraData.State.value.1", "Waiting");
        this.add("tooltip.anvillib_rendering.structure_preview.nbt.anvilcraft.pulse_generator.ExtraData.State.value.2", "Outputting");
        this.add("tooltip.anvillib_rendering.structure_preview.nbt.anvilcraft.pulse_generator.ExtraData.PhaseDuration", "Phase duration");
        this.add("tooltip.anvillib_rendering.structure_preview.nbt.anvilcraft.pulse_generator.ExtraData.PhaseDuration.format", "%s ticks");
        this.add("tooltip.anvillib_rendering.structure_preview.nbt.anvilcraft.pulse_generator.ExtraData.PhaseStartGameTime", "Phase start time");
        this.add("tooltip.anvillib_rendering.structure_preview.nbt.anvilcraft.advanced_comparator.ExtraData.CompareMode", "Compare mode");
        this.add("tooltip.anvillib_rendering.structure_preview.nbt.anvilcraft.advanced_comparator.ExtraData.CompareMode.value.0", "Hysteresis");
        this.add("tooltip.anvillib_rendering.structure_preview.nbt.anvilcraft.advanced_comparator.ExtraData.CompareMode.value.1", "Window");
        this.add("tooltip.anvillib_rendering.structure_preview.nbt.anvilcraft.advanced_comparator.ExtraData.HighLimit", "Upper limit");
        this.add("tooltip.anvillib_rendering.structure_preview.nbt.anvilcraft.advanced_comparator.ExtraData.LowLimit", "Lower limit");
        this.add("tooltip.anvillib_rendering.structure_preview.nbt.anvilcraft.advanced_comparator.ExtraData.InputSignal", "Input signal");
        this.add("tooltip.anvillib_rendering.structure_preview.nbt.anvilcraft.advanced_comparator.ExtraData.OutputMode", "Invert output");
        this.add("tooltip.anvillib_rendering.structure_preview.nbt.anvilcraft.advanced_comparator.ExtraData.OutputMode.value.0", "Off");
        this.add("tooltip.anvillib_rendering.structure_preview.nbt.anvilcraft.advanced_comparator.ExtraData.OutputMode.value.1", "On");
        this.add("tooltip.anvillib_rendering.structure_preview.nbt.anvilcraft.advanced_comparator.ExtraData.RedstoneControl", "Redstone control");
        this.add("tooltip.anvillib_rendering.structure_preview.nbt.anvilcraft.advanced_comparator.ExtraData.RedstoneControl.value.0", "Off");
        this.add("tooltip.anvillib_rendering.structure_preview.nbt.anvilcraft.advanced_comparator.ExtraData.RedstoneControl.value.1", "On");
        this.add("tooltip.anvillib_rendering.structure_preview.nbt.anvilcraft.item_detector.FilterMode", "Match mode");
        this.add("tooltip.anvillib_rendering.structure_preview.nbt.anvilcraft.item_detector.FilterMode.value.ANY", "Any match");
        this.add("tooltip.anvillib_rendering.structure_preview.nbt.anvilcraft.item_detector.FilterMode.value.ALL", "All match");
        this.add("tooltip.anvillib_rendering.structure_preview.nbt.anvilcraft.item_detector.OutputMode", "Output mode");
        this.add("tooltip.anvillib_rendering.structure_preview.nbt.anvilcraft.item_detector.OutputMode.value.0", "Normal");
        this.add("tooltip.anvillib_rendering.structure_preview.nbt.anvilcraft.item_detector.OutputMode.value.1", "15 when input is zero");
        this.add("tooltip.anvillib_rendering.structure_preview.nbt.anvilcraft.item_detector.OutputMode.value.2", "15 minus signal");
        this.add("tooltip.anvillib_rendering.structure_preview.nbt.anvilcraft.item_collector.RangeRadius", "Range radius");
        this.add("tooltip.anvillib_rendering.structure_preview.nbt.anvilcraft.item_collector.RangeRadius.value.0", "1 blocks");
        this.add("tooltip.anvillib_rendering.structure_preview.nbt.anvilcraft.item_collector.RangeRadius.value.1", "2 blocks");
        this.add("tooltip.anvillib_rendering.structure_preview.nbt.anvilcraft.item_collector.RangeRadius.value.2", "4 blocks");
        this.add("tooltip.anvillib_rendering.structure_preview.nbt.anvilcraft.item_collector.RangeRadius.value.3", "8 blocks");
        this.add("tooltip.anvillib_rendering.structure_preview.nbt.anvilcraft.item_collector.Cooldown", "Collection interval");
        this.add("tooltip.anvillib_rendering.structure_preview.nbt.anvilcraft.item_collector.Cooldown.value.0", "Intercept mode");
        this.add("tooltip.anvillib_rendering.structure_preview.nbt.anvilcraft.item_collector.Cooldown.value.1", "2 ticks");
        this.add("tooltip.anvillib_rendering.structure_preview.nbt.anvilcraft.item_collector.Cooldown.value.2", "10 ticks");
        this.add("tooltip.anvillib_rendering.structure_preview.nbt.anvilcraft.item_collector.Cooldown.value.3", "60 ticks");
        this.add("tooltip.ageratum.structure_export", "Export the full structure to the current server world's data/ageratum folder");
        this.add("system.ageratum.structure_export.sending", "Sending structure export to the server...");
        this.add("system.ageratum.structure_export.success", "Structure exported to the current world: %s");
        this.add("system.ageratum.structure_export.failed", "Structure export failed. Check the game/server log.");
        this.add("system.ageratum.structure_export.too_large", "Structure export exceeds the 8 MiB compressed size limit.");
        this.add("system.ageratum.structure_export.busy", "Please wait two seconds between structure exports.");
        this.add("key.ageratum.more_info", "Get More Info");
        this.add("key.ageratum.structure_projection.layer_up", "Increase Projection Layers");
        this.add("key.ageratum.structure_projection.layer_down", "Decrease Projection Layers");
        this.add("key.ageratum.structure_projection.remove", "Remove Structure Projection");
        this.add("key.categories.ageratum", "Ageratum");
        this.add("itemGroup.ageratum.default", "Ageratum");
        this.add("item.ageratum.guidebook", "Ageratum Guidebook");
    }
}
