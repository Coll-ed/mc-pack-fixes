package fuzs.easymagic.rerollmemory;

import net.neoforged.fml.loading.FMLPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * 「刷新记忆」的配置。一份普通 properties 文件，放在游戏 config 目录（路径取自
 * {@link FMLPaths#CONFIGDIR}，不写死）。
 *
 * 改动文件后**下一次刷附魔即生效**（每次生成附魔时比对一次文件修改时间，开销只有一次 stat）。
 *
 * 文件只在不存在时生成一次，之后不会被覆盖，注释与用户改动都保留。
 */
public final class RerollMemoryConfig {

    /** 日志器。命名独立于 easymagic，方便在本模组自己的日志行里过滤。 */
    public static final Logger LOGGER = LoggerFactory.getLogger("easymagic-rerollmemory");

    /** 配置文件名（位于 {@link FMLPaths#CONFIGDIR} 下）。 */
    private static final String FILE_NAME = "easymagic-rerollmemory.properties";

    // ── 默认值：单一来源。模板文本与「读不到时的回退」都用这里 ──
    /** 是否启用「刷过的附魔不再出现」。 */
    private static final boolean DEFAULT_ENABLED = true;
    /**
     * 候选池被刷完（全部附魔都见过）时是否清空记忆、重新开始一轮。
     * false = 从此刻起不再排除（回到原版行为），会记一条 WARN。
     */
    private static final boolean DEFAULT_RESET_WHEN_EXHAUSTED = true;
    /**
     * 附魔完成时清空的范围：
     * false = 只清当前物品类别的池子（默认；池子本来就按类别独立，给剑附魔不该抹掉书的进度）
     * true  = 清空该玩家所有物品类别的池子
     */
    private static final boolean DEFAULT_CLEAR_ALL_ITEM_POOLS = false;

    private static final String KEY_ENABLED = "enabled";
    private static final String KEY_RESET_WHEN_EXHAUSTED = "resetWhenExhausted";
    private static final String KEY_CLEAR_ALL_ITEM_POOLS = "clearAllItemPools";

    private static Path configFile;
    private static long lastSeenModified = Long.MIN_VALUE;
    private static boolean loaded;
    private static boolean enabled = DEFAULT_ENABLED;
    private static boolean resetWhenExhausted = DEFAULT_RESET_WHEN_EXHAUSTED;
    private static boolean clearAllItemPools = DEFAULT_CLEAR_ALL_ITEM_POOLS;

    /** 每次生成附魔前调一次：文件变了就重读。 */
    public static synchronized void maybeReload() {
        if (!loaded) {
            loaded = true;
            resolvePath();
            readConfig();
            return;
        }
        if (configFile == null) {
            return;
        }
        try {
            long modified = Files.exists(configFile) ? Files.getLastModifiedTime(configFile).toMillis() : -1L;
            if (modified != lastSeenModified) {
                readConfig();
                LOGGER.info("检测到配置文件变更，已重新加载: {}", configFile);
            }
        } catch (Exception e) {
            // 不静默吞：读不到修改时间就明说，并保持上次的值继续跑
            LOGGER.warn("读取配置文件修改时间失败，继续使用当前值: {}", configFile, e);
        }
    }

    public static boolean enabled() {
        return enabled;
    }

    public static boolean resetWhenExhausted() {
        return resetWhenExhausted;
    }

    public static boolean clearAllItemPools() {
        return clearAllItemPools;
    }

    /** 配置文件路径；解析失败时为 null（此时只用默认值）。 */
    public static Path configFile() {
        return configFile;
    }

    private static void resolvePath() {
        try {
            configFile = FMLPaths.CONFIGDIR.get().resolve(FILE_NAME);
        } catch (Exception e) {
            LOGGER.warn("无法解析配置目录，将只使用内置默认值", e);
            configFile = null;
        }
    }

    private static void readConfig() {
        if (configFile == null) {
            return;
        }
        try {
            if (!Files.exists(configFile)) {
                writeTemplate();
            }
            Properties properties = new Properties();
            try (InputStream in = Files.newInputStream(configFile)) {
                properties.load(in);
            }
            enabled = parseBoolean(properties, KEY_ENABLED, DEFAULT_ENABLED);
            resetWhenExhausted = parseBoolean(properties, KEY_RESET_WHEN_EXHAUSTED, DEFAULT_RESET_WHEN_EXHAUSTED);
            clearAllItemPools = parseBoolean(properties, KEY_CLEAR_ALL_ITEM_POOLS, DEFAULT_CLEAR_ALL_ITEM_POOLS);
            lastSeenModified = Files.getLastModifiedTime(configFile).toMillis();
            LOGGER.info("配置已加载: {} (enabled={}, resetWhenExhausted={}, clearAllItemPools={})",
                    configFile, enabled, resetWhenExhausted, clearAllItemPools);
        } catch (Exception e) {
            // 显式失败：记日志并回退到默认值，而不是装作什么都没发生
            enabled = DEFAULT_ENABLED;
            resetWhenExhausted = DEFAULT_RESET_WHEN_EXHAUSTED;
            clearAllItemPools = DEFAULT_CLEAR_ALL_ITEM_POOLS;
            LOGGER.warn("读取配置失败，回退到默认值 (enabled={}, resetWhenExhausted={}, clearAllItemPools={}): {}",
                    enabled, resetWhenExhausted, clearAllItemPools, configFile, e);
        }
    }

    /**
     * 解析一个布尔项。只认 "true"/"false"（忽略大小写）；
     * 其它内容既不当作 true 也不当作 false，而是记 WARN 并用默认值 ——
     * 拼错时不该悄悄变成 false。
     */
    private static boolean parseBoolean(Properties properties, String key, boolean fallback) {
        String raw = properties.getProperty(key);
        if (raw == null) {
            return fallback;
        }
        String value = raw.trim();
        if ("true".equalsIgnoreCase(value)) {
            return true;
        }
        if ("false".equalsIgnoreCase(value)) {
            return false;
        }
        LOGGER.warn("配置项 {} 的值 '{}' 不是 true/false，使用默认值 {}（请改正，避免误解）",
                key, raw, fallback);
        return fallback;
    }

    private static void writeTemplate() throws Exception {
        Path parent = configFile.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        String template = ""
                + "# Easy Magic「刷新记忆」配置（改版新增机制）\n"
                + "# 作用：附魔台每次刷新（reroll）会把刷出来的附魔记下来，之后的刷新不再出现它们，\n"
                + "#       从而不断刷出新附魔。附魔完成时清空池子、回归正常。\n"
                + "# 改完保存即生效，不需要重启游戏。\n"
                + "# 本文件只在不存在时生成，之后不会被覆盖。\n"
                + "\n"
                + "# 池子按 (玩家, 物品类别) 划分。物品类别 = 物品的 Java 类名，所以在原版物品下：\n"
                + "#   钻石剑/铁剑/木剑 -> SwordItem（共用一个池子）\n"
                + "#   镐 / 斧 / 锹 / 锄  -> PickaxeItem / AxeItem / ShovelItem / HoeItem（各一个）\n"
                + "#   4 种盔甲           -> 都是 ArmorItem（共用一个）\n"
                + "#   书                 -> BookItem\n"
                + "\n"
                + "# 是否启用本机制。false = 完全回到 Easy Magic 原版行为。\n"
                + "# 默认: " + DEFAULT_ENABLED + "\n"
                + KEY_ENABLED + "=" + DEFAULT_ENABLED + "\n"
                + "\n"
                + "# 池子被刷完（该类别能出的附魔都见过一遍）时，是否清空记忆、重新开始一轮。\n"
                + "#   true  = 清空后重新开始排除（推荐；可无限循环刷）\n"
                + "#   false = 从此刻起不再排除，回到原版行为（会记一条 WARN）\n"
                + "# 默认: " + DEFAULT_RESET_WHEN_EXHAUSTED + "\n"
                + KEY_RESET_WHEN_EXHAUSTED + "=" + DEFAULT_RESET_WHEN_EXHAUSTED + "\n"
                + "\n"
                + "# 附魔完成时清空的范围。\n"
                + "#   false = 只清当前物品类别的池子（默认；给剑附魔不会抹掉书的进度）\n"
                + "#   true  = 清空该玩家所有物品类别的池子\n"
                + "# 默认: " + DEFAULT_CLEAR_ALL_ITEM_POOLS + "\n"
                + KEY_CLEAR_ALL_ITEM_POOLS + "=" + DEFAULT_CLEAR_ALL_ITEM_POOLS + "\n";
        try (OutputStream out = Files.newOutputStream(configFile)) {
            out.write(template.getBytes(StandardCharsets.UTF_8));
        }
    }

    private RerollMemoryConfig() {
    }
}
