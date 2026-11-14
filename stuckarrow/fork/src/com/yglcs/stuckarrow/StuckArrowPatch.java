package com.yglcs.stuckarrow;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * 客户端小补丁：让 AITK 的假箭按「真实命中点 + 弹道朝向」绘制。
 *
 * <p>只做两件事：
 * <ol>
 *   <li>{@code AbstractArrowHitMixin} 在命中瞬间采集偏移与朝向（服务端线程，单人有效）；</li>
 *   <li>{@code AitkLayerOverrideMixin} 在渲染 AITK 箭头层时优先用这份数据，没有则回退 AITK 原逻辑。</li>
 * </ol>
 */
@Mod(value = StuckArrowPatch.MOD_ID, dist = Dist.CLIENT)
public final class StuckArrowPatch {

   public static final String MOD_ID = "stuckarrowpatch";

   public StuckArrowPatch(IEventBus modBus) {
      // 换世界/重连时清空记录：实体 id 会复用，旧数据不能跨世界存活
      NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut event) -> StuckArrows.clear());
      NeoForge.EVENT_BUS.register(StuckArrowRenderHandler.class);
   }
}
