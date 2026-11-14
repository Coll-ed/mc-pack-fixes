/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.platform.GlStateManager
 *  com.mojang.blaze3d.platform.GlStateManager$class_4534
 *  com.mojang.blaze3d.platform.GlStateManager$class_4535
 *  com.mojang.blaze3d.platform.TextureUtil
 *  com.mojang.blaze3d.systems.RenderSystem
 *  net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext
 *  net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents$AfterTranslucent
 *  net.minecraft.class_1163
 *  net.minecraft.class_1920
 *  net.minecraft.class_2338
 *  net.minecraft.class_243
 *  net.minecraft.class_286
 *  net.minecraft.class_287
 *  net.minecraft.class_289
 *  net.minecraft.class_290
 *  net.minecraft.class_293$class_5596
 *  net.minecraft.class_310
 *  net.minecraft.class_4608
 *  net.minecraft.class_761
 *  net.minecraft.class_9801
 *  org.joml.Matrix4f
 *  org.lwjgl.system.MemoryUtil
 */
package com.goby56.wakes.render;

import com.goby56.wakes.WakesClient;
import com.goby56.wakes.render.BlendingFunction;
import com.goby56.wakes.render.RenderType;
import com.goby56.wakes.utils.WakeColor;
import com.goby56.wakes.utils.WakeHandler;
import com.goby56.wakes.utils.WakeNode;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.ArrayList;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.class_1163;
import net.minecraft.class_1920;
import net.minecraft.class_2338;
import net.minecraft.class_243;
import net.minecraft.class_286;
import net.minecraft.class_287;
import net.minecraft.class_289;
import net.minecraft.class_290;
import net.minecraft.class_293;
import net.minecraft.class_310;
import net.minecraft.class_4608;
import net.minecraft.class_761;
import net.minecraft.class_9801;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryUtil;

