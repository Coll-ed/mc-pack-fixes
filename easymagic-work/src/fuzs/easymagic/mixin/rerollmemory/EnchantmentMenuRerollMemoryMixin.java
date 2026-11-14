package fuzs.easymagic.mixin.rerollmemory;

import fuzs.easymagic.mixin.accessor.EnchantmentMenuAccessor;
import fuzs.easymagic.rerollmemory.RerollMemory;
import fuzs.easymagic.rerollmemory.RerollMemoryConfig;
import fuzs.easymagic.rerollmemory.RerollMemoryTargets;
import fuzs.easymagic.world.inventory.ModEnchantmentMenu;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.stream.Stream;

/**
 * 在唯一的附魔生成点 {@code EnchantmentMenu#getEnchantmentList} 上做两件事：
 *
 *   1. {@link ModifyArg} —— 把交给 {@code selectEnchantment} 的候选流换成
 *      「这个池子没展示过的那部分」。
 *
 *   2. {@link Inject}(RETURN) —— **空池子保护**：如果过滤后的结果为空，
 *      就用原版方法本身（通过 Easy Magic 的 accessor）**不过滤地重跑一次**，
 *      返回原版结果。
 *
 * 为什么保护必须放在 RETURN：
 *   候选流是**整张附魔表 tag**，而"这件物品能不能附"与"费用区间对不对"
 *   是 {@code selectEnchantment} 内部才筛的。所以"过滤后还有候选"并不等于
 *   "过滤后还有可用的附魔"。实测（debug.log）：63 次界面刷新里有 33 次
 *   某个槽位因为过滤而拿不到任何附魔 ⇒ 该格空白、连费用都不显示、点不动。
 *
 * 为什么用"重跑原版方法"而不是自己复刻：
 *   原版在方法开头会 {@code random.setSeed(enchantmentSeed.get() + slot)}，
 *   并且对书还有一条额外剔除逻辑。重跑一遍这些全部自动一致（含 RNG 轨迹，
 *   所以提示与实际附上的附魔仍然对得上），也不会有第二份实现。
 *
 * 另外：附魔完成后的清池子仍挂在 {@code clickMenuButton}（只认返回 true）。
 * 旧版"在 getEnchantmentList 返回处记录整份候选"的注入已删除，
 * 改由 {@link ModEnchantmentMenuClueMixin} 记录界面真正展示的附魔。
 */
@Mixin(EnchantmentMenu.class)
public abstract class EnchantmentMenuRerollMemoryMixin {

    /** 附魔槽容器（0=待附魔物品）。用于在附魔按钮按下时抓取"附魔前"的物品。 */
    @Shadow
    private Container enchantSlots;

    /** 本次生成附魔时用的物品（在 getEnchantmentList 开头抓取）。 */
    @Unique
    private ItemStack rerollMemory$rollingItem;

    /** 按下附魔按钮时槽里的物品（附魔**前**的那份），用于决定清哪个类别的池子。 */
    @Unique
    private ItemStack rerollMemory$enchantTarget;

    /** 本次调用是否**真的**过滤掉了候选（空池子保护只在 true 时允许干预）。 */
    @Unique
    private boolean rerollMemory$filterApplied;

    /** 是否正在做空池子保护的重跑（防止自己再触发自己）。 */
    @Unique
    private boolean rerollMemory$retrying;

    /**
     * 抓取本次生成附魔用的物品，并复位每次调用的状态。
     * 复位很重要：若某次调用因为附魔表 tag 为空而提前返回，
     * ModifyArg 根本不会执行，标志位会留下上一次的值。
     */
    @Inject(method = RerollMemoryTargets.GET_ENCHANTMENT_LIST, at = @At("HEAD"))
    private void rerollMemory$captureRollingItem(RegistryAccess registryAccess,
                                                 ItemStack stack,
                                                 int slot,
                                                 int cost,
                                                 CallbackInfoReturnable<List<EnchantmentInstance>> callback) {
        this.rerollMemory$rollingItem = stack;
        this.rerollMemory$filterApplied = false;
    }

