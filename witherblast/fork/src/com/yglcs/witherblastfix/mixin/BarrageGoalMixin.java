package com.yglcs.witherblastfix.mixin;

import com.yglcs.witherblastfix.WitherBlastFix;
import com.yglcs.witherblastfix.WitherBlastFixSymbols;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.WitherSkull;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 齐射（Barrage）发射的正是原版 {@link WitherSkull}：{@code new WitherSkull(level, wither, dir)}
 * 然后 {@code level.addFreshEntity(skull)}。这里在“放进世界”的那一刻给它打持久化标记，
 * 之后爆炸时就能精确识别出「这颗是齐射发的」。
 */
@Mixin(targets = WitherBlastFixSymbols.BARRAGE_GOAL)
public abstract class BarrageGoalMixin {

    @Redirect(
            method = WitherBlastFixSymbols.BARRAGE_TICK,
            at = @At(value = "INVOKE", target = WitherBlastFixSymbols.ADD_FRESH_ENTITY))
    private boolean witherblastfix$tagBarrageSkull(Level level, Entity entity) {
        if (WitherBlastFix.enabled() && entity instanceof WitherSkull skull) {
            skull.getPersistentData().putBoolean(WitherBlastFixSymbols.NO_GRIEF_TAG, true);
        }
        return level.addFreshEntity(entity);
    }
}
