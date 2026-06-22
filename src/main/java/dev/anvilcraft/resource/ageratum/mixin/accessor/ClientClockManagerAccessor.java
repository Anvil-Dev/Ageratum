package dev.anvilcraft.resource.ageratum.mixin.accessor;

import net.minecraft.client.ClientClockManager;
import net.minecraft.core.Holder;
import net.minecraft.world.clock.WorldClock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ClientClockManager.class)
public interface ClientClockManagerAccessor {
    @Invoker
    ClientClockManager.ClockInstance invokeGetInstance(Holder<WorldClock> definition);
}