    /**
     * 过滤候选附魔。
     * 返回类型必须与目标参数一致（{@code Stream<Holder<Enchantment>>}）。
     */
    @ModifyArg(
            method = RerollMemoryTargets.GET_ENCHANTMENT_LIST,
            at = @At(value = "INVOKE", target = RerollMemoryTargets.SELECT_ENCHANTMENT),
            index = RerollMemoryTargets.SELECT_ENCHANTMENT_CANDIDATES_INDEX)
    private Stream<Holder<Enchantment>> rerollMemory$filterCandidates(Stream<Holder<Enchantment>> candidates) {
        if (this.rerollMemory$retrying) {
            // 保护性重跑：这一次必须完全按原版来，一行都不许过滤
            this.rerollMemory$filterApplied = false;
            return candidates;
        }
        RerollMemory.FilterOutcome outcome = RerollMemory.filterCandidates(
                rerollMemory$player(), this.rerollMemory$rollingItem, candidates);
        this.rerollMemory$filterApplied = outcome.removedAny();
        return outcome.stream();
    }

    /**
     * 空池子保护：过滤后结果为空时，用原版方法本身不过滤地重跑一次。
     *
     * 三个前置条件缺一不可：
     *   · 本次确实过滤过（否则"空"是原版本就有的现象，不该干预）
     *   · 不在重跑中（防递归）
     *   · 拿得到 accessor（拿不到就只记 WARN，不静默）
     */
    @Inject(method = RerollMemoryTargets.GET_ENCHANTMENT_LIST, at = @At("RETURN"), cancellable = true)
    private void rerollMemory$protectEmptyPool(RegistryAccess registryAccess,
                                               ItemStack stack,
                                               int slot,
                                               int cost,
                                               CallbackInfoReturnable<List<EnchantmentInstance>> callback) {
        List<EnchantmentInstance> result = callback.getReturnValue();
        if (result != null && !result.isEmpty()) {
            return;
        }
        if (!this.rerollMemory$filterApplied || this.rerollMemory$retrying) {
            return;
        }
        Object self = this;
        if (!(self instanceof EnchantmentMenuAccessor accessor)) {
            RerollMemoryConfig.LOGGER.warn(
                    "empty-pool protection unavailable: {} was not applied to {} - "
                            + "this slot stays empty (report this line)",
                    RerollMemoryTargets.ENCHANTMENT_MENU_ACCESSOR,
                    RerollMemoryTargets.ENCHANTMENT_MENU);
            return;
        }
        RerollMemory.logEmptyPoolProtection(rerollMemory$player(), stack, slot);
        this.rerollMemory$retrying = true;
        try {
            callback.setReturnValue(accessor.callGetEnchantmentList(registryAccess, stack, slot, cost));
        } finally {
            this.rerollMemory$retrying = false;
        }
    }

    /** 附魔按钮按下时抓取槽里的物品（此时还是附魔前的原件）。 */
    @Inject(method = RerollMemoryTargets.CLICK_MENU_BUTTON, at = @At("HEAD"))
    private void rerollMemory$captureEnchantTarget(Player player,
                                                  int buttonId,
                                                  CallbackInfoReturnable<Boolean> callback) {
        this.rerollMemory$enchantTarget = this.enchantSlots.getItem(0);
    }

    /**
     * 附魔完成后清空池子。
     *
     * 原版 clickMenuButton 只有在**真正附上附魔**时才返回 true
     * （花费不够、等级不够、物品为空等一律 false），所以只认 true 即可。
     * Easy Magic 的刷新是 buttonId 4，在它自己的重写里就处理掉了、不会走到这里。
     */
    @Inject(method = RerollMemoryTargets.CLICK_MENU_BUTTON, at = @At("RETURN"))
    private void rerollMemory$clearAfterEnchant(Player player,
                                                int buttonId,
                                                CallbackInfoReturnable<Boolean> callback) {
        if (Boolean.TRUE.equals(callback.getReturnValue())) {
            RerollMemory.clear(player, this.rerollMemory$enchantTarget);
        }
    }

    /**
     * 取当前附魔台的玩家。
     *
     * {@code ModEnchantmentMenu.player} 在两个构造器里都会被赋值。
     * 先转成 Object 再 instanceof：编译期本类与 ModEnchantmentMenu 无继承关系，
     * 直接 instanceof 会被 javac 判为不可能而报错。
     */
    @Unique
    private Player rerollMemory$player() {
        Object self = this;
        if (self instanceof ModEnchantmentMenu menu) {
            return menu.player;
        }
        return null;
    }
}
