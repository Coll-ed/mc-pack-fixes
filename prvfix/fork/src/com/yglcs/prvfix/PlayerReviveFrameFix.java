package com.yglcs.prvfix;

import com.mojang.logging.LogUtils;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

/**
 * PlayerRevive 倒地闪烁修复。
 *
 * <p>PlayerRevive 的 {@code ReviveEventClient#tick} 订阅的是 {@code RenderGuiLayerEvent.Post}，
 * 这个事件在每帧会按 GUI 图层触发多次（原版 + 各模组图层）。它每次都会重设倒地状态并切换后处理链，
 * 于是同一帧内反复 loadEffect / checkEntityPostEffect，表现为世界与 UI 剧烈闪烁。</p>
 *
 * <p>这里用「每帧只放行第一次调用」把它降回每帧一次，并让它在绘制后恢复 RenderSystem 状态。</p>
 */
@Mod(value = PlayerReviveFrameFix.MOD_ID, dist = Dist.CLIENT)
public final class PlayerReviveFrameFix {

    public static final String MOD_ID = "prvfix";
    public static final Logger LOGGER = LogUtils.getLogger();

    /** 当前渲染帧是否已经执行过 PlayerRevive 的 tick 逻辑。 */
    private static boolean handledThisFrame;

    public PlayerReviveFrameFix(ModContainer container, IEventBus modBus) {
        NeoForge.EVENT_BUS.addListener(PlayerReviveFrameFix::onRenderFramePre);
        LOGGER.info("[prvfix] 已加载：PlayerRevive 倒地逻辑改为每帧只执行一次");
    }

    private static void onRenderFramePre(RenderFrameEvent.Pre event) {
        handledThisFrame = false;
    }

    /** 供 mixin 调用：返回 true 表示本帧已处理过，调用方应取消。 */
    public static boolean beginFrame() {
        if (handledThisFrame) {
            return false;
        }
        handledThisFrame = true;
        return true;
    }
}
