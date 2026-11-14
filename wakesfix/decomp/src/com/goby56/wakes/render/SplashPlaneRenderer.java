/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.systems.RenderSystem
 *  io.github.jdiemke.triangulation.DelaunayTriangulator
 *  io.github.jdiemke.triangulation.NotEnoughPointsException
 *  io.github.jdiemke.triangulation.Triangle2D
 *  io.github.jdiemke.triangulation.Vector2D
 *  net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents$ClientStarted
 *  net.minecraft.class_1163
 *  net.minecraft.class_1297
 *  net.minecraft.class_1920
 *  net.minecraft.class_2338
 *  net.minecraft.class_241
 *  net.minecraft.class_243
 *  net.minecraft.class_286
 *  net.minecraft.class_287
 *  net.minecraft.class_289
 *  net.minecraft.class_290
 *  net.minecraft.class_293$class_5596
 *  net.minecraft.class_2960
 *  net.minecraft.class_310
 *  net.minecraft.class_4587
 *  net.minecraft.class_4608
 *  net.minecraft.class_9801
 *  org.joml.Matrix4f
 *  org.joml.Vector3f
 */
package com.goby56.wakes.render;

import com.goby56.wakes.WakesClient;
import com.goby56.wakes.render.RenderType;
import com.mojang.blaze3d.systems.RenderSystem;
import io.github.jdiemke.triangulation.DelaunayTriangulator;
import io.github.jdiemke.triangulation.NotEnoughPointsException;
import io.github.jdiemke.triangulation.Triangle2D;
import io.github.jdiemke.triangulation.Vector2D;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.minecraft.class_1163;
import net.minecraft.class_1297;
import net.minecraft.class_1920;
import net.minecraft.class_2338;
import net.minecraft.class_241;
import net.minecraft.class_243;
import net.minecraft.class_286;
import net.minecraft.class_287;
import net.minecraft.class_289;
import net.minecraft.class_290;
import net.minecraft.class_293;
import net.minecraft.class_2960;
import net.minecraft.class_310;
import net.minecraft.class_4587;
import net.minecraft.class_4608;
import net.minecraft.class_9801;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public class SplashPlaneRenderer
implements ClientLifecycleEvents.ClientStarted {
    private static ArrayList<Vector2D> points;
    private static List<Triangle2D> triangles;
    private static ArrayList<class_243> vertices;
    private static ArrayList<class_243> normals;
    private static final Texture tex;
    private static int ticks;
    private static int animationFrame;
    private static final double SQRT_8;

    public static void tick() {
        if (++ticks > 10) {
            ++animationFrame;
            animationFrame %= 2;
            ticks = 0;
        }
    }

    public static <T extends class_1297> void render(T entity, float yaw, float tickDelta, class_4587 matrices, int light) {
        RenderSystem.setShader(RenderType.getProgram());
        RenderSystem.setShaderColor((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
        RenderSystem.enableBlend();
        matrices.method_22903();
        float velocity = (float)Math.floor(entity.method_18798().method_37267() * 20.0) / 20.0f;
        float progress = Math.min(1.0f, velocity / WakesClient.CONFIG_INSTANCE.maxSplashPlaneVelocity);
        float scalar = (float)((double)WakesClient.CONFIG_INSTANCE.splashPlaneScale * Math.sqrt(entity.method_17681() * Math.max(1.0f, progress) + 1.0f) / 3.0);
        matrices.method_22905(scalar, scalar, scalar);
        Matrix4f matrix = matrices.method_23760().method_23761();
        Vector3f color = new Vector3f();
        if (WakesClient.CONFIG_INSTANCE.useWaterBlending) {
            int waterCol = class_1163.method_4961((class_1920)entity.method_37908(), (class_2338)entity.method_24515());
            color.x = (float)(waterCol >> 16 & 0xFF) / 255.0f;
            color.y = (float)(waterCol >> 8 & 0xFF) / 255.0f;
            color.z = (float)(waterCol & 0xFF) / 255.0f;
        } else {
            color.set(1.0f, 1.0f, 1.0f);
        }
        RenderSystem.setShaderTexture((int)0, (class_2960)SplashPlaneRenderer.tex.id);
        tex.offsetPixels(animationFrame * SplashPlaneRenderer.tex.res, (int)(progress * (float)(SplashPlaneRenderer.tex.outlineOffset - 1)) * SplashPlaneRenderer.tex.res);
        SplashPlaneRenderer.renderSurface(matrix, color, light);
        color.set(1.0f, 1.0f, 1.0f);
        tex.offsetPixels(animationFrame * SplashPlaneRenderer.tex.res, ((int)(progress * (float)(SplashPlaneRenderer.tex.outlineOffset - 1)) + SplashPlaneRenderer.tex.outlineOffset) * SplashPlaneRenderer.tex.res);
        SplashPlaneRenderer.renderSurface(matrix, color, light);
        matrices.method_22909();
    }

    private static void renderSurface(Matrix4f matrix, Vector3f color, int light) {
        class_287 buffer = class_289.method_1348().method_60827(class_293.class_5596.field_27379, class_290.field_1580);
        for (int s = -1; s < 2; ++s) {
            if (s == 0) continue;
            for (int i = 0; i < vertices.size(); ++i) {
                class_243 vertex = vertices.get(i);
                class_243 normal = normals.get(i);
                buffer.method_22918(matrix, (float)((double)s * vertex.field_1352 * (double)WakesClient.CONFIG_INSTANCE.splashPlaneWidth), (float)(vertex.field_1350 * (double)WakesClient.CONFIG_INSTANCE.splashPlaneHeight), (float)(vertex.field_1351 * (double)WakesClient.CONFIG_INSTANCE.splashPlaneDepth)).method_22915(color.x, color.y, color.z, 1.0f).method_22913((float)(vertex.field_1352 / (double)SplashPlaneRenderer.tex.width + (double)SplashPlaneRenderer.tex.uvOffset.field_1343), (float)(vertex.field_1351 / (double)SplashPlaneRenderer.tex.height + (double)SplashPlaneRenderer.tex.uvOffset.field_1342)).method_22922(class_4608.field_21444).method_60803(light).method_22914((float)normal.field_1352, (float)normal.field_1351, (float)normal.field_1350);
            }
        }
        RenderSystem.disableCull();
        class_286.method_43433((class_9801)buffer.method_60800());
        RenderSystem.enableCull();
    }

    private static double upperBound(double x) {
        return -2.0 * x * x + SQRT_8 * x;
    }

    private static double lowerBound(double x) {
        return (SQRT_8 - 2.0) * x * x;
    }

    private static double height(double x, double y) {
        return 4.0 * (x * (SQRT_8 - x) - y - x * x) / SQRT_8;
    }

    private static class_243 normal(double x, double y) {
        double nx = SQRT_8 / (4.0 * (4.0 * x + y - SQRT_8));
        double ny = SQRT_8 / (4.0 * (2.0 * x * x - SQRT_8 + 1.0));
        return class_243.method_1030((float)((float)Math.tan(nx)), (float)((float)Math.tan(ny)));
    }

    private static void distributePoints() {
        int res = WakesClient.CONFIG_INSTANCE.splashPlaneResolution;
        points = new ArrayList();
        for (float i = 0.0f; i < (float)res; i += 1.0f) {
            double x = i / (float)(res - 1);
            double h = SplashPlaneRenderer.upperBound(x) - SplashPlaneRenderer.lowerBound(x);
            int n_points = (int)Math.max(1.0, Math.floor(h * (double)res));
            for (float j = 0.0f; j < (float)(n_points + 1); j += 1.0f) {
                float y = (float)((double)(j / (float)n_points) * h + SplashPlaneRenderer.lowerBound(x));
                points.add(new Vector2D(x, (double)y));
            }
        }
    }

    private static void generateMesh() {
        vertices = new ArrayList();
        normals = new ArrayList();
        try {
            DelaunayTriangulator delaunay = new DelaunayTriangulator(points);
            delaunay.triangulate();
            triangles = delaunay.getTriangles();
        }
        catch (NotEnoughPointsException e) {
            e.printStackTrace();
        }
        for (Triangle2D tri : triangles) {
            for (Vector2D vec : new Vector2D[]{tri.a, tri.b, tri.c}) {
                double x = vec.x;
                double y = vec.y;
                vertices.add(new class_243(x, y, SplashPlaneRenderer.height(x, y)));
                normals.add(SplashPlaneRenderer.normal(x, y));
            }
        }
    }

    public static void initSplashPlane() {
        SplashPlaneRenderer.distributePoints();
        SplashPlaneRenderer.generateMesh();
    }

    public void onClientStarted(class_310 client) {
        SplashPlaneRenderer.initSplashPlane();
    }

    static {
        tex = new Texture("textures/splash_plane_animation.png", 16, 2, 8);
        ticks = 0;
        animationFrame = 0;
        SQRT_8 = Math.sqrt(8.0);
    }

    private static class Texture {
        public final int res;
        public final int width;
        public final int height;
        public final int outlineOffset;
        public final class_2960 id;
        public class_241 uvOffset = new class_241(0.0f, 0.0f);

        public Texture(String path, int resolution, int frames, int stages) {
            this.id = class_2960.method_60655((String)"wakes", (String)path);
            this.res = resolution;
            this.width = frames;
            this.height = stages * 2;
            this.outlineOffset = stages;
        }

        public void offsetPixels(int x, int y) {
            this.uvOffset = new class_241((float)x / (float)(this.width * this.res), (float)y / (float)(this.height * this.res));
        }
    }
}

