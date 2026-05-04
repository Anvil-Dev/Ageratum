package dev.anvilcraft.resource.ageratum.mixin.accessor;

import net.minecraft.client.ClientClockManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ClientClockManager.ClockInstance.class)
public interface ClientClockManager$ClockInstanceAccessor {
    @Accessor("totalTicks")
    void totalTicks(long totalTicks);
}
