package com.yglcs.geofpv.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.yglcs.geofpv.GeoFirstPersonFix;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * GeckoLib 的 {@code GeoArmorRenderer} 只按 HumanoidModel 部件可见性决定骨骼是否绘制，
 * 整个类没有任何相机类型判定（javap 核对：camera/firstperson 命中 0 次）。
 *
 * <p>原版是逐部位调用 {@code renderToBuffer}，所以在这里取消一次即可覆盖头/胸/腿/靴四个部位。</p>
 */
@Mixin(targets = "software.bernie.geckolib.renderer.GeoArmorRenderer")
public abstract class GeoArmorRendererMixin {

    /** GeckoLib 在 prepForRender 里写入、渲染时用来判断“正在画谁”的字段。 */
    @Shadow
    protected Entity currentEntity;

    @Inject(
            method = "renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;III)V",
            at = @At("HEAD"),
            cancellable = true)
    private void geofpv$skipInFirstPerson(PoseStack poseStack, VertexConsumer buffer, int packedLight,
                                          int packedOverlay, int color, CallbackInfo ci) {
        if (!GeoFirstPersonFix.enabled()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.options == null) {
            return;
        }
        if (currentEntity != minecraft.player) {
            return;   // 只针对本地玩家自己；第三人称、其他实体照常
        }
        if (minecraft.options.getCameraType().isFirstPerson()) {
            ci.cancel();
        }
    }
}
