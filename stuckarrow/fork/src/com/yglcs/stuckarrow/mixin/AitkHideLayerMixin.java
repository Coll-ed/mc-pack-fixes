package com.yglcs.stuckarrow.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 隐藏 AITK 的随机假箭层（无条件取消）。
 *
 * <p>理由（实测结论）：AITK 的 {@code GenericStuckInBodyLayer#render} 内部会重设 PoseStack，
 * 我们在它外面做的 translate 根本不会生效 —— 所以在它的层里改位置是死路。
 * 现在改为：这一层直接跳过，箭头由 {@code StuckArrowRenderHandler} 在
 * {@code RenderLivingEvent.Post}（实体局部坐标系、pose 由我们自己掌控）绘制。
 */
@Mixin(targets = "com.github.razorplay01.aitk.util.GenericStuckInBodyLayer")
public abstract class AitkHideLayerMixin {

   @Inject(method = "render", at = @At("HEAD"), cancellable = true)
   private void stuckarrow$hideAitkLayer(PoseStack poseStack, MultiBufferSource buffer, int light,
                                         LivingEntity entity, float limbSwing, float limbSwingAmount,
                                         float partialTick, float ageInTicks, float netHeadYaw,
                                         float headPitch, CallbackInfo ci) {
      ci.cancel();
   }
}
