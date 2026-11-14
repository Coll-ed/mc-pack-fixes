package com.yglcs.fpafix;

import com.mojang.logging.LogUtils;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

/** 第一人称下取消本地玩家自己的盔甲层渲染。 */
@Mod(value = FirstPersonArmorFix.MOD_ID, dist = Dist.CLIENT)
public final class FirstPersonArmorFix {

    public static final String MOD_ID = "fpad";
    public static final Logger LOGGER = LogUtils.getLogger();

    public FirstPersonArmorFix(ModContainer container, IEventBus modBus) {
        LOGGER.info("[fpad] 诊断探针已加载：第一人称下若调用盔甲层，会打印完整调用栈（不做任何干预）");
    }
}
