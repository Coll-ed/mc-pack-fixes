package com.yglcs.stuckarrow.mixin;

import com.yglcs.stuckarrow.StuckArrows;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 在箭命中生物的那一刻采集「命中点」与「弹道方向」。
 *
 * <p>依据 1.21.1 官方类：{@code AbstractArrow#onHitEntity(EntityHitResult)}。
 * {@code EntityHitResult#getLocation()} 就是世界坐标下的命中点；
 * 箭自身的 {@code getDeltaMovement()} 归一化即弹道方向，用来还原 yaw/pitch，
 * 这样假箭的朝向与原版箭一致（原版就是按弹道朝向渲染的）。
 */
@Mixin(AbstractArrow.class)
public abstract class AbstractArrowHitMixin {

   private static final org.slf4j.Logger LOGGER = com.mojang.logging.LogUtils.getLogger();

   @Inject(method = "onHitEntity", at = @At("HEAD"))
   private void stuckarrow$captureHit(EntityHitResult result, CallbackInfo ci) {
      Entity victim = result.getEntity();
      if (victim == null || victim.level() == null) {
         return;
      }
      AbstractArrow self = (AbstractArrow)(Object)this;

      // 命中点求法（按 1.21.1 官方 API 逐级降级，每级都可验证）：
      // ① 官方 ProjectileUtil 同款：上一 tick 位置 → 当前位置 与包围盒裁剪；
      // ② 若时间步导致箭已进入盒内/速度被清零（实测 clip 返回 empty），
      //    就把箭当前位置夹到包围盒表面 —— 得到“入射面”上的一个点，非零且贴合身体。
      Vec3 motionForHit = self.getDeltaMovement();
      Vec3 from = self.position().subtract(motionForHit);
      Vec3 to = self.position();
      net.minecraft.world.phys.AABB box = victim.getBoundingBox();
      Vec3 hit = box.inflate(0.3D).clip(from, to).orElse(null);
      String how = "clip";
      if (hit == null) {
         how = "clamp";
         hit = new Vec3(
            net.minecraft.util.Mth.clamp(to.x, box.minX, box.maxX),
            net.minecraft.util.Mth.clamp(to.y, box.minY, box.maxY),
            net.minecraft.util.Mth.clamp(to.z, box.minZ, box.maxZ));
      }

      Vec3 offset = hit.subtract(victim.position());

      Vec3 motion = self.getDeltaMovement();
      if (motion.lengthSqr() < 1.0E-6D) {
         motion = new Vec3(0.0D, -1.0D, 0.0D);
      }
      Vec3 dir = motion.normalize();
      float yaw = (float)(Math.atan2(dir.x, dir.z) * 180.0D / Math.PI);
      float pitch = (float)(-Math.asin(dir.y) * 180.0D / Math.PI);

      StuckArrows.add(victim.getId(), offset, yaw, pitch, victim.level().getGameTime());
      LOGGER.info("[stuckarrow] hit victim={} via={} offset=({}, {}, {}) yaw={} pitch={}",
         victim.getId(), how, offset.x, offset.y, offset.z, yaw, pitch);
   }
}
