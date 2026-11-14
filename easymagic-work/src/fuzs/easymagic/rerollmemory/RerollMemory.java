package fuzs.easymagic.rerollmemory;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentInstance;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

/**
 * 「刷过的附魔不再出现」的记忆。
 *
 * ── 唯一挂钩点 ──
 * 全游戏只有一个附魔生成点：
 * {@code EnchantmentMenu#getEnchantmentList(RegistryAccess, ItemStack, int, int)}。
 * Easy Magic 的刷新算提示、原版 clickMenuButton 真正附魔，都走它。
 *
 * ── v2 修正（由实机日志 + 反编译证实）──
 *
 * 1) 【记录粒度】旧实现在 getEnchantmentList 返回处记录，
 *    记的是 selectEnchantment 返回的**整份**候选（实测一次记 4~10 个），
 *    而 enchantment_hint=SINGLE 时界面每格只显示 **1** 个。
 *    ⇒ 玩家没见过的附魔被静默"烧掉"，池子远早于观感就空了。
 *    现在改为在 {@code ModEnchantmentMenu.updateLevelsAndClues} 返回处按 {@code clues}
 *    记录 —— **界面展示什么就消耗什么**（NONE/SINGLE/ALL 三种模式都正确）。
 *
 * 2) 【空池子保护】交给 selectEnchantment 的候选流是**整张附魔表 tag**
 *    （不是"这件物品能附的"）；物品适用性与费用区间是 selectEnchantment
 *    内部才筛的。于是"没见过的"在全表层面几乎永不空（所以旧耗尽分支从不触发），
 *    但过滤后那部分可能**没有一个落在费用区间内** ⇒ 返回空 ⇒ costs[i]=0
 *    ⇒ 该格空白、点不动、连费用都不显示。
 *    保护必须做在 RETURN 处（用原版方法本身重跑一次不过滤的），
 *    本类只负责如实报告"这次到底有没有真的排除掉候选"。
 *
 * ── 记忆范围（规格）──
 * · 池子按 (玩家, 物品类别) 划分；类别 = 物品的 Java 类名（见 {@link #categoryOf}）。
 * · 去重粒度 = 附魔注册 id，不区分等级。
 * · 不落盘：附魔完成清空该类别；关开附魔台不清空；重启清空。
 */
public final class RerollMemory {

    /** 池子标识：一个玩家 + 一种物品类别。用 record 而不是拼字符串，避免拼接歧义。 */
    public record PoolKey(UUID player, String category) {
    }

    /**
     * 一次候选过滤的结果。
     *
     * @param stream     真正交给 selectEnchantment 的候选流
     * @param removedAny 本次是否**真的排除掉了**至少一个候选；
     *                   RETURN 处的空池子保护只在它为 true 时才允许干预 ——
     *                   否则会把"原版本本来就出不了附魔"的格子也赖到自己头上。
     */
    public record FilterOutcome(Stream<Holder<Enchantment>> stream, boolean removedAny) {
    }

    /** 池子 -> 已展示过的附魔 id 集合。 */
    private static final Map<PoolKey, Set<String>> SEEN = new ConcurrentHashMap<>();

    /**
     * 池子 -> **本轮展示过、但尚未提交进 {@link #SEEN}** 的附魔 id。
     *
     * 为什么要延迟提交（v2 的第三个修正）：
     * 界面提示与"真正附魔"是**两次**调用同一个 getEnchantmentList
     * （先算提示；玩家点了按钮再算一次，用同一颗种子）。过滤结果依赖 SEEN，
     * 若算完提示就立刻把展示过的记进 SEEN，玩家点下去那一刻候选流已经变了 ⇒
     *   · 附上的附魔与提示不是同一个（实测："我附魔锋利，实际给了火焰附加"）
     *   · 候选可能被过滤空 ⇒ 点了没反应（实测："有附魔，但附魔不成功"）
     * 所以展示过的先进 PENDING，等**下一次刷新开始**（updateLevelsAndClues HEAD）
     * 再提交 —— 同一轮里"提示"与"落子"看到的就是同一个池子。
     */
    private static final Map<PoolKey, Set<String>> PENDING = new ConcurrentHashMap<>();

