package com.yglcs.wakesfix;

import com.mojang.logging.LogUtils;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

/**
 * Wakes 联动修正：船只从高处水面落入低处水面时，尾迹波浪不应跟着船下沉。
 *
 * <p>不修改 Wakes 的 jar，也不写数据包：只作为独立的客户端联动模组，
 * 在客户端 tick 里校正 {@code ProducesWake.producingWaterLevel}。</p>
 */
@Mod(value = WakesFix.MOD_ID, dist = Dist.CLIENT)
public final class WakesFix {

    public static final String MOD_ID = "wakesfix";
    static final Logger LOGGER = LogUtils.getLogger();

    public WakesFix(ModContainer container, IEventBus modBus) {
        NeoForge.EVENT_BUS.addListener(WakesFix::onClientTick);
        LOGGER.info("[wakesfix] 已加载：船只落差时把尾迹水位钉在水面上");
    }

    private static void onClientTick(ClientTickEvent.Post event) {
        WakeHeightCorrector.tick();
    }
}
