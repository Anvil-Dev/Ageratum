package dev.anvilcraft.resource.ageratum.client.util;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.neoforged.fml.ModList;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

/**
 * 对 Sodium 精灵活跃度 API 的可选兼容桥接。
 *
 * <p>通过反射解析目标类，保证在安装或未安装 Sodium 时都能运行。</p>
 */
public class SodiumSpriteBridge {
    private static final Logger LOG = LoggerFactory.getLogger(SodiumSpriteBridge.class);

    @Nullable
    private static final MethodHandle METHOD_HANDLE;

    static {
        MethodHandle handle = null;
        try {
            handle = MethodHandles.lookup().findStatic(
                Class.forName("me.jellysquid.mods.sodium.client.render.texture.SpriteUtil"),
                "markSpriteActive",
                MethodType.methodType(void.class, TextureAtlasSprite.class));
            LOG.info("Loaded Sodium active sprite compat.");
        } catch (NoSuchMethodException | IllegalAccessException | ClassNotFoundException e) {
            if (ModList.get().getModContainerById("sodium").isPresent()) {
                LOG.error("Failed to load Sodium active sprite compat.", e);
            }
        }

        METHOD_HANDLE = handle;
    }

    /**
     * 若兼容句柄存在，则通知 Sodium 当前帧使用了该精灵。
     */
    public static void pingSpriteUsage(@Nullable TextureAtlasSprite sprite) {
        if (sprite != null && METHOD_HANDLE != null) {
            try {
                METHOD_HANDLE.invokeExact(sprite);
            } catch (Throwable e) {
                throw new RuntimeException("Failed to invoke SpriteUtil#markSpriteActive", e);
            }
        }
    }
}

