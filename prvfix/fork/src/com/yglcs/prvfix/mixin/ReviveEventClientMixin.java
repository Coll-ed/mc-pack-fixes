package com.yglcs.prvfix.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import com.yglcs.prvfix.PlayerReviveFrameFix;
import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * 目标：PlayerRevive 的客户端事件处理类（模组类，无需 remap）。
 *
 * <p>两处修正：
 * <ul>
 *   <li>{@code tick(RenderGuiLayerEvent.Post)}：每帧只允许执行一次，其余图层触发的调用直接取消；</li>
 *   <li>{@code render(GuiGraphics, List)}：绘制完把 shaderColor / blend / depth 复位，避免污染后续图层。</li>
 * </ul>
 */
@Mixin(targets = "team.creative.playerrevive.client.ReviveEventClient", remap = false)
public abstract class ReviveEventClientMixin {

    @Inject(
            method = "tick(Lnet/neoforged/neoforge/client/event/RenderGuiLayerEvent$Post;)V",
            at = @At("HEAD"),
            cancellable = true,
            remap = false)
    private void prvfix$oncePerFrame(RenderGuiLayerEvent.Post event, CallbackInfo ci) {
        if (!PlayerReviveFrameFix.beginFrame()) {
            ci.cancel();
        }
    }

    @Inject(
            method = "render(Lnet/minecraft/client/gui/GuiGraphics;Ljava/util/List;)V",
            at = @At("RETURN"),
            remap = false)
    private static void prvfix$restoreRenderState(GuiGraphics graphics, List<?> lines, CallbackInfo ci) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
    }
}