    /** 已刷完一轮、且配置为「不重置」的池子（避免重复刷同样的 WARN）。 */
    private static final Set<PoolKey> EXHAUSTED = ConcurrentHashMap.newKeySet();

    /** 物品类别 = 物品的 Java 类名；物品为空返回 null（调用方据此跳过本机制）。 */
    public static String categoryOf(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        return stack.getItem().getClass().getName();
    }

    /**
     * 过滤候选附魔：只把「这个池子没展示过」的交给 selectEnchantment。
     *
     * 在候选阶段过滤的好处：加权抽取本身只在未见过的附魔里进行，
     * 不需要"先抽再剔除"（那样可能把结果剔空，还会扭曲权重）。
     *
     * 注意：本方法**无法**保证过滤后的结果非空 —— 见类注释第 2 条。
     * 空池子保护由调用方在 RETURN 处做。
     */
    public static FilterOutcome filterCandidates(Player player, ItemStack stack,
                                                 Stream<Holder<Enchantment>> candidates) {
        RerollMemoryConfig.maybeReload();
        PoolKey key = poolKey(player, stack);
        if (key == null || !RerollMemoryConfig.enabled()) {
            return new FilterOutcome(candidates, false);
        }

        // 配置改回「重置」时，解除之前的"已停用"状态
        if (RerollMemoryConfig.resetWhenExhausted()) {
            EXHAUSTED.remove(key);
        }

        // 流只能用一次，先物化（候选池是标签里的全部附魔，规模不大）
        List<Holder<Enchantment>> all = candidates.toList();
        if (all.isEmpty()) {
            return new FilterOutcome(all.stream(), false);
        }

        Set<String> seen = SEEN.get(key);
        if (seen == null || seen.isEmpty()) {
            return new FilterOutcome(all.stream(), false);
        }

        List<Holder<Enchantment>> unseen = removeSeen(all, RerollMemory::keyOf, seen);
        if (!unseen.isEmpty()) {
            return new FilterOutcome(unseen.stream(), unseen.size() < all.size());
        }

        // 池子被刷完了：这个类别能出的附魔都展示过一遍
        SEEN.remove(key);
        if (RerollMemoryConfig.resetWhenExhausted()) {
            RerollMemoryConfig.LOGGER.info(
                    "[{}] every candidate enchantment was shown ({} kinds); memory cleared, new round starts",
                    shortCategory(key), all.size());
        } else {
            EXHAUSTED.add(key);
            RerollMemoryConfig.LOGGER.warn(
                    "[{}] every candidate enchantment was shown ({} kinds); resetWhenExhausted=false "
                            + "=> filtering is off from now on (vanilla behaviour)",
                    shortCategory(key), all.size());
        }
        return new FilterOutcome(all.stream(), false);
    }

    /**
     * 记录**界面真正展示给玩家的**附魔。
     *
     * 调用点：{@code ModEnchantmentMenu.updateLevelsAndClues} 的 RETURN，读 {@code clues}。
     * 这是 v2 的核心修正（见类注释第 1 条）。
     *
     * @param clues 每个槽位的提示列表；createClue 已按 enchantment_hint 决定里面
     *              放 0 个（NONE）/ 1 个（SINGLE）/ 全部（ALL）
     */
    public static void recordShown(Player player, ItemStack stack, List<List<EnchantmentInstance>> clues) {
        if (clues == null || clues.isEmpty()) {
            return;
        }
        PoolKey key = poolKey(player, stack);
        if (key == null || !RerollMemoryConfig.enabled()) {
            return;
        }
        if (EXHAUSTED.contains(key)) {
            return;
        }
        Set<String> pending = PENDING.computeIfAbsent(key, k -> ConcurrentHashMap.newKeySet());
        Set<String> committed = SEEN.getOrDefault(key, Set.of());
        List<String> newlyShown = new ArrayList<>();
        for (List<EnchantmentInstance> clue : clues) {
            if (clue == null) {
                continue;
            }
            for (EnchantmentInstance instance : clue) {
                String id = keyOf(instance.enchantment);
                if (id != null && !committed.contains(id) && pending.add(id)) {
                    newlyShown.add(id);
                }
            }
        }
        if (newlyShown.isEmpty()) {
            RerollMemoryConfig.LOGGER.debug(
                    "[{}] refresh showed no new enchantment (pending {} / pool {} kinds) [class={}]",
                    shortCategory(key), pending.size(), committed.size(), fullCategory(key));
        } else {
            RerollMemoryConfig.LOGGER.debug(
                    "[{}] recorded shown enchantments {} (pending {} / pool {} kinds) [class={}]",
                    shortCategory(key), newlyShown, pending.size(), committed.size(), fullCategory(key));
        }
    }

