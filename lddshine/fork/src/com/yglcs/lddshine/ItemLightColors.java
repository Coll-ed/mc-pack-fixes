package com.yglcs.lddshine;

import com.bloom.client.coloredlight.ColoredLightConfig;
import com.bloom.client.coloredlight.ColoredLightConfigManager;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 把「光源手里的东西」翻译成 Shine 的颜色 / 半径 / 强度。
 *
 * <p>优先级：物品是 BlockItem → 直接用 Shine 的 blockLights 表按方块查（用户选定的方案）；
 * 查不到或不是方块物品 → 查本模组的 itemOverrides 覆盖表；都没有则这个光源不参与着色。</p>
 */
public final class ItemLightColors {

    /** 解析结果，通道与 Shine 的 uniform 对齐（0..1 浮点 + 世界半径 + 强度）。 */
    public record Resolved(float r, float g, float b, float radius, float intensity) {
    }

    private ItemLightColors() {
    }

    /** Shine 的彩色光总开关（关掉时桥接也不该偷偷发光）。 */
    public static boolean coloredLightEnabled() {
        ColoredLightConfig config = ColoredLightConfigManager.get();
        return config != null && config.enabled;
    }

    /** 解析结果；无颜色可给时返回 {@code null}（调用方跳过这个光源）。 */
    public static Resolved resolve(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }

        ColoredLightConfig config = ColoredLightConfigManager.get();
        if (config == null) {
            return null;
        }

        if (stack.getItem() instanceof BlockItem blockItem) {
            BlockState state = blockItem.getBlock().defaultBlockState();
            ColoredLightConfig.BlockLight light = config.lightForState(state);
            if (light != null && light.enabled) {
                return new Resolved(
                        channel(light.color, 16),
                        channel(light.color, 8),
                        channel(light.color, 0),
                        (float) light.radius,
                        (float) light.intensity);
            }
        }

        String itemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();

        String hex = BridgeSettings.itemOverrides().get(itemId);
        if (hex != null) {
            int rgb = parseHex(hex);
            if (rgb < 0) {
                return null;
            }
            return new Resolved(
                    channel(rgb, 16),
                    channel(rgb, 8),
                    channel(rgb, 0),
                    (float) BridgeSettings.fallbackRadius(),
                    (float) BridgeSettings.fallbackIntensity());
        }

        DefaultItemColors.Entry fallback = DefaultItemColors.get(itemId);
        if (fallback != null) {
            return new Resolved(fallback.r(), fallback.g(), fallback.b(),
                    fallback.radius(), fallback.intensity());
        }
        return null;
    }

    /** 取 24 位颜色的某个 8 bit 通道并归一化到 0..1。 */
    private static float channel(int rgb, int shift) {
        return ((rgb >> shift) & BridgeConstants.CHANNEL_MAX) / (float) BridgeConstants.CHANNEL_MAX;
    }

    /** 解析 "#RRGGBB" / "RRGGBB"；非法返回 -1 并记日志（不静默吞）。 */
    private static int parseHex(String hex) {
        String normalized = hex.startsWith("#") ? hex.substring(1) : hex;
        if (normalized.length() != 6) {
            LddShine.LOGGER.warn("[lddshine] itemOverrides 颜色应为 6 位十六进制，实得：{}", hex);
            return -1;
        }
        try {
            return Integer.parseInt(normalized, 16) & BridgeConstants.RGB_MASK;
        } catch (NumberFormatException e) {
            LddShine.LOGGER.warn("[lddshine] itemOverrides 颜色解析失败：{}", hex, e);
            return -1;
        }
    }
}
