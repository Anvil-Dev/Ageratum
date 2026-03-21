package dev.anvilcraft.resource.ageratum;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

@Mod(Ageratum.MOD_ID)
public class Ageratum {
    public static final String MOD_ID = "ageratum";
    private static final Logger LOGGER = LogUtils.getLogger();

    public Ageratum(IEventBus modEventBus, ModContainer modContainer) {
    }
}
