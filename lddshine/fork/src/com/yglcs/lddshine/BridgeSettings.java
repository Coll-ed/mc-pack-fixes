package com.yglcs.lddshine;

import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 客户端配置。所有可调项都在这里，代码里只读符号。
 */
public final class BridgeSettings {

    public static final ModConfigSpec SPEC;

    private static final ModConfigSpec.BooleanValue ENABLED;
    private static final ModConfigSpec.IntValue MAX_EXTRA_LIGHTS;
    private static final ModConfigSpec.BooleanValue DEBUG_LOG;
    private static final ModConfigSpec.DoubleValue FALLBACK_RADIUS;
    private static final ModConfigSpec.DoubleValue FALLBACK_INTENSITY;
    private static final ModConfigSpec.ConfigValue<List<? extends String>> ITEM_OVERRIDES;

    /** 覆盖表解析缓存（配置项内容不变就不重复解析）。 */
    private static List<? extends String> cachedRawOverrides;
    private static Map<String, String> cachedOverrides = Map.of();

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.comment("LambDynamicLights 动态光 → Shine 彩色光 桥接").push("bridge");
        ENABLED = builder.comment("总开关")
                .define("enabled", true);
        MAX_EXTRA_LIGHTS = builder.comment("每帧最多追加多少个动态光（Shine 单次上传上限 64，且与它自己的方块光共享名额）")
                .defineInRange("maxExtraLights", 16, 0, 32);
        DEBUG_LOG = builder.comment("每 5 秒在日志里打印一次桥接统计")
                .define("debugLog", false);
        builder.pop();

        builder.comment("颜色来源：优先复用 Shine 的 blockLights（手持物品是方块物品时按方块查）").push("colors");
        FALLBACK_RADIUS = builder.comment("走 itemOverrides 的非方块物品使用的半径（方块物品一律用 Shine 表里的值）")
                .defineInRange("fallbackRadius", 12.0D, 1.0D, 64.0D);
        FALLBACK_INTENSITY = builder.comment("走 itemOverrides 的非方块物品使用的强度")
                .defineInRange("fallbackIntensity", 1.0D, 0.0D, 4.0D);
        ITEM_OVERRIDES = builder.comment("物品 id → #RRGGBB，例：\"minecraft:glowstone_dust=#FFD27F\"")
                .defineListAllowEmpty(List.of("itemOverrides"), () -> List.<String>of(),
                        o -> o instanceof String s && s.contains("="));
        builder.pop();

        SPEC = builder.build();
    }

    private BridgeSettings() {
    }

    public static boolean enabled() {
        return ENABLED.get();
    }

    public static int maxExtraLights() {
        return MAX_EXTRA_LIGHTS.get();
    }

    public static boolean debugLog() {
        return DEBUG_LOG.get();
    }

    public static double fallbackRadius() {
        return FALLBACK_RADIUS.get();
    }

    public static double fallbackIntensity() {
        return FALLBACK_INTENSITY.get();
    }

    /** 物品 id → 颜色字符串。内容变化时重新解析。 */
    public static Map<String, String> itemOverrides() {
        List<? extends String> raw = ITEM_OVERRIDES.get();
        if (!Objects.equals(raw, cachedRawOverrides)) {
            cachedRawOverrides = raw;
            cachedOverrides = parse(raw);
        }
        return cachedOverrides;
    }

    private static Map<String, String> parse(List<? extends String> raw) {
        Map<String, String> parsed = new LinkedHashMap<>();
        for (String entry : raw) {
            int separator = entry.indexOf('=');
            if (separator <= 0 || separator == entry.length() - 1) {
                LddShine.LOGGER.warn("[lddshine] itemOverrides 条目格式非法，已忽略：{}", entry);
                continue;
            }
            String id = entry.substring(0, separator).trim();
            String color = entry.substring(separator + 1).trim();
            parsed.put(id, color);
        }
        return Map.copyOf(parsed);
    }
}
