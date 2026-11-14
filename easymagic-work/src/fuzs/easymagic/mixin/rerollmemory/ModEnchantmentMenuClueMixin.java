package fuzs.easymagic.mixin.rerollmemory;

import fuzs.easymagic.rerollmemory.RerollMemory;
import fuzs.easymagic.rerollmemory.RerollMemoryTargets;
import fuzs.easymagic.world.inventory.ModEnchantmentMenu;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 记录**界面真正展示给玩家的**附魔 —— v2 的核心修正。
 *
 * ── 为什么必须挂在这里，而不是 getEnchantmentList 的返回处 ──
 * {@code getEnchantmentList} 返回的是 selectEnchantment 算出来的**整份**候选
 * （实测一次 4~10 个），而玩家在界面上看到的由 Easy Magic 的
 * {@code createClue} 决定：enchantment_hint=SINGLE 时**每格只显示 1 个**
 * （NONE 显示 0 个，ALL 显示全部）。
 *
 * 旧实现在 getEnchantmentList 返回处把整份候选都记成"已刷过"，
 * 于是玩家根本没见过的附魔被静默烧掉 —— 日志实测：书的池子在 14 次刷新里
 * 涨到 45 种，远超玩家实际看到的东西，池子因此提前枯竭。
 *
 * 挂在 {@code updateLevelsAndClues} 的 RETURN，读的是它刚写好的 {@code clues}，
 * 也就是**同一份会被 broadcastChanges 发给客户端渲染的内容** ——
 * 因此三种 hint 模式下"看到什么就消耗什么"都自动成立。
 *
 * ── 为什么不用 @Shadow ──
 * {@code clues} 与 {@code player} 都是 public final 字段，直接强转读取即可，
 * 少一层对 Mixin 字段语义的依赖（同一招已用在 EnchantmentMenuRerollMemoryMixin）。
 */
@Mixin(ModEnchantmentMenu.class)
public abstract class ModEnchantmentMenuClueMixin {

    /**
     * 新一轮提示开始计算：先把**上一轮**展示过的提交进筛选池。
     *
     * 这是"提示与落子必须一致"的关键：本方法只在刷新/开台时触发，
     * 真正附魔走的 clickMenuButton 不会触发它，所以玩家点击时
     * 候选流与提示生成时完全相同。
     */
    @Inject(method = RerollMemoryTargets.UPDATE_LEVELS_AND_CLUES, at = @At("HEAD"))
    private void rerollMemory$beginRefresh(RegistryAccess registryAccess,
                                           ItemStack stack,
                                           int bookshelfCount,
                                           CallbackInfo callback) {
        Object self = this;
        if (self instanceof ModEnchantmentMenu menu) {
            RerollMemory.beginRefresh(menu.player, stack);
        }
    }

    /**
     * 提示算完后记录一次。
     *
     * 参数顺序必须与 {@code updateLevelsAndClues(RegistryAccess, ItemStack, int)} 一致，
     * 第三个参数是书架数量（不是槽位下标）。
     */
    @Inject(method = RerollMemoryTargets.UPDATE_LEVELS_AND_CLUES, at = @At("RETURN"))
    private void rerollMemory$recordShownClues(RegistryAccess registryAccess,
                                               ItemStack stack,
                                               int bookshelfCount,
                                               CallbackInfo callback) {
        Object self = this;
        if (self instanceof ModEnchantmentMenu menu) {
            RerollMemory.recordShown(menu.player, stack, menu.clues);
        }
    }
}
