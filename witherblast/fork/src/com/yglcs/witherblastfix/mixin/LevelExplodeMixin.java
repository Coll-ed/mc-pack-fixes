package com.yglcs.witherblastfix.mixin;

import com.yglcs.witherblastfix.WitherBlastFix;
import com.yglcs.witherblastfix.WitherBlastFixSymbols;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.WitherSkull;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 原版 {@code WitherSkull#onHit} 调的是
 * {@code level.explode(this, x, y, z, 1.0F, false, ExplosionInteraction.MOB)}，
 * MOB 语义就是「mobGriefing 开着就破坏方块」。
 *
 * <p>这里只拦带标记的来源（齐射炮弹），把 interaction 换成 {@code NONE}：
 * 方块破坏关闭，实体伤害、粒子、音效、击退全部照旧。
 * 普通凋零头（Boss 普通攻击）不带标记，一行都不受影响。</p>
 */
@Mixin(Level.class)
public abstract class LevelExplodeMixin {

    @Inject(
            method = "explode(Lnet/minecraft/world/entity/Entity;DDDFZLnet/minecraft/world/level/Level$ExplosionInteraction;)Lnet/minecraft/world/level/Explosion;",
            at = @At("HEAD"),
            cancellable = true)
    private void witherblastfix$skipBlockDamage(Entity source,
                                                double x, double y, double z,
                                                float power, boolean fire,
                                                Level.ExplosionInteraction interaction,
                                                CallbackInfoReturnable<Explosion> cir) {
        // 防递归：我们下面自己发起的 NONE 爆炸会再次进到这里
        if (interaction == Level.ExplosionInteraction.NONE || !WitherBlastFix.enabled()) {
            return;
        }
        if (!(source instanceof WitherSkull skull)) {
            return;
        }
        if (!skull.getPersistentData().getBoolean(WitherBlastFixSymbols.NO_GRIEF_TAG)) {
            return;
        }
        Level self = (Level) (Object) this;
        cir.setReturnValue(self.explode(source, x, y, z, power, fire, Level.ExplosionInteraction.NONE));
    }
}
