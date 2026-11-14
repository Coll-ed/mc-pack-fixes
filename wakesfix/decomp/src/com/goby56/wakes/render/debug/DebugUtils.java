/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.systems.RenderSystem
 *  net.minecraft.class_243
 *  net.minecraft.class_286
 *  net.minecraft.class_287
 *  net.minecraft.class_289
 *  net.minecraft.class_290
 *  net.minecraft.class_293$class_5596
 *  net.minecraft.class_757
 *  net.minecraft.class_9801
 *  org.joml.Vector4i
 */
package com.goby56.wakes.render.debug;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.class_243;
import net.minecraft.class_286;
import net.minecraft.class_287;
import net.minecraft.class_289;
import net.minecraft.class_290;
import net.minecraft.class_293;
import net.minecraft.class_757;
import net.minecraft.class_9801;
import org.joml.Vector4i;

public class DebugUtils {
    public static Vector4i rgba = new Vector4i(255, 0, 255, 255);

    private static void addVertex(class_243 pos, class_287 bufferBuilder) {
        bufferBuilder.method_22912((float)pos.field_1352, (float)pos.field_1351, (float)pos.field_1350).method_1336(DebugUtils.rgba.x, DebugUtils.rgba.y, DebugUtils.rgba.z, DebugUtils.rgba.w);
    }

    private static void drawLines(class_243[] vertices, class_243 renderContext, boolean connect) {
        RenderSystem.enableDepthTest();
        RenderSystem.setShader(class_757::method_34540);
        class_289 tessellator = class_289.method_1348();
        class_287 bufferBuilder = tessellator.method_60827(class_293.class_5596.field_29345, class_290.field_1576);
        for (class_243 vertex : vertices) {
            class_243 v = vertex.method_1020(renderContext);
            DebugUtils.addVertex(v, bufferBuilder);
        }
        if (connect) {
            class_243 v = vertices[0].method_1020(renderContext);
            DebugUtils.addVertex(v, bufferBuilder);
        }
        class_286.method_43433((class_9801)bufferBuilder.method_60800());
    }

    public static void drawLine(class_243 from, class_243 to, class_243 renderContext) {
        class_243[] vertices = new class_243[]{from, to};
        DebugUtils.drawLines(vertices, renderContext);
    }

    public static void drawLines(class_243[] vertices, class_243 renderContext) {
        DebugUtils.drawLines(vertices, renderContext, false);
    }

    public static void drawPoly(class_243[] vertices, class_243 renderContext) {
        DebugUtils.drawLines(vertices, renderContext, true);
    }
}

