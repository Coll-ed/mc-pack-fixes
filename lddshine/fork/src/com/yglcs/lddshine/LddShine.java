package com.yglcs.lddshine;

import com.mojang.logging.LogUtils;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import org.slf4j.Logger;

/**
 * LambDynamicLights 动态光 → Shine 彩色光 桥接（纯客户端）。
 */
@Mod(value = LddShine.MOD_ID, dist = Dist.CLIENT)
public final class LddShine {

    public static final String MOD_ID = "lddshine";
    public static final Logger LOGGER = LogUtils.getLogger();

    private static long lastDebugLogMs;
    private static int lastReportedAdded = -1;

    public LddShine(ModContainer container, IEventBus modBus) {
        container.registerConfig(ModConfig.Type.CLIENT, BridgeSettings.SPEC);
        LOGGER.info("[lddshine] 已加载：LambDynamicLights 动态光 → Shine 彩色光桥接");
    }

    /** 调试输出（受配置 debugLog 控制，5 秒最多一条）。 */
    public static void debugBridge(int added, int uploadTotal) {
        if (!BridgeSettings.debugLog() || added == lastReportedAdded) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now - lastDebugLogMs < BridgeConstants.DEBUG_LOG_INTERVAL_MS) {
            return;
        }
        lastDebugLogMs = now;
        lastReportedAdded = added;
        LOGGER.info("[lddshine] 本帧追加动态光 {} 个；Shine 当前上传光源共 {} 个", added, uploadTotal);
    }
}