    /**
     * 新一轮提示开始计算：把**上一轮**展示过的提交进筛选池。
     *
     * 调用点：{@code ModEnchantmentMenu.updateLevelsAndClues} 的 HEAD。
     * 这个方法本身内部循环 3 个槽位，所以每次界面刷新只调用一次，
     * 正好是"上一轮结束、下一轮开始"的唯一时刻 —— 此刻上一轮的提示早已失效，
     * 提交它不会影响任何还没点的按钮。
     *
     * 反过来，真正附魔走的 clickMenuButton **不会**调到这里，
     * 所以点击时池子与提示生成时完全一致（见 {@link #PENDING}）。
     */
    public static void beginRefresh(Player player, ItemStack stack) {
        RerollMemoryConfig.maybeReload();
        PoolKey key = poolKey(player, stack);
        if (key == null || !RerollMemoryConfig.enabled() || EXHAUSTED.contains(key)) {
            return;
        }
        Set<String> pending = PENDING.remove(key);
        if (pending == null || pending.isEmpty()) {
            return;
        }
        Set<String> seen = SEEN.computeIfAbsent(key, k -> ConcurrentHashMap.newKeySet());
        List<String> added = new ArrayList<>();
        for (String id : pending) {
            if (seen.add(id)) {
                added.add(id);
            }
        }
        RerollMemoryConfig.LOGGER.debug(
                "[{}] new refresh: committed {} shown enchantment(s) {} (pool now {} kinds) [class={}]",
                shortCategory(key), added.size(), added, seen.size(), fullCategory(key));
    }

    /**
     * 附魔完成后清空池子（回归正常附魔）。
     * 默认只清当前物品类别；clearAllItemPools=true 时清该玩家所有类别。
     *
     * @param stack 按下附魔按钮时槽里的物品（必须是**附魔前**的那份）
     */
    public static void clear(Player player, ItemStack stack) {
        UUID playerId = playerId(player);
        if (playerId == null) {
            return;
        }
        if (RerollMemoryConfig.clearAllItemPools()) {
            clearAll(playerId);
            return;
        }
        PoolKey key = poolKey(player, stack);
        if (key == null) {
            return;
        }
        Set<String> removed = SEEN.remove(key);
        PENDING.remove(key);
        EXHAUSTED.remove(key);
        if (removed != null && !removed.isEmpty()) {
            RerollMemoryConfig.LOGGER.info(
                    "[{}] enchantment applied; filter pool cleared ({} kinds) - back to vanilla enchanting",
                    shortCategory(key), removed.size());
        } else {
            RerollMemoryConfig.LOGGER.debug(
                    "[{}] enchantment applied; filter pool was already empty", shortCategory(key));
        }
    }

    /** 清空该玩家**所有**物品类别的池子。 */
    public static void clearAll(UUID playerId) {
        int before = SEEN.size();
        SEEN.keySet().removeIf(key -> key.player().equals(playerId));
        PENDING.keySet().removeIf(key -> key.player().equals(playerId));
        EXHAUSTED.removeIf(key -> key.player().equals(playerId));
        RerollMemoryConfig.LOGGER.info(
                "enchantment applied; cleared all item-category pools of this player ({} pools removed)",
                before - SEEN.size());
    }

