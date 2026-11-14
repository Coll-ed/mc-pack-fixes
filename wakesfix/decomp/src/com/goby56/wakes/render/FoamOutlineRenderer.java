/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.systems.RenderSystem
 *  net.minecraft.class_1297
 *  net.minecraft.class_286
 *  net.minecraft.class_287
 *  net.minecraft.class_289
 *  net.minecraft.class_290
 *  net.minecraft.class_293$class_5596
 *  net.minecraft.class_2960
 *  net.minecraft.class_310
 *  net.minecraft.class_4587
 *  net.minecraft.class_4597
 *  net.minecraft.class_4608
 *  net.minecraft.class_5599
 *  net.minecraft.class_5602
 *  net.minecraft.class_630
 *  net.minecraft.class_630$class_593
 *  net.minecraft.class_630$class_618
 *  net.minecraft.class_757
 *  net.minecraft.class_9801
 *  org.joml.Matrix4f
 *  org.joml.Vector3f
 *  org.joml.Vector4f
 */
package com.goby56.wakes.render;

import com.goby56.wakes.config.WakesConfig;
import com.goby56.wakes.duck.ProducesWake;
import com.goby56.wakes.utils.WakesUtils;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.stream.Stream;
import net.minecraft.class_1297;
import net.minecraft.class_286;
import net.minecraft.class_287;
import net.minecraft.class_289;
import net.minecraft.class_290;
import net.minecraft.class_293;
import net.minecraft.class_2960;
import net.minecraft.class_310;
import net.minecraft.class_4587;
import net.minecraft.class_4597;
import net.minecraft.class_4608;
import net.minecraft.class_5599;
import net.minecraft.class_5602;
import net.minecraft.class_630;
import net.minecraft.class_757;
import net.minecraft.class_9801;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

public class FoamOutlineRenderer {
    public static <T extends class_1297> void render(T entity, float yaw, float tickDelta, class_4587 matrices, class_4597 vertexConsumers, int light) {
        if (!WakesConfig.loadConfig().renderWakes || !((ProducesWake)entity).onWaterSurface()) {
            return;
        }
        float height = WakesUtils.getWaterLevel(entity.method_37908(), entity);
        String[] typeID = entity.method_5864().toString().split("\\.");
        class_5599 modelLoader = class_310.method_1551().method_31974();
        Stream<class_630> parts = class_5602.method_32076().filter(layer -> layer.method_35743().toString().contains(typeID[typeID.length - 1])).map(arg_0 -> ((class_5599)modelLoader).method_32072(arg_0));
        class_287 buffer = class_289.method_1348().method_60827(class_293.class_5596.field_27382, class_290.field_1580);
        RenderSystem.setShader(class_757::method_34502);
        RenderSystem.setShaderTexture((int)0, (class_2960)class_2960.method_60655((String)"wakes", (String)"icon.png"));
        Matrix4f matrix = matrices.method_23760().method_23761();
        Vector3f entityPos = entity.method_19538().method_46409();
        parts.forEach(modelPart -> modelPart.method_35745(matrices, (entry, path, index, cuboid) -> {
            Matrix4f cuboidMatrix = entry.method_23761();
            float minY = Float.POSITIVE_INFINITY;
            float maxY = Float.NEGATIVE_INFINITY;
            for (class_630.class_593 quad : cuboid.field_3649) {
                for (class_630.class_618 vertex : quad.field_3502) {
                    Vector3f pos = FoamOutlineRenderer.getVertexAbsolutePos(vertex, modelPart, cuboidMatrix, entityPos);
                    if (pos.y < minY) {
                        minY = pos.y;
                    }
                    if (!(pos.y > maxY)) continue;
                    maxY = pos.y;
                }
            }
            if (minY > height || maxY < height) {
                return;
            }
            for (class_630.class_593 quad : cuboid.field_3649) {
                if (quad.field_21618.y == 0.0f) continue;
                for (int i = 0; i < 4; ++i) {
                    Vector3f pos = FoamOutlineRenderer.getVertexAbsolutePos(quad.field_3502[i], modelPart, cuboidMatrix, entityPos);
                    System.out.printf("writing vertex %d at %s from %s\n", i, pos, path);
                    buffer.method_22918(matrix, pos.x + 0.1f * Math.signum(pos.x), height, pos.z + 0.1f * Math.signum(pos.z)).method_22915(1.0f, 1.0f, 1.0f, 1.0f).method_22913(i < 2 ? 0.0f : 1.0f, i == 0 || i == 3 ? 0.0f : 1.0f).method_22922(class_4608.field_21444).method_60803(light).method_22914(0.0f, 1.0f, 0.0f);
                }
                return;
            }
        }));
        class_286.method_43433((class_9801)buffer.method_60800());
    }

    private static Vector3f getVertexAbsolutePos(class_630.class_618 vertex, class_630 part, Matrix4f cuboidTransform, Vector3f origin) {
        Vector4f pos = cuboidTransform.transform(new Vector4f((part.field_3657 + vertex.field_3605.x) / 16.0f, (part.field_3656 + vertex.field_3605.y) / 16.0f, (part.field_3655 + vertex.field_3605.z) / 16.0f, 0.0f));
        return new Vector3f(pos.x + origin.x, pos.y + origin.y, pos.z + origin.z);
    }
}

