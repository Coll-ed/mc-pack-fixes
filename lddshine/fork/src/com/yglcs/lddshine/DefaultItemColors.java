package com.yglcs.lddshine;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * 本模组自带的内置默认表（jar 内资源）：给 Shine 颜色表里没有、但玩家常拿在手上的发光物品兜底。
 *
 * <p>优先级最低：Shine 的 blockLights 表 → 用户配置 itemOverrides → 这张表。</p>
 */
public final class DefaultItemColors {

    /** 内置资源路径（跟随 jar 结构，不是可配置项）。 */
    private static final String RESOURCE_PATH = "/assets/lddshine/defaults/item_colors.json";
    private static final String ITEMS_KEY = "items";
    private static final String COLOR_KEY = "color";
    private static final String RADIUS_KEY = "radius";
    private static final String INTENSITY_KEY = "intensity";

    /** 一个内置条目。 */
    public record Entry(float r, float g, float b, float radius, float intensity) {
    }

    private static Map<String, Entry> entries;
    private static boolean loadFailed;

    private DefaultItemColors() {
    }

    public static Entry get(String itemId) {
        return table().get(itemId);
    }

    private static Map<String, Entry> table() {
        if (entries != null || loadFailed) {
            return entries == null ? Map.of() : entries;
        }
        try (var stream = DefaultItemColors.class.getResourceAsStream(RESOURCE_PATH)) {
            if (stream == null) {
                loadFailed = true;
                LddShine.LOGGER.error("[lddshine] 内置默认表缺失：{}", RESOURCE_PATH);
                return Map.of();
            }
            try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                entries = parse(JsonParser.parseReader(reader));
            }
            return entries;
        } catch (Exception e) {
            loadFailed = true;
            LddShine.LOGGER.error("[lddshine] 内置默认表解析失败，桥接只用 Shine 表与用户覆盖表", e);
            return Map.of();
        }
    }

    private static Map<String, Entry> parse(JsonElement root) {
        JsonObject items = root.getAsJsonObject().getAsJsonObject(ITEMS_KEY);
        if (items == null) {
            LddShine.LOGGER.error("[lddshine] 内置默认表缺少 \"{}\" 节点", ITEMS_KEY);
            return Map.of();
        }
        Map<String, Entry> parsed = new HashMap<>();
        for (Map.Entry<String, JsonElement> raw : items.entrySet()) {
            try {
                JsonObject obj = raw.getValue().getAsJsonObject();
                int rgb = Integer.parseInt(obj.get(COLOR_KEY).getAsString().replace("#", ""), 16)
                        & BridgeConstants.RGB_MASK;
                parsed.put(raw.getKey(), new Entry(
                        channel(rgb, 16),
                        channel(rgb, 8),
                        channel(rgb, 0),
                        obj.get(RADIUS_KEY).getAsFloat(),
                        obj.get(INTENSITY_KEY).getAsFloat()));
            } catch (RuntimeException e) {
                LddShine.LOGGER.warn("[lddshine] 内置默认表条目非法，已跳过：{}", raw.getKey(), e);
            }
        }
        LddShine.LOGGER.info("[lddshine] 内置默认表载入 {} 个物品", parsed.size());
        return Map.copyOf(parsed);
    }

    private static float channel(int rgb, int shift) {
        return ((rgb >> shift) & BridgeConstants.CHANNEL_MAX) / (float) BridgeConstants.CHANNEL_MAX;
    }
}
