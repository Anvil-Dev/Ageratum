package dev.anvilcraft.resource.ageratum.init;

import dev.anvilcraft.resource.ageratum.Ageratum;
import dev.anvilcraft.resource.ageratum.item.component.Doc;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import java.util.function.Supplier;

public final class AgeratumDataComponents {
    private static final DeferredRegister<DataComponentType<?>> COMPONENTS =
        DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, Ageratum.MOD_ID);

    public static final Supplier<DataComponentType<Doc>> DOC = COMPONENTS.register("doc",
        () -> DataComponentType.<Doc>builder().persistent(Doc.CODEC).networkSynchronized(Doc.STREAM_CODEC).build());

    private AgeratumDataComponents() {}

    public static void register(IEventBus bus) {
        COMPONENTS.register(bus);
    }
}
