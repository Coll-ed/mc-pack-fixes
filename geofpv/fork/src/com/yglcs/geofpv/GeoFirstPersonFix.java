package com.yglcs.geofpv;

import com.mojang.logging.LogUtils;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.slf4j.Logger;

/**
 * 让 GeckoLib 的 3D 盔甲在第一人称整体不渲染（头部 / 胸甲 / 护腿 / 靴子四个部位都覆盖）。
 *
 * <p>为什么一个挂点就够：原版 {@code HumanoidArmorLayer} 是<b>逐部位</b>调用
 * {@code model.renderToBuffer(...)}，所以只要在那个方法入口按“第一人称 + 本地玩家”取消，
 * 四个部位自然会一起被拦下，不需要按 EquipmentSlot 分别处理。</p>
 */
@Mod(value = GeoFirstPersonFix.MOD_ID, dist = Dist.CLIENT)
public final class GeoFirstPersonFix {

    public static final String MOD_ID = "geofpv";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final ModConfigSpec SPEC;
    private static final ModConfigSpec.BooleanValue ENABLED;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        ENABLED = builder
                .comment("第一人称下不渲染 GeckoLib 3D 盔甲（头盔/胸甲/护腿/靴子全部生效）")
                .define("skipOwnGeoArmorInFirstPerson", true);
        SPEC = builder.build();
    }

    public GeoFirstPersonFix(ModContainer container, IEventBus modBus) {
        container.registerConfig(ModConfig.Type.CLIENT, SPEC);
        LOGGER.info("[geofpv] 已加载：GeckoLib 3D 盔甲在第一人称不再渲染（四个部位统一处理）");
    }

    public static boolean enabled() {
        return ENABLED.get();
    }
}