    /**
     * 纯逻辑：从候选里去掉已经在 {@code seen} 里的。
     *
     * 特意做成泛型 + 只依赖一个「取 key」函数，不碰任何 Minecraft 类型 ——
     * 这样这段真正的判断逻辑可以脱离游戏环境单独验证（见 VerifyEasyMagic）。
     *
     * @param keyOf 取 key 的函数；返回 null 表示无法识别，按"没见过"处理（宁多不少）
     * @param seen  已见过的 key 集合；null 视为空集
     * @return 没见过的候选；全被排除时返回空列表（调用方据此判定"池子刷完"）
     */
    public static <T> List<T> removeSeen(List<T> all, java.util.function.Function<T, String> keyOf,
                                         Set<String> seen) {
        List<T> unseen = new ArrayList<>(all.size());
        for (T item : all) {
            String key = keyOf.apply(item);
            if (key == null || seen == null || !seen.contains(key)) {
                unseen.add(item);
            }
        }
        return unseen;
    }

    /**
     * 日志辅助：本次 getEnchantmentList 是否因过滤而付出代价（供 mixin 记 WARN）。
     * 只在真的要干预时调用，避免正常路径产生噪声。
     */
    public static void logEmptyPoolProtection(Player player, ItemStack stack, int slot) {
        PoolKey key = poolKey(player, stack);
        String category = key == null ? "?" : shortCategory(key);
        String item = stack == null || stack.isEmpty() ? "?" : stack.getItem().toString();
        RerollMemoryConfig.LOGGER.warn(
                "[{}] filtering left nothing usable for slot {} (item={}, pool={} kinds) "
                        + "=> empty-pool protection: rerunning this lookup unfiltered (vanilla result)",
                category, slot, item, key == null ? -1 : sizeOf(key));
    }

    /** 某个池子当前记了多少种（仅用于日志）。 */
    private static int sizeOf(PoolKey key) {
        Set<String> seen = SEEN.get(key);
        return seen == null ? 0 : seen.size();
    }

    /** 组装池子标识；玩家无效 / 客户端侧 / 物品为空时返回 null。 */
    public static PoolKey poolKey(Player player, ItemStack stack) {
        UUID id = playerId(player);
        if (id == null) {
            return null;
        }
        String category = categoryOf(stack);
        if (category == null) {
            return null;
        }
        return new PoolKey(id, category);
    }

    /**
     * 取玩家标识。客户端侧返回 null —— 只在服务端记账，避免两端各记一份导致不一致。
     */
    private static UUID playerId(Player player) {
        if (player == null) {
            return null;
        }
        try {
            if (player.level().isClientSide()) {
                return null;
            }
        } catch (Exception e) {
            RerollMemoryConfig.LOGGER.warn(
                    "failed to determine client/server side; skipping reroll memory for this call", e);
            return null;
        }
        return player.getUUID();
    }

    /** 日志用短类别名（去掉包名）。注意会丢失区分度，精确定位请看 fullCategory。 */
    public static String shortCategory(PoolKey key) {
        return shortCategory(key.category());
    }

    /** 日志用短类别名（去掉包名）。 */
    public static String shortCategory(String category) {
        int dot = category.lastIndexOf('.');
        return dot < 0 ? category : category.substring(dot + 1);
    }

    /** 完整类名（不同包的 ArmorItem 在短名里会长得一样，排查时要看这个）。 */
    public static String fullCategory(PoolKey key) {
        return key.category();
    }

    /** 附魔的注册 id；拿不到（未注册的 Holder）时返回 null。 */
    private static String keyOf(Holder<Enchantment> holder) {
        if (holder == null) {
            return null;
        }
        return holder.unwrapKey()
                .map(resourceKey -> {
                    ResourceLocation location = resourceKey.location();
                    return location.toString();
                })
                .orElse(null);
    }

    private RerollMemory() {
    }
}
