package com.yglcs.witherblastfix;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.slf4j.Logger;

/**
 * 只做一件事：让 Wither Reincarnated 的齐射炮弹不破坏方块。
 */
@Mod(WitherBlastFix.MOD_ID)
public final class WitherBlastFix {

    public static final String MOD_ID = "witherblastfix";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final ModConfigSpec SPEC;
    private static final ModConfigSpec.BooleanValue ENABLED;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        ENABLED = builder
                .comment("总开关：关闭后齐射炮弹恢复原版的方块破坏行为")
                .define("enabled", true);
        SPEC = builder.build();
    }

    public WitherBlastFix(ModContainer container, IEventBus modBus) {
        container.registerConfig(ModConfig.Type.COMMON, SPEC);
        LOGGER.info("[witherblastfix] 已加载：齐射凋零头不破坏方块（其余行为不变）");
    }

    public static boolean enabled() {
        return ENABLED.get();
    }
}
