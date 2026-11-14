package com.yglcs.fpafix.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.yglcs.fpafix.FirstPersonArmorFix;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 纯诊断：不取消任何渲染，只在「第一人称 + 渲染本地玩家盔甲层」时打印调用栈，
 * 用来定位到底是谁在第一人称把盔甲层带上来的。
 */
@Mixin(HumanoidArmorLayer.class)
public abstract class HumanoidArmorLayerMixin {

    /** 同一秒内只打印一次，避免刷屏。 */
    private static final long LOG_COOLDOWN_MS = 1000L;
    private static long lastLogMillis;

    @Inject(
            method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/entity/LivingEntity;FFFFFF)V",
            at = @At("HEAD"))
    private void fpad$logFirstPersonArmorRender(PoseStack poseStack, MultiBufferSource buffer, int packedLight,
                                                LivingEntity entity, float limbSwing, float limbSwingAmount,
                                                float partialTick, float ageInTicks, float netHeadYaw, float headPitch,
                                                CallbackInfo ci) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || entity != minecraft.player) {
            return;
        }
        if (!minecraft.options.getCameraType().isFirstPerson()) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now - lastLogMillis < LOG_COOLDOWN_MS) {
            return;
        }
        lastLogMillis = now;
        FirstPersonArmorFix.LOGGER.warn("[fpad] 第一人称下有人调用了本地玩家的盔甲层，调用栈如下：",
                new Throwable("fpad-caller"));
    }
}