public class WakeTextureRenderer
implements WorldRenderEvents.AfterTranslucent {
    public void afterTranslucent(WorldRenderContext context) {
        BlendingFunction blendMode;
        if (!WakesClient.CONFIG_INSTANCE.renderWakes) {
            return;
        }
        WakeHandler wakeHandler = WakeHandler.getInstance();
        if (wakeHandler == null || wakeHandler.resetScheduled) {
            return;
        }
        ArrayList<WakeNode> nodes = wakeHandler.getVisible(context.frustum());
        Matrix4f matrix = context.matrixStack().method_23760().method_23761();
        RenderSystem.enableBlend();
        context.lightmapTextureManager().method_3316();
        if (class_310.method_29611()) {
            WakesClient.CONFIG_INSTANCE.blendMode = BlendingFunction.SCREEN;
        }
        if ((blendMode = BlendingFunction.getBlendFunc()) == BlendingFunction.DEFAULT) {
            RenderSystem.defaultBlendFunc();
        } else {
            RenderSystem.blendFunc((GlStateManager.class_4535)blendMode.srcFactor, (GlStateManager.class_4534)blendMode.dstFactor);
        }
        int res = wakeHandler.resolution.res;
        if (wakeHandler.glFoamTexId == -1) {
            wakeHandler.glFoamTexId = WakeTextureRenderer.initTexture(res);
        }
        if (wakeHandler.glWakeTexId == -1) {
            wakeHandler.glWakeTexId = WakeTextureRenderer.initTexture(res);
        }
        if (wakeHandler.foamImgPtr == -1L) {
            wakeHandler.foamImgPtr = MemoryUtil.nmemAlloc((long)((long)res * (long)res * 4L));
        }
        if (wakeHandler.wakeImgPtr == -1L) {
            wakeHandler.wakeImgPtr = MemoryUtil.nmemAlloc((long)((long)res * (long)res * 4L));
        }
        for (WakeNode node : nodes) {
            float b;
            float g;
            float r;
            if (node.isDead() || res != node.res) continue;
            class_243 screenSpace = node.getPos().method_1019(context.camera().method_19326().method_22882());
            float x = (float)screenSpace.field_1352;
            float y = (float)screenSpace.field_1351;
            float z = (float)screenSpace.field_1350;
            for (int i = 0; i < res; ++i) {
                for (int j = 0; j < res; ++j) {
                    float avg = (node.u[0][i + 1][j + 1] + node.u[1][i + 1][j + 1] + node.u[2][i + 1][j + 1]) / 3.0f;
                    WakeColor wakeCol = WakeColor.getColor(avg);
                    if (wakeCol == WakeColor.WHITE) {
                        MemoryUtil.memPutInt((long)(wakeHandler.foamImgPtr + ((long)i * (long)res + (long)j) * 4L), (int)wakeCol.argb);
                        MemoryUtil.memPutInt((long)(wakeHandler.wakeImgPtr + ((long)i * (long)res + (long)j) * 4L), (int)0);
                        continue;
                    }
                    MemoryUtil.memPutInt((long)(wakeHandler.foamImgPtr + ((long)i * (long)res + (long)j) * 4L), (int)0);
                    MemoryUtil.memPutInt((long)(wakeHandler.wakeImgPtr + ((long)i * (long)res + (long)j) * 4L), (int)wakeCol.argb);
                }
            }
            int waterCol = class_1163.method_4961((class_1920)context.world(), (class_2338)node.blockPos());
            int light = class_761.method_23794((class_1920)context.world(), (class_2338)node.blockPos());
            if (WakesClient.CONFIG_INSTANCE.useWaterBlending) {
                r = (float)(waterCol >> 16 & 0xFF) / 255.0f;
                g = (float)(waterCol >> 8 & 0xFF) / 255.0f;
                b = (float)(waterCol & 0xFF) / 255.0f;
            } else {
                r = 1.0f;
                g = 1.0f;
                b = 1.0f;
            }
            float a = (float)((-Math.pow(node.t, 2.0) + 1.0) * Math.pow(WakesClient.CONFIG_INSTANCE.wakeOpacity, WakesClient.CONFIG_INSTANCE.blendMode.canVaryOpacity ? 1.0 : 0.0));
            WakeTextureRenderer.renderTexture(res, wakeHandler.glWakeTexId, wakeHandler.wakeImgPtr, matrix, x, y, z, r, g, b, a, light);
            WakeTextureRenderer.renderTexture(res, wakeHandler.glFoamTexId, wakeHandler.foamImgPtr, matrix, x, y, z, 1.0f, 1.0f, 1.0f, a, light);
        }
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableCull();
    }

    private static void renderTexture(int resolution, int textureID, long texture, Matrix4f matrix, float x, float y, float z, float r, float g, float b, float a, int light) {
        GlStateManager._bindTexture((int)textureID);
        GlStateManager._pixelStore((int)3314, (int)0);
        GlStateManager._pixelStore((int)3316, (int)0);
        GlStateManager._pixelStore((int)3315, (int)0);
        GlStateManager._pixelStore((int)3317, (int)4);
        GlStateManager._texSubImage2D((int)3553, (int)0, (int)0, (int)0, (int)resolution, (int)resolution, (int)6408, (int)5121, (long)texture);
        RenderSystem.setShaderTexture((int)0, (int)textureID);
        RenderSystem.enableDepthTest();
        RenderSystem.setShader(RenderType.getProgram());
        class_287 buffer = class_289.method_1348().method_60827(class_293.class_5596.field_27382, class_290.field_1580);
        buffer.method_22918(matrix, x, y, z).method_22915(r, g, b, a).method_22913(0.0f, 0.0f).method_22922(class_4608.field_21444).method_60803(light).method_22914(0.0f, 1.0f, 0.0f);
        buffer.method_22918(matrix, x, y, z + 1.0f).method_22915(r, g, b, a).method_22913(0.0f, 1.0f).method_22922(class_4608.field_21444).method_60803(light).method_22914(0.0f, 1.0f, 0.0f);
        buffer.method_22918(matrix, x + 1.0f, y, z + 1.0f).method_22915(r, g, b, a).method_22913(1.0f, 1.0f).method_22922(class_4608.field_21444).method_60803(light).method_22914(0.0f, 1.0f, 0.0f);
        buffer.method_22918(matrix, x + 1.0f, y, z).method_22915(r, g, b, a).method_22913(1.0f, 0.0f).method_22922(class_4608.field_21444).method_60803(light).method_22914(0.0f, 1.0f, 0.0f);
        class_286.method_43433((class_9801)buffer.method_60800());
    }

    private static int initTexture(int resolution) {
        int texId = TextureUtil.generateTextureId();
        GlStateManager._bindTexture((int)texId);
        GlStateManager._texParameter((int)3553, (int)33085, (int)0);
        GlStateManager._texParameter((int)3553, (int)33082, (int)0);
        GlStateManager._texParameter((int)3553, (int)33083, (int)0);
        GlStateManager._texParameter((int)3553, (int)34049, (float)0.0f);
        GlStateManager._texParameter((int)3553, (int)10241, (int)9728);
        GlStateManager._texParameter((int)3553, (int)10240, (int)9728);
        GlStateManager._texImage2D((int)3553, (int)0, (int)6408, (int)resolution, (int)resolution, (int)0, (int)6408, (int)5121, null);
        return texId;
    }
}

